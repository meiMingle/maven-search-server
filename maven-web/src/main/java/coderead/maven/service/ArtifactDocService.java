package coderead.maven.service;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/**
 * 文档管理
 *
 * @author 鲁班大叔
 * @date 2021
 */
@Service
public class ArtifactDocService {
  static  final   Logger logger = LoggerFactory.getLogger(ArtifactDocService.class);
    @Value("${doc.root}")
    private String docRoot;

    // 读取文档
    public String getIndexDoc(String groupId, String artifactId) {
        if (!existsInexDoc(groupId, artifactId)) {
            return null;
        }
        File file = new File(docRoot, String.format("%s/%s/index.md", groupId, artifactId));
        try {
            try (RandomAccessFile rf = new RandomAccessFile(file, "r")) {
                byte[] bytes = new byte[(int) rf.length()];
                rf.readFully(bytes);
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败:", e);
        }
    }

    public String getIndexDocToHtml(String groupId, String artifactId){
        if (!existsInexDoc(groupId, artifactId)) {
            return null;
        }
        Parser parser = Parser.builder().build();
        Node node = parser.parse(getIndexDoc(groupId, artifactId));
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(node);
    }

    public boolean existsInexDoc(String groupId, String artifactId) {
        File file = new File(docRoot, String.format("%s/%s/index.md", groupId, artifactId));
        return file.exists() && file.isFile();
    }
}
