package coderead.maven.service;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import coderead.maven.bean.ArtifactIndexInfo;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Bits;
import org.apache.maven.index.*;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.updater.*;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author 鲁班大叔
 * @date 2021
 */
@Component
@Configuration
public class MavenIndexManager implements DisposableBean, InitializingBean {
    static final Logger logger = LoggerFactory.getLogger(MavenIndexManager.class);
    private DefaultPlexusContainer plexusContainer;
    Indexer indexer;
    private IndexingContext centralContext;
    @Value("${index.repository.path}")
    String indexRepository;
    @Value("${index.remote.url}")
    String indexRepositoryRemoteUrl;
    @Value("${index.remote.update.url}")
    String indexRepositoryRemoteUpdateUrl;


    private IndexUpdater indexUpdater; // 索引更新组件
    private Wagon httpWagon;// 往 络请求组件
    private int size;
    private Date lastTimestamp;
    @Value("${test:false}")
    private boolean testModel;
    @Autowired
    private ArtifactInfoStore artifactInfoStore;

    public MavenIndexManager() {

    }
    // 索引更新计划


    public List<ArtifactInfo> search(String groupId, String artifactId) throws IOException {
        List<ArtifactInfo> result = new ArrayList<>();
        final Query groupIdQ =
                indexer.constructQuery(MAVEN.GROUP_ID, new SourcedSearchExpression(groupId));
        final Query artifactIdQ =
                indexer.constructQuery(MAVEN.ARTIFACT_ID, new SourcedSearchExpression(artifactId));
        final BooleanQuery query = new BooleanQuery.Builder()
                .add(groupIdQ, BooleanClause.Occur.MUST)
                .add(artifactIdQ, BooleanClause.Occur.MUST)
/*
                .add(indexer.constructQuery(MAVEN.PACKAGING, new SourcedSearchExpression("jar")), BooleanClause.Occur.MUST)
*/
                .add(indexer.constructQuery(MAVEN.CLASSIFIER,
                        new SourcedSearchExpression(Field.NOT_PRESENT)), BooleanClause.Occur.MUST_NOT)
                .build();
        FlatSearchRequest flatRequest = new FlatSearchRequest(query, ArtifactInfo.VERSION_COMPARATOR, centralContext);
        FlatSearchResponse flatSearchResponse = indexer.searchFlat(flatRequest);
        for (ArtifactInfo ai : flatSearchResponse.getResults()) {
            result.add(ai);
        }
        return result;
    }


    @Override
    public void destroy() throws Exception {
        centralContext.close(false);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
        if (testModel) {
            return;
        }
        firstCheck(); //检测是否为初次更新
    }
    private void init() throws Exception {
        final DefaultContainerConfiguration config = new DefaultContainerConfiguration();
        config.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
        this.plexusContainer = new DefaultPlexusContainer(config);
        this.indexer = plexusContainer.lookup(Indexer.class);
        File centralIndexDir = new File(indexRepository);
        List<IndexCreator> indexers = new ArrayList<>();
        indexers.add(plexusContainer.lookup(IndexCreator.class, "min"));
        indexers.add(plexusContainer.lookup(IndexCreator.class, "jarContent"));
        indexers.add(plexusContainer.lookup(IndexCreator.class, "maven-plugin"));
        centralContext =
                indexer.createIndexingContext("central-context", "central",
                        null,
                        centralIndexDir,
                        indexRepositoryRemoteUrl,
                        indexRepositoryRemoteUpdateUrl, true, true, indexers);
        this.indexUpdater = plexusContainer.lookup(IndexUpdater.class);
        // lookup wagon used to remotely fetch index
        this.httpWagon = plexusContainer.lookup(Wagon.class, "http");
        size = centralContext.getSize();
        lastTimestamp = centralContext.getTimestamp();
    }
    private void firstCheck() throws IOException {
        // 启动时检查
        if (centralContext.getTimestamp() == null || centralContext.getSize() < 100) {
            update();//执行更新
        } else if ( artifactInfoStore.getAllArtifact(false).isEmpty()) {
            logger.info("索引文件开始加载.."+artifactInfoStore.getIndexFile());
            createShortIndexFile("初次构建索引文件");
        }

    }

