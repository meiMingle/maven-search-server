package coderead.maven.service;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

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
        File file = new File(docRoot, String.format("%s%s%s%sindex.md", groupId,File.separator ,artifactId,File.separator));
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
        //Parser parser = Parser.builder().build();

        List<Extension> extensions = Arrays.asList( TablesExtension.create());
        Parser parser = Parser.builder()
                .extensions(extensions)
                .build();

        Node node = parser.parse(getIndexDoc(groupId, artifactId));


        HtmlRenderer renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .build();

        //HtmlRenderer renderer = HtmlRenderer.builder().build();

        String render=renderer.render( node );
        if (render.contains( "<table>" )){
            render=render.replaceAll( "<table>","<table class='gridtable'>" );
        }

        // windows 系统
        if (render.contains( ".\\images\\" )){
            String replaceAll=render.replaceAll( ".\\\\images\\\\", "/"+groupId+"/"+artifactId+"/images/" );
            return replaceAll;
        // mac 系统
        }else if(render.contains( "./images/" )){
            String replaceAll=render.replaceAll( "./images/", "/"+groupId+"/"+artifactId+"/images/" );
            return replaceAll;
        }
        return render;
    }

    public boolean existsInexDoc(String groupId, String artifactId) {
        File file = new File(docRoot, String.format("%s%s%s%sindex.md", groupId, File.separator,artifactId,File.separator));
        return file.exists() && file.isFile();
    }
}
