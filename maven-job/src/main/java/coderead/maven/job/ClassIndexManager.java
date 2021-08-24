package coderead.maven.job;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */


import coderead.maven.job.dao.Artifact;
import coderead.maven.job.dao.ArtifactClass;
import coderead.maven.job.dao.ArtifactMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author 鲁班大叔
 * @date 2021
 */
@Component
public class ClassIndexManager implements InitializingBean {
    static final Logger logger = LoggerFactory.getLogger(ClassIndexManager.class);


    @Value("${jar.download.urls}")
    String[] downloadUrls;
    @Value("${job.active}")
    private boolean jobActive;
    @Value("${job.task.size:100}")
    private int taskSize;
    @Value("${job.thread.size:10}")
    private int jobThreadSize;
    @Autowired
    ArtifactMapper artifactMapper;

    private ThreadPoolExecutor downloadExecutor;

    private OkHttpClient okHttpClient;
    private Thread job;
    private Map<Artifact, Integer> failTasks = new ConcurrentHashMap<>();
    int lastId = 0;
    long begin = System.currentTimeMillis();
    @Override
    public void afterPropertiesSet() throws Exception {
        okHttpClient = new OkHttpClient()
                .newBuilder()
                .callTimeout(30, TimeUnit.SECONDS)
                .build();
        downloadExecutor = new ThreadPoolExecutor(jobThreadSize, jobThreadSize,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        if (jobActive) {
            runJob();
        }
    }

    private void runJob() {

        job = new Thread(() -> {

            while (true) {
                try {
                    logger.info(String.format("执行进度:%s 用时%s",
                            downloadExecutor,
                            NumberUtil.distanceCurrentTime(begin)));
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    logger.error("", e);
                }
                if (downloadExecutor.getQueue().size() < taskSize * 2) {
                    int size = startUpdateTask(lastId);
                    if (size == 0) {
                        logger.info(String.format("当前不存在待更新任务，休眠%s分钟", 60));
                        lastId = 0;     //还原ID，执行下一次更新周期
                        try {
                            Thread.sleep(60 * 60 * 1000);
                        } catch (InterruptedException e) {
                            logger.error("", e);
                        }
                        begin = System.currentTimeMillis();
                    }
                }
            }
        });
        job.setDaemon(true);
        job.start();
    }

    // 执行索引更新任务
    public int startUpdateTask(long beginId) {

        //1. 获取待更新任务
        List<Artifact> artifacts = artifactMapper.findArtifactByState(beginId, ArtifactMapper.unIndex, taskSize);
        // where
        if (artifacts.isEmpty()) {
            return 0;
        }


        //2.修改任务状态为进行中...
        Integer[] ids = new Integer[artifacts.size()];
        for (int i = 0; i < artifacts.size(); i++) {
            ids[i] = artifacts.get(i).getId();
        }
        artifactMapper.setArtifactSate(ids, ArtifactMapper.indexing);
        lastId = ids[ids.length - 1];
        //3.添加至下载更新队列
        for (Artifact fact : artifacts) {
            downloadExecutor.submit(() -> {
                try {
                    tryDownloadArt(fact);
                } catch (Throwable e) {
                    logger.error("", e);
                }
            });
        }
        return artifacts.size();
    }


    private boolean tryDownloadArt(Artifact artifact) {
        for (String repositoryUrl : downloadUrls) {
            try {
                List<ArtifactClass> list = downloadJar(repositoryUrl, artifact);
                failTasks.remove(artifact);
                saveClassIndex(artifact,list);
                return true;
            } catch (IOException | IllegalStateException e) {
                logger.error(String.format("下载或解析失败:%s:%s:%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getLastVersion())
                        , e);
            } catch (Throwable ex) {
                logger.error(String.format("更新失败，未知异常:%s:%s:%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getLastVersion()), ex);
                failTasks.remove(artifact);
                saveFailed(artifact);
                return false;
            }
        }
        int failCount = failTasks.getOrDefault(artifact, 0) + 1;
        failTasks.put(artifact, failCount);
        if (failCount < 2) { // 重试
            downloadExecutor.submit(() -> tryDownloadArt(artifact));// 放至队列未尾
        } else {
            failTasks.remove(artifact);
            logger.error(String.format("下载或解析失败:%s:%s:%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getLastVersion()));
            saveFailed(artifact);
        }
        return false;
    }

    private void saveFailed(Artifact artifact) {
        artifactMapper.setArtifactSate(new Integer[]{artifact.getId()}, ArtifactMapper.fail);
    }


    public List<ArtifactClass> downloadJar(String repositoryUrl, Artifact info) throws IOException {
//      示例  https://repo1.maven.org/maven2/com/alibaba/dubbo/2.6.10/dubbo-2.6.10.jar
        String urlText = String.format("%s/%s/%s/%s/%s-%s.jar",
                repositoryUrl,
                info.getGroupId().replaceAll("\\.", "/"),
                info.getArtifactId(),
                info.getLastVersion(),
                info.getArtifactId(),
                info.getLastVersion());
        logger.debug("下载：" + urlText);
        long begin = System.currentTimeMillis();
        InputStream inputStream;
        final Request request = new Request.Builder()
                .cacheControl(new CacheControl.Builder()
                        .noStore()
                        .build())
                .addHeader("sec-ch-ua", "\" Not;A Brand\";v=\"99\", \"Google Chrome\";v=\"91\", \"Chromium\";v=\"91\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("Upgrade-Insecure-Requests", "1")
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.164 Safari/537.36")
                .url(urlText)
                .build();
        final Call call = okHttpClient.newCall(request);
        Response response = call.execute();
        if (!response.isSuccessful()) {
            response.close();
            throw new IllegalStateException(String.format("下载失败：状码码:%s  %s 链接：%s", response.code(), response.message(), urlText));
        }
        inputStream = response.body().byteStream();
        List<ArtifactClass> result = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String classFileName = zipEntry.getName();
                if (!classFileName.endsWith(".class")) {
                    continue;
                }
                if (!classFileName.contains("/")) {// 必须要包含一个包名
                    continue;
                }
                classFileName = classFileName.substring(0, classFileName.lastIndexOf("."));
                if (classFileName.endsWith("$")) {
                    continue;
                }
                String className = classFileName
                        .replaceAll("/", ".")
                        .replaceAll("\\$\\$", ".")
                        .replaceAll("\\$", ".");

                if (NumberUtil.isInteger(className.substring(className.lastIndexOf(".") + 1))) {
                    continue; //过滤无名类部类
                }
                ArtifactClass artifactClass = new ArtifactClass();
                artifactClass.setArtifact(info.getArtifact());
                artifactClass.setVersion(info.getLastVersion());
                artifactClass.setClassname(className);
                result.add(artifactClass);
            }
        }
        response.close();
        if (result.isEmpty()) {
            logger.warn("jar 包 class 数量为空：{}", urlText);
        }
        long end = System.currentTimeMillis();
        logger.debug(String.format("下载用时：%s 秒,链接：%s", (end - begin) / 1000f, urlText));
        return result;
    }

    private void saveClassIndex(Artifact artifact, List<ArtifactClass> infos) throws IOException {
        if (infos.isEmpty()) {
            artifactMapper.setArtifactSate(new Integer[]{artifact.getId()},ArtifactMapper.succeed);
            return;
        }
        artifactMapper.updateArtifactClass(infos);
    }


    public String getTaskInfo() {
        return String.format("执行进度:%s 用时%s",
                downloadExecutor,
                NumberUtil.distanceCurrentTime(begin));
    }
}
