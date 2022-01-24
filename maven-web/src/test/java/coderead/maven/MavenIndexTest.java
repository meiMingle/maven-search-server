package coderead.maven;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import coderead.maven.bean.ArtifactIndexInfo;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Bits;
import org.apache.maven.index.*;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author 鲁班大叔
 * @date 2021
 */
public class MavenIndexTest {
    Indexer indexer;
    private IndexingContext centralContext;
    private DefaultPlexusContainer plexusContainer;

    @BeforeEach
    public void init() throws PlexusContainerException, ComponentLookupException, IOException {
        final DefaultContainerConfiguration config = new DefaultContainerConfiguration();
        config.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
        this.plexusContainer = new DefaultPlexusContainer(config);
        this.indexer = plexusContainer.lookup(Indexer.class);
        File centralIndexDir = new File("/Users/tommy/data/central-index");
        List<IndexCreator> indexers = new ArrayList<>();
        indexers.add(plexusContainer.lookup(IndexCreator.class, "min"));
        indexers.add(plexusContainer.lookup(IndexCreator.class, "jarContent"));
        indexers.add(plexusContainer.lookup(IndexCreator.class, "maven-plugin"));
        centralContext =
                indexer.createIndexingContext("central-context", "central",
                        null,
                        centralIndexDir,
                        null,
                        null, true, true, indexers);

    }

    @Test
    public void search() throws IOException {

        final Query groupIdQ =
                indexer.constructQuery(MAVEN.GROUP_ID, new SourcedSearchExpression( "org.springframework.boot"));
        final Query artifactIdQ =
                indexer.constructQuery(MAVEN.ARTIFACT_ID, new SourcedSearchExpression("spring-boot"));
        final BooleanQuery query = new BooleanQuery.Builder()
                .add(groupIdQ, BooleanClause.Occur.MUST)
                .add(artifactIdQ, BooleanClause.Occur.MUST)
              /*  .add(indexer.constructQuery(MAVEN.PACKAGING, new SourcedSearchExpression("jar")), BooleanClause.Occur.MUST)*/
                .add(indexer.constructQuery(MAVEN.CLASSIFIER,
                        new SourcedSearchExpression(org.apache.maven.index.Field.NOT_PRESENT)), BooleanClause.Occur.MUST_NOT)
                .build();
        FlatSearchResponse response = indexer.searchFlat(new FlatSearchRequest(query, centralContext));
        for (ArtifactInfo info : response.getResults()) {
            System.out.println(info);
        }
    }
   @Test
   public void contextLoads() throws IOException {
        IndexSearcher indexSearcher = centralContext.acquireIndexSearcher();
        Document doc = indexSearcher.doc(1);
        String i = doc.get("i") + "|18";
        doc.removeFields("i");
        doc.add(new StringField("i",i, Field.Store.YES));
        IndexUtils.constructArtifactInfo(doc, centralContext);
        IndexWriter indexWriter = centralContext.getIndexWriter();
        indexWriter.deleteDocuments(new TermQuery(new Term("1", doc.get("1"))));
        indexWriter.commit();
        doc.add(new LongField("hot", 100l, Field.Store.YES));
        indexWriter.addDocument(doc);
        indexWriter.commit();

        TopDocs search = indexSearcher.search(new TermQuery(new Term("1", doc.get("1"))), 100);
        Document doc1 = indexSearcher.doc(search.scoreDocs[0].doc);
        System.out.println(doc1);
        centralContext.close(false);
    }

    @Test
    public void getAllTest() throws IOException {
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
            for (int i = 0; i < ir.maxDoc(); i++) {
                if (liveDocs == null || liveDocs.get(i)) {
                    final Document doc = ir.document(i, files);
                    //示例值：ogr.grails|grails-web|2.5.2|NA|jar
                    u = doc.get("u");
                    String su=u;
                    if (u == null || Stream.of("NA|jar","NA|pom").noneMatch(su::endsWith)) {
                        continue;
                    }
                    split = u.split("\\|");
                    key = split[0].trim() + " " + split[1].trim();
                    try {
                        artifact = ArtifactIndexInfo.parse(key + " " + doc.get("m") + " " + split[2].trim() + " false");
                    } catch (IllegalArgumentException e) {
                        System.err.println("数据格式错误："+u);
                        e.printStackTrace();
                        continue;
                    }
                    if (!infos.containsKey(key) ||
                            artifact.lastModified > infos.get(key).lastModified) {
                        infos.put(key, artifact);
                    }
                }

            }
            // 保存
        } finally {
            centralContext.releaseIndexSearcher(searcher);
            System.out.println("遍历完成:总数"+infos.size());
        }
    }

}
