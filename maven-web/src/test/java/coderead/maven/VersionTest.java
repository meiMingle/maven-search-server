package coderead.maven;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import java.io.File;
import java.io.IOException;
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
        Files.write(new File(output).toPath(),collect.getBytes());
    }
}
