package coderead.maven;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author 鲁班大叔
 * @date 2021
 */
public class StreamTest {
    @Test
    public void test() throws IOException {
        List<Integer> list=new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
        List<Integer> collect = list.stream()
                .parallel()
                .peek(a -> System.out.println(Thread.currentThread().getName() + " " + a))
                .sequential()
                .collect(Collectors.toList());
        collect.toArray();
        System.in.read();
    }
}
