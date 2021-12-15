package coderead.maven.control;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import cn.hutool.crypto.digest.DigestUtil;
import coderead.maven.bean.Artifact;
import coderead.maven.bean.ArtifactClass;
import coderead.maven.dao.ArtifactMapper;
import coderead.maven.search.SearchResult;
import coderead.maven.service.ArtifactInfoStore;
import coderead.maven.bean.ArtifactIndexInfo;
import coderead.maven.search.IndexShortSearch;
import coderead.maven.service.MavenIndexManager;
import coderead.maven.service.MavenSearchService;
import org.apache.maven.index.ArtifactInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author 鲁班大叔
 * @date 2021
 */
@Controller
@RequestMapping("/api")
public class ApiControl {
    static final Logger logger = LoggerFactory.getLogger(ApiControl.class);
    static final String CLASS_REGEX = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*";
    @Autowired
    ArtifactMapper mapper;
    @Autowired
    IndexShortSearch search;
    @Autowired
    MavenIndexManager indexManager;
    @Autowired
    ArtifactInfoStore versionCountStore;
    @Autowired
    ArtifactMapper artifactMapper;
    @Autowired
    MavenSearchService mavenSearchService;

    @RequestMapping("/search")
    @ResponseBody
    public List<SimpleSearchResult> doSearch(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new HttpClientErrorException(HttpStatus.NOT_ACCEPTABLE,"参数keyword不能为空");
        }
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
            if (items.isEmpty()) {
                return new ArrayList<>();
            }

            ArtifactInfo fastArtifact = items.get(0);
            Artifact artifact = artifactMapper.getArtifact(fastArtifact.getGroupId() + ":" + fastArtifact.getArtifactId());
            if (artifact != null && StringUtils.hasText(artifact.getDescribe())) {
                String description = buildDescribe(artifact);
                items.forEach(i-> i.setDescription(description));// 设置描述
            }

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

    @RequestMapping("/search/class")
    @ResponseBody
    public List<ArtifactClass> searchByClass(String keyword, Model model) {
        Assert.hasText(keyword, "搜索条件不能为空");
        keyword = keyword.trim();
        return mavenSearchService.searchByClass(keyword);
    }

    private String buildDescribe(Artifact artifact) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();

        String a = "|<a href='http://mvn.coderead.cn/redirect?site=%s'>%s</a> &nbsp ";
        if (StringUtils.hasText(artifact.getDocSite())) {
            String encode = URLEncoder.encode(artifact.getDocSite(), "UTF-8");
            builder.append(String.format(a, encode, "文档"));
        }
        if (StringUtils.hasText(artifact.getSourceSite())) {
            String sourceSite = URLEncoder.encode(artifact.getSourceSite(), "UTF-8");
            builder.append(String.format(a, sourceSite, "源码"));
        }
        builder.append(artifact.getDescribe());
        return builder.toString();
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
        int hot;// 下载热度
        public String describe;// 项目描述


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

        public int getHot() {
            return hot;
        }

        public void setHot(int hot) {
            this.hot = hot;
        }

        public String getDescribe() {
            return describe;
        }

        public void setDescribe(String describe) {
            this.describe = describe;
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
