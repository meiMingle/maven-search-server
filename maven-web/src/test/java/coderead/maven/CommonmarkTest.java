package coderead.maven;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * markdown 转 html
 * @author 鲁班大叔
 * @date 2021
 */
public class CommonmarkTest {
    public static void main(String[] args) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse("This is *Sparta*");
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        System.out.println(renderer.render(document));
    }

    @Test
    public void test() throws IOException {
        Parser parser = Parser.builder().build();
        Node node = parser.parseReader(new FileReader(System.getProperty("user.dir") + "/src/test/resources/SpringBean的生命周期.md"));
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        System.out.println(renderer.render(node));
    }

    @Test
    public void test2() throws IOException {
      String f=  System.getProperty("user.dir") + "/src/test/resources/SpringBean的生命周期.md";

        RandomAccessFile rf = new RandomAccessFile(f,"r");
        byte[] b = new byte[(int) rf.length()];
        rf.readFully(b);
        System.out.println(new String(b, StandardCharsets.UTF_8));
        rf.close();
    }
}
