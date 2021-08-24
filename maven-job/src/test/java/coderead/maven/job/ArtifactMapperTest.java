package coderead.maven.job;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import coderead.maven.job.dao.Artifact;
import coderead.maven.job.dao.ArtifactClass;
import coderead.maven.job.dao.ArtifactMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 鲁班大叔
 * @date 2021
 */
@SpringBootTest(classes = MavenJobApplication.class)
@RunWith(SpringRunner.class)
public class ArtifactMapperTest {
    @Autowired
    ArtifactMapper artifactMapper;

    @Autowired
    ClassIndexManager indexManager;

    @Test
    public void findArtifactTest() {
        List<Artifact> artifact = artifactMapper.findArtifactByState(0L, ArtifactMapper.unIndex, 2);
        Assert.assertNotNull(artifact);
    }

    @Test
    public void saveTest() throws IOException {
        List<Artifact> artifact = artifactMapper.findArtifactByState(0L, ArtifactMapper.unIndex, 2);
        String url="https://archiva-maven-storage-prod.oss-cn-beijing.aliyuncs.com/repository/central";
        List<ArtifactClass> artifactClasses = indexManager.downloadJar(url,artifact.get(0));
        artifactMapper.updateArtifactClass(artifactClasses);
    }

    @Test
    public void downloadTest() throws IOException {
        String url="https://archiva-maven-storage-prod.oss-cn-beijing.aliyuncs.com/repository/central";
//        String url="https://repo1.maven.org/maven2";

        Artifact artifact1 = new Artifact();
        artifact1.setArtifact("org.apache.karaf.archetypes:karaf-blueprint-archetype");
        artifact1.setLastVersion("4.3.2");
        List<ArtifactClass> artifactClasses = indexManager.downloadJar(url,artifact1);
        artifactMapper.updateArtifactClass(artifactClasses);
    }

    @Test
    public void jobTest() throws IOException {
        System.in.read();
    }
}
