package coderead.maven;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import coderead.maven.bean.Artifact;
import coderead.maven.bean.ArtifactVersion;
import coderead.maven.dao.ArtifactMapper;
import coderead.maven.dao.ArtifactVersionMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 鲁班大叔
 * @date 2021
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = MavenWebApplication.class)
public class ArtifactVersionMapperTest {

    @Autowired
    ArtifactVersionMapper mapper;
    @Autowired
    ArtifactMapper artifactMapper;
    @Test
    public void getVersionTest() {
        List<ArtifactVersion> version = mapper.getVersion(10);
        assert !version.isEmpty();
    }

    @Test
    public void updateVersionTest() {
        List<ArtifactVersion> version = mapper.getVersion(10);
        version.stream()
                .peek(v->v.setIncrementCount(new AtomicInteger(3)))
                .filter(v->v.getCount()>200)
                .forEach(v->v.setArtifact("test_"+v.getArtifact()));
        mapper.updateVersion(version);
    }

    @Test
    public void getArtifactsTest() {
        List<String> list = Arrays.asList("com.github.handywings:hdw-common",
                "org.fusesource.fabric.fabric-examples:fabric-cxf-demo-features",
                "net.officefloor.tutorial:ActivityHttpServer",
                "com.github.fanpan26:tio-websocket-spring-boot-starter");

        List<Artifact> artifacts = artifactMapper.getArtifacts(list);
        assert artifacts.size()>0;
    }
}