    // 每周三凌晨5点开始更新
    public void update() throws IOException {
        String planId = "UP" + new SimpleDateFormat("yyyyMMddhhmm").format(new Date());
        logger.info("{}-执行索引更新计划开始------", planId);
        long begin = System.currentTimeMillis();
        TransferListener listener = new AbstractTransferListener() {
            public void transferStarted(TransferEvent transferEvent) {
                logger.info("正在下载 " + transferEvent.getResource().getName());
            }

            @Override
            public void debug(String message) {
                logger.debug(message);
            }

            @Override
            public void transferError(TransferEvent transferEvent) {
                logger.error("{}-索引下载失败：{}", planId, transferEvent);
            }

            public void transferCompleted(TransferEvent transferEvent) {
                logger.info("{}-索引下载成功：{}", planId, transferEvent);
            }
        };
        ResourceFetcher resourceFetcher = new WagonHelper.WagonFetcher(httpWagon, listener, null, null);
        Date centralContextCurrentTimestamp = centralContext.getTimestamp();
        IndexUpdateRequest updateRequest = new IndexUpdateRequest(centralContext, resourceFetcher);
        IndexUpdateResult updateResult = indexUpdater.fetchAndUpdateIndex(updateRequest);
        if (updateResult.isFullUpdate()) {
            logger.info("{}-完成了全量更新", planId);
            createShortIndexFile(planId);
        } else if (updateResult.getTimestamp().equals(centralContextCurrentTimestamp)) {
            logger.info("{}-无需更新，索引当前是最新的", planId);
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            logger.info("{}-实现了增量更新，从{}至{}",
                    planId,
                    dateFormat.format(centralContextCurrentTimestamp),
                    dateFormat.format(updateResult.getTimestamp()));
            createShortIndexFile(planId);
        }


        size = centralContext.getSize();
        lastTimestamp = centralContext.getTimestamp();

        long end = System.currentTimeMillis();
        logger.info("{}-索引更新计划结束,用时{}秒", planId, (end - begin) / 1000);
    }

    public void createShortIndexFile(String planId) throws IOException {
        logger.info("{}加载快捷索引文件..", planId);

        final IndexSearcher searcher = centralContext.acquireIndexSearcher();
        Map<String, ArtifactIndexInfo> infos = new HashMap<>();

        try {
            final IndexReader ir = searcher.getIndexReader();
            Bits liveDocs = MultiFields.getLiveDocs(ir);// 获取 所有文档
            String u, key;
            ArtifactIndexInfo artifact;
            String[] split;
            Set<String> files = new HashSet<>();
            files.add("u");
            files.add("m");
            final int[] progress = {0, 0, ir.maxDoc()};
            new Thread(() -> {
                while (progress[0] < progress[2]) {
                    logger.info("{}快捷索加载进度:{}  完成数:{}", planId, progress[0] * 100 / progress[2], progress[1]);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        logger.error("", e);
                    }
                }
            }).start();

            for (int i = 0; i < ir.maxDoc(); i++) {
                progress[0]++;
                if (liveDocs == null || liveDocs.get(i)) {
                    final Document doc = ir.document(i, files);
                    //示例值：ogr.grails|grails-web|2.5.2|NA|jar
                    u = doc.get("u");
                    if (u == null || !u.endsWith("NA|jar")) {
                        continue;
                    }
                    split = u.split("\\|");
                    if (split.length < 5) {
                        logger.warn("{}快捷索引加载失败，错误的数据格式{}", planId, u);
                        continue;
                    }
                    key = split[0].trim() + " " + split[1].trim();
                    try {
                        artifact = ArtifactIndexInfo.parse(key + " " + doc.get("m") + " " + split[2].trim() + " false");
                    } catch (IllegalArgumentException e) {
                        logger.error("数据格式错误："+u,e);
                        continue;
                    }
                    if (!infos.containsKey(key) ||
                            artifact.lastModified > infos.get(key).lastModified) {
                        infos.put(key, artifact);
                    }
                    progress[1]++;
                }

            }
            // 保存
            artifactInfoStore.margeAndCoverArtifact(new ArrayList<>(infos.values()));
        } finally {
            centralContext.releaseIndexSearcher(searcher);
            logger.info("{}快捷索引加载完成", planId);
        }
    }

    public IndexingContext getCentralContext() {
        return centralContext;
    }

    public Date getLastTimestamp() {
        return this.lastTimestamp;
    }

    public int getSize() {
        return this.size;
    }

    public String getRespositoryUrl() {
        return indexRepositoryRemoteUrl;
    }


}
