package coderead.maven;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import coderead.maven.bean.Artifact;
import coderead.maven.bean.ArtifactClass;
import coderead.maven.dao.ArtifactMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author 鲁班大叔
 * @date 2021
 */
@SpringBootTest(classes = MavenWebApplication.class)
@RunWith(SpringRunner.class)
public class DaoTest {

    @Autowired
    ArtifactMapper mapper;

    @Test
    public void addArtifactTest() {
        Artifact record=new Artifact();
        record.setArtifact("org.dubbo:dubbo");
        record.setIndexClass((byte) 1);
        record.setLastVersion("2.6.1");
        record.setLastModify(new Date());

    }

    @Test
    public void findClasstest() {
        List<ArtifactClass> list = mapper.findClassBySimpleName("DispatcherServlet");
        assert !list.isEmpty();
    }

    @Test
    public void getArtifactTest() {
        Artifact artifact = mapper.getArtifact("org.apache.dubbo:dubbo");
        assert artifact != null : "artifact不为空";
        Artifact artifact1 = mapper.getArtifact("org.dubbo:dubbo22222");
        assert  artifact1 ==null;
    }



    public static void main(String[] args) throws IOException {
        // 洗数据
        String old = "/Users/tommy/git/cbtu-web-ide/maven-web/src/test/resources/short.txt";
        String newFile = "/Users/tommy/git/cbtu-web-ide/maven-web/src/test/resources/short2.txt";
        PrintWriter print=new PrintWriter(new FileWriter(new File(newFile)));
        print.println("artifact|last|version");
        Files.lines(new File(old).toPath())
                .map(l-> {
                    String[] s = l.split(" ");
                    return String.format("%s:%s|%s|%s",s[0],s[1],new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong(s[2]))),s[3]);
                }).forEach(line-> print.println(line));
        print.close();
    }



}
