package coderead.maven;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import coderead.maven.search.IndexShortSearch;
import coderead.maven.service.ArtifactInfoStore;
import coderead.maven.service.MavenIndexManager;
import coderead.maven.search.SearchResult;
import org.apache.lucene.index.IndexWriter;
import org.apache.maven.index.ArtifactInfo;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author 鲁班大叔
 * @date 2021
 */
public class SearchTest {
    @Test
    public void test() throws Exception {
        //
        long begin=System.currentTimeMillis();
        ArtifactInfoStore store=new ArtifactInfoStore();
        store.setIndexFile("/Users/tommy/data/central-index2/short.index");
        store.setVersionStoreFile("/Users/tommy/data/central-index2/version.txt");
        store.afterPropertiesSet();
        IndexShortSearch search=new IndexShortSearch( );
        search.setArtifactInfoStore(store);
        search.afterPropertiesSet();
        long end = System.currentTimeMillis();
        System.out.println(String.format("构建用时:%s 秒", (end-begin)/1000));
        begin=end;
        List<SearchResult> results = search.search("dubbo");
        end = System.currentTimeMillis();
        System.out.println(String.format("查询用时:%s 秒", (end-begin)/1000));

        results.stream().map(i->i.getItem().getGroupId()+":"+i.getItem().getArtifactId()).forEach(System.out::println);

        System.out.println(results.size());
    }

    // 排序
    @Test
    public void test2() throws IOException {
        String filePath = System.getProperty("user.dir") + "/src/test/resources/jar.text";
        BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(filePath+"2"));
        PrintWriter printWriter=new PrintWriter(bufferedWriter);
        Stream<String> lines = Files.lines(new File(filePath).toPath());
        lines.sorted((Comparator.comparingInt(String::length)))
                .forEach(s -> {
            printWriter.println(s);
        });
        printWriter.flush();
        printWriter.close();
    }

    @Test
    public void test3() throws Exception {
        MavenIndexManager example = new MavenIndexManager();
        List<ArtifactInfo> list = example.search("spring-boot", "org.springframework.boot");
        for (ArtifactInfo info : list) {
            System.out.println(info);
        }
    }

    // 更新索引测试
    @Test
    public void updateIndexTest() throws IOException {
        MavenIndexManager manager = new MavenIndexManager();
        IndexWriter indexWriter = manager.getCentralContext().getIndexWriter();
    }
}
