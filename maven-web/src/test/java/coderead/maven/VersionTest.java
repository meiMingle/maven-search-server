package coderead.maven;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.stream.Collectors;

/**
 * @author 鲁班大叔
 * @date 2021
 */
public class VersionTest {
    public static void main(String[] args) throws IOException {
        String pathname = "/Users/tommy/git/coderead-maven/maven-web/src/main/resources/test.txt";
        String output = "/Users/tommy/git/coderead-maven/maven-web/src/main/resources/test2.txt";

        String collect = Files.lines(new File(pathname).toPath())
                .map(l -> {
                    String[] split = l.split(":");
                    return String.format("%s:%s %s", split[0], split[1], split[2]);
                }).collect(Collectors.joining("\r\n"));
        Files.write(new File(output).toPath(), collect.getBytes());
    }

    @Test
    public void test() throws IOException {

        String pathname = "/Users/tommy/git/coderead-maven/maven-web/src/test/resources/test.txt";
        String output = "/Users/tommy/git/coderead-maven/maven-web/src/test/resources/test2-html.txt";

        String collect = Files.lines(new File(pathname).toPath())
                .map(l -> {
                    //http://mvn.coderead.cn/version?groupId=%s&artifactId=%s
                    String[] split = l.split(":");
                    return String.format("http://mvn.coderead.cn/version?groupId=%s&artifactId=%s", split[0], split[1]);
                }).collect(Collectors.joining("\r\n"));
        Files.write(new File(output).toPath(), collect.getBytes());
    }

    @Test
    public void test3() throws IOException {
        String pathname = "/Users/tommy/git/coderead-maven/maven-web/src/test/resources/test2-html.txt";
        Files.lines(new File(pathname).toPath()).forEach(l -> {
            try {
                Thread.sleep(1000);
                System.out.println(IOUtils.toString(new URL(l), "UTF-8"));
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
