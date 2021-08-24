package coderead.maven.control;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import coderead.maven.service.ArtifactInfoStore;
import coderead.maven.bean.ArtifactIndexInfo;
import coderead.maven.search.IndexShortSearch;
import coderead.maven.service.MavenIndexManager;
import org.apache.maven.index.ArtifactInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author 鲁班大叔
 * @date 2021
 */
@Controller
@RequestMapping("/api")
public class ApiControl {

    @Autowired
    IndexShortSearch search;
    @Autowired
    MavenIndexManager indexManager;
    @Autowired
    ArtifactInfoStore versionCountStore;

    @RequestMapping("/search")
    @ResponseBody
    public List<SimpleSearchResult> doSearch(String keyword) {
        return this.search.search(keyword).stream()
                .map(i -> {
                    SimpleSearchResult simpleSearchResult = new SimpleSearchResult();
                    ArtifactIndexInfo item = i.getItem();
                    simpleSearchResult.matchIndex = i.getHighlight();
                    simpleSearchResult.matchText = item.getArtifactId() + ":" + item.getGroupId();
                    BeanUtils.copyProperties(item, simpleSearchResult);
                    return simpleSearchResult;
                }).collect(Collectors.toList());
    }

    @RequestMapping("/version")
    @ResponseBody
    public List<SimpleArtifactInfo> getVersions(String groupId, String artifactId) {
        try {
            List<ArtifactInfo> items = indexManager.search(groupId, artifactId);
            List<SimpleArtifactInfo> result = items.stream().map(i -> {
                SimpleArtifactInfo target = new SimpleArtifactInfo();
                BeanUtils.copyProperties(i, target);
                target.downloads = versionCountStore.getVersionCount(i.getGroupId(), i.getArtifactId(), i.getVersion());
                return target;
            }).collect(Collectors.toList());
            return result;
        } catch (IOException e) {
            throw new RuntimeException("版本查询失败", e);
        }
    }

    @RequestMapping("/count/version")
    @ResponseBody
    public String versionCount(String groupId, String artifactId, String versionId) {
        versionCountStore.versionCount(groupId, artifactId, versionId);
        return "OK";
    }

    private class SimpleSearchResult implements Serializable {
        int matchIndex[];    // 匹配索引
        String matchText;   // 匹配文本
        String artifactId;
        String groupId;
        long lastModified;
        String lastVersion;
        AtomicInteger hot;// 下载热度

        public int[] getMatchIndex() {
            return matchIndex;
        }

        public void setMatchIndex(int[] matchIndex) {
            this.matchIndex = matchIndex;
        }

        public String getMatchText() {
            return matchText;
        }

        public void setMatchText(String matchText) {
            this.matchText = matchText;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public long getLastModified() {
            return lastModified;
        }

        public void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }

        public String getLastVersion() {
            return lastVersion;
        }

        public void setLastVersion(String lastVersion) {
            this.lastVersion = lastVersion;
        }

        public AtomicInteger getHot() {
            return hot;
        }

        public void setHot(AtomicInteger hot) {
            this.hot = hot;
        }
    }

    private static class SimpleArtifactInfo implements Serializable {
        public String artifactId;
        public String groupId;
        public String version;
        public long lastModified = -1;
        public String packaging;
        public String name;
        public String description;
        public int downloads;

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public long getLastModified() {
            return lastModified;
        }

        public void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }

        public String getPackaging() {
            return packaging;
        }

        public void setPackaging(String packaging) {
            this.packaging = packaging;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getDownloads() {
            return downloads;
        }

        public void setDownloads(int downloads) {
            this.downloads = downloads;
        }
    }

}
