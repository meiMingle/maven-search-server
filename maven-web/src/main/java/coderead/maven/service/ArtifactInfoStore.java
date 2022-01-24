package coderead.maven.service;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

/**
 * @author 鲁班大叔
 * @date 2021
 */

import coderead.maven.bean.ArtifactIndexInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 用于记录版本使用数据
 */
@Component
@DependsOn("indexShortSearch")
public class ArtifactInfoStore implements InitializingBean {
    static final Logger logger = LoggerFactory.getLogger(ArtifactInfoStore.class);

    @Value("${versionStoreFile}")
    String versionStoreFile;
    // 版本下载统计
    private Map<String, AtomicInteger> versionCounts = new ConcurrentHashMap<>();
    @Value("${index.repository.shortIndexFile}")
    String indexFile;
    private Map<String, ArtifactIndexInfo> infos;



    public void versionCount(String groupId, String artifactId, String versionId) {
        String key = groupId + ":" + artifactId + ":" + versionId;
        if (versionCounts.containsKey(key)) {
            AtomicInteger integer = versionCounts.get(key);
            if (integer != null) {
                integer.incrementAndGet();
            }
        } else {
            versionCounts.computeIfAbsent(key, k -> new AtomicInteger(1));
        }
    }

    public void storeVersionCount() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(versionStoreFile));
        PrintWriter printWriter = new PrintWriter(writer);
        versionCounts.entrySet().stream().map((e) -> e.getKey() + " " + e.getValue()).forEach(printWriter::println);
        printWriter.flush();
        printWriter.close();
        putToArtifactIndexInfo();// 更新到索引中
    }

    //  保存Artifact信息到索引文件中
    public void storeArtifact() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile));
        PrintWriter printWriter = new PrintWriter(writer);
        getAllArtifact(false).stream()
                .map(ArtifactIndexInfo::toLine)
                .forEach(printWriter::println);
        printWriter.flush();
        printWriter.close();
    }


    public ArtifactIndexInfo findArtifact(String groupId, String artifactId){
        return infos.get(groupId + " " + artifactId);
    }


    // 合并之后覆盖原来的 Artifact 数据
    public void margeAndCoverArtifact(List<ArtifactIndexInfo> newArtifacts){
        // 版本更新
        Map<String, ArtifactIndexInfo> newInfos = new HashMap<>();
        String key;
        ArtifactIndexInfo oldArtifact;
        for (ArtifactIndexInfo artifact : newArtifacts) {
            key = artifact.groupId + " " + artifact.artifactId;
            newInfos.put(key,artifact);
            if (infos.containsKey(key)) {
                oldArtifact=infos.get(key);
                artifact.indexClass =oldArtifact.indexClass;
                if (artifact.lastModified>oldArtifact.lastModified) {
                    artifact.indexClass =false;// 新版本 重新索引类
                }
            }
        }
        this.infos=newInfos;
        putToArtifactIndexInfo();// 更新使用次数
        try {
            storeArtifact();
        } catch (IOException e) {
            logger.error("索引保存失败：",e);
        }
    }



    private void putToArtifactIndexInfo() {
        Map<String, Integer> collect = versionCounts.entrySet().stream()
                .collect(Collectors.
                        toMap(e -> e.getKey().substring(0, e.getKey().lastIndexOf(":")),
                                e -> e.getValue().intValue(),
                                (v1, v2) -> v1 + v2));
        collect.forEach((k, v) -> {
            String[] split = k.split(":");
            ArtifactIndexInfo indexInfo = infos.get(split[0]+" "+ split[1]);
            if (indexInfo != null) {
                indexInfo.setHot(v);
            }
        });
    }


    public void loadVersionCount() throws IOException {
        versionCounts.clear();
        Files.lines(new File(versionStoreFile).toPath())
                .map(line -> {
                    String[] s = line.split(" ");
                    if (s.length!=2) {
                        logger.error("版本引用次数格式错误: {}",line);
                        return null;
                    }
                    return s;
                }).filter(Objects::nonNull)
                .forEach(a -> {
             versionCounts.put(a[0], new AtomicInteger(Integer.parseInt(a[1])));
        });

        putToArtifactIndexInfo();
    }

    public void loadArtifacts() throws IOException {
        if (infos != null) {
            infos.clear();
            infos = null;
        }
        logger.info("开始索引解析加载:"+indexFile);
        Assert.isTrue(new File(indexFile).exists(),"找不到索引文件："+indexFile);
        Map<String, ArtifactIndexInfo> collect = Files.lines(new File(indexFile).toPath())
                .filter(StringUtils::hasText)
                .map(line -> {
                    ArtifactIndexInfo parse = null;
                    try {
                        parse = ArtifactIndexInfo.parse(line);
                        return parse;
                    } catch (IllegalArgumentException e) {
                        logger.error(e.getMessage());
                    }
                    return null;
                }).filter(Objects::nonNull)
                .collect(Collectors.toMap(a -> a.groupId + " " + a.artifactId, a -> a));
        this.infos = collect;
        logger.info("完成索引加载:" + indexFile);
    }


    public int getVersionCount(String groupId, String artifactId, String versionId) {
        String key = groupId + ":" + artifactId + ":" + versionId;
        return versionCounts.getOrDefault(key, new AtomicInteger(0)).intValue();
    }



    public List<ArtifactIndexInfo> getAllArtifact(boolean sort){
        Collection<ArtifactIndexInfo> values = infos.values();
        if (sort) {
            return values.stream().sorted((Comparator.comparingInt(o -> o.getArtifactId().length()))).collect(Collectors.toList());
        }
        return new ArrayList<>(values);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (String s : Arrays.asList(versionStoreFile, indexFile)) {
            File file = new File(s);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        }

        loadArtifacts();  // 加载 Artifact 数据
        loadVersionCount();// 加载版本数据
    }


    public String getVersionStoreFile() {
        return versionStoreFile;
    }

    public void setVersionStoreFile(String versionStoreFile) {
        this.versionStoreFile = versionStoreFile;
    }

    public String getIndexFile() {
        return indexFile;
    }

    public void setIndexFile(String indexFile) {
        this.indexFile = indexFile;
    }

}
