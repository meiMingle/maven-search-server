package coderead.maven;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import org.apache.lucene.document.*;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
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
import java.util.ArrayList;
import java.util.List;

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

}
