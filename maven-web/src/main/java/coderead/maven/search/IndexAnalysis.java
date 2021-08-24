package coderead.maven.search;

import coderead.maven.bean.ArtifactIndexInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 索引分词器
 *
 * @author Tommy
 **/
public class IndexAnalysis {
    private static Map<String, String> KEYWORD = new HashMap<>();
    static final Logger logger = LoggerFactory.getLogger(IndexAnalysis.class);

    // 解析器 index file
    public List<WordIndex> parse( List<ArtifactIndexInfo> infos) throws IOException {
        return infos.stream()
                .flatMap(info->parseIndex(info).stream())
                .collect(Collectors.toList());
    }

    private List<WordIndex> parseIndex(ArtifactIndexInfo info) {
        List<WordIndex> wordIndices = splitFileName(info.artifactId + ":" + info.groupId);
        linkResource(wordIndices, info);
        return wordIndices;
    }

    private void linkResource(List<WordIndex> wordIndices, Object resource) {
        wordIndices.stream().filter(a -> a.getNext() == null).findFirst()
                .ifPresent(a -> a.setNext(resource));
    }

    private List<WordIndex> splitFileName(String name) {
        WordIndex parent = null;
        StringBuilder builder = new StringBuilder();
        List<WordIndex> result = new ArrayList();
        Character[] chars = new Character[]{'_', '-', '.', ':'};

        for (int i = 0; i < name.toCharArray().length; i++) {
            // 拼装词
            char c = name.charAt(i);
            builder.append(c);

            // 检查分词条件
            boolean split;
            if (i == name.length() - 1) {
                split = true;
            } else if (builder.length() < 2) {
                split = false;
            } else if (Arrays.stream(chars).anyMatch(a -> a == c)) {
                split = true;
            } else if (builder.chars().allMatch(Character::isUpperCase)) {
                split = false;
            } else if (Character.isUpperCase(c)) {
                split = true;
            } else {
                split = false;
            }

            // 构建分词
            if (split) {
                //_aaaa
                WordIndex word = new WordIndex();
                String wordName = i == name.length() - 1 ? builder.toString() : builder.substring(0, builder.length() - 1);
                try {
                    word.setWord(getKeyword(wordName).toLowerCase());
                } catch (Exception e) {
                    System.out.println(name + "----" + i);
                    e.printStackTrace();
                }
                word.setIndex(i - builder.length() + 1);
                Optional.ofNullable(parent).ifPresent(p -> p.setNext(word));
                parent = word;
                result.add(word);
                //d_
                builder.delete(0, builder.length() - 1);// 保留当前字符
            }
            // 连续分词
            if (Arrays.stream(chars).anyMatch(a -> a == c)) {
                WordIndex word = new WordIndex();
                word.setWord(String.valueOf(c));
                word.setIndex(i);
                Optional.ofNullable(parent).ifPresent(p -> p.setNext(word));
                parent = word;
                builder.delete(0, 1);
                result.add(word);
            }
        }
        // 结束
        return result;
    }

    public static String getKeyword(String word) {
        if (!KEYWORD.containsKey(word)) {
            synchronized (KEYWORD) {
                if (!KEYWORD.containsKey(word)) {
                    KEYWORD.put(word, word);
                }
            }
        }
        return KEYWORD.get(word);
    }
}
