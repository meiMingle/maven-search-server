package coderead.maven.control;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import coderead.maven.bean.Artifact;
import coderead.maven.bean.ArtifactClass;
import coderead.maven.control.vo.SimpleArtifactInfo;
import coderead.maven.control.vo.SimpleSearchResult;
import coderead.maven.control.vo.SimpleSearchResultNew;
import coderead.maven.dao.ArtifactMapper;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
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
    public List<SimpleSearchResult> doSearch(String keyword, String appVersion) {
        if (!StringUtils.hasText(keyword)) {
            throw new HttpClientErrorException(HttpStatus.NOT_ACCEPTABLE,"参数keyword不能为空");
        }
        return this.search.search(keyword).stream()
                .map(i -> {
                    SimpleSearchResult simpleSearchResult;
                    if (!StringUtils.hasText(appVersion)) {
                        simpleSearchResult = new SimpleSearchResult();
                    }else {
                        simpleSearchResult = new SimpleSearchResultNew();
                    }
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





}
