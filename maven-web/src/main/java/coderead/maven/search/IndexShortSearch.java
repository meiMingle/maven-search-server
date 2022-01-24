package coderead.maven.search;

import coderead.maven.bean.Artifact;
import coderead.maven.bean.ArtifactIndexInfo;
import coderead.maven.dao.ArtifactMapper;
import coderead.maven.service.ArtifactInfoStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.*;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Character.isLowerCase;

/**
 * @author Tommy
 * Created by Tommy on 2021/6/5
 **/
@Component
@DependsOn("mavenIndexManager")
public class IndexShortSearch implements InitializingBean {
    static final Logger logger = LoggerFactory.getLogger(IndexShortSearch.class);
    WordIndex[][] indexes;
    @Autowired
    ArtifactInfoStore artifactInfoStore;

    @Autowired
    ArtifactMapper artifactMapper;

    @Value("${test:false}")
    private boolean testModel;
    @Value("${search.max.KeyWorld:50}")
    private int maxKeyWorld=50;
    public IndexShortSearch() {
    }

    public List<SearchResult> search(String keyWorld) {
        Assert.hasText(keyWorld, "keyWorld 不能为空");
        if (keyWorld.length() > maxKeyWorld) {
            logger.warn("搜索字符过长{},已截断至前{}个字符", keyWorld, maxKeyWorld);
            keyWorld = keyWorld.substring(0, maxKeyWorld);
        }
        keyWorld = keyWorld.toLowerCase();
        Stream<WordIndex> stream = getIndexByFirst(keyWorld);
        final int[] currentWordIndex = {0}; // 当前匹配的词项索引
        final WordIndex[] currentWord = new WordIndex[1];// 当前匹配词项
        final int[] highlight = new int[keyWorld.length()];
        int keyIndex = 0;
        for (final char c : keyWorld.toCharArray()) {
            final int finalKeyIndex = keyIndex;
            stream = stream.filter(r -> {
                boolean result;
                if (r.match(currentWordIndex[0], c)) {
                    highlight[finalKeyIndex] = r.getIndex() + currentWordIndex[0];
                    currentWordIndex[0]++;
                    currentWord[0] = r;
                    result = true;
                } else if ((currentWord[0] = r.findNext(c)) != null) {
                    highlight[finalKeyIndex] = currentWord[0].getIndex();
                    currentWordIndex[0] = 1;
                    result = true;
                } else {
                    currentWordIndex[0] = 0;
                    currentWord[0] = null;
                    result = false;
                }
                return result;
            }).map(r -> currentWord[0]);
            keyIndex++;
        }

        // 索引复原
        stream = stream.peek(r -> {
            currentWordIndex[0] = 0;
            currentWord[0] = null;
        });

        // 返回最前面的一千个进行转换排序


        Stream<SearchResult> searchResultStream = stream.limit(1000)
                .distinct()// 去重
                .map(r -> { //转换
                    SearchResult result = new SearchResult((ArtifactIndexInfo) r.getResource());
                    result.setHighlight(Arrays.copyOf(highlight, highlight.length));
                    result.setMathIndex(r);
                    return result;
                }).sorted(SearchResult::compareTo)
                .limit(50);// 限制返回数量并去重
        return searchResultStream.collect(Collectors.toList());
    }

    // 基于第一个字母 获取索引并将其合并
    private Stream<WordIndex> getIndexByFirst(String keys) {
        keys = keys.toLowerCase();
        char firstChar = keys.charAt(0);
        int i = isLowerCase(firstChar) ? firstChar - 97 : 26;
        i=Math.min(i,26);
        return Arrays.stream(indexes[i]);
    }

    // 装载索引文件
    private WordIndex[][] loadIndex() throws IOException {
        logger.info("开始解析快捷索引");
        // 解析索引文件内容 ，生成分词
        IndexAnalysis indexAnalysis = new IndexAnalysis();
        List<WordIndex> parse = indexAnalysis.parse(artifactInfoStore.getAllArtifact(true));
        // 按首字母分组保存，其它字符保存在最后一个
        final WordIndex[][] stores = new WordIndex[27][0];
        parse.stream()
                .collect(Collectors.groupingBy(a -> a.getWord().toLowerCase().charAt(0)))
                .forEach((key, value) -> {
                    int index = isLowerCase(key) ? key - 97 : 26;
                    stores[index] = concat(stores[index],
                            value.toArray(new WordIndex[0]));
                });
        logger.info("完成解析快捷索引");
        return stores;
    }

    @Override
    public void afterPropertiesSet() throws IOException {
        if (testModel) {
            return;
        }
        this.indexes = loadIndex(); //加载索引
    }


    //重载索引
    public void reload() throws IOException {
        WordIndex[][] newIndexs = loadIndex();
        this.indexes = newIndexs;
    }

    static <T> T[] concat(T[] a, T[] b) {
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length + b.length);
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;

    }

    public ArtifactInfoStore getArtifactInfoStore() {
        return artifactInfoStore;
    }

    public void setArtifactInfoStore(ArtifactInfoStore artifactInfoStore) {
        this.artifactInfoStore = artifactInfoStore;
    }
}
