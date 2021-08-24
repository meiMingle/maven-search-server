package coderead.maven.control;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import coderead.maven.bean.ArtifactClass;
import coderead.maven.bean.ArtifactIndexInfo;
import coderead.maven.dao.ArtifactMapper;
import coderead.maven.service.ArtifactDocService;
import coderead.maven.service.ArtifactInfoStore;
import coderead.maven.service.MavenIndexManager;
import coderead.maven.search.IndexShortSearch;
import coderead.maven.search.SearchResult;
import org.apache.maven.index.ArtifactInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author 鲁班大叔
 * @date 2021
 */
@Controller
public class MavenSearchControl {
    static final String CLASS_REGEX = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*";

    @Autowired
    IndexShortSearch search;
    @Autowired
    MavenIndexManager indexManager;
    @Autowired
    ArtifactInfoStore versionCountStore;
    @Autowired
    ArtifactMapper mapper;
    @Autowired
    ArtifactDocService docService;

    @RequestMapping("/index.html")
    public String openSearch(Model model) {
        String format = new SimpleDateFormat("yyyy-MM-dd").format(indexManager.getLastTimestamp());
        model.addAttribute("lastModify", format);
        model.addAttribute("count", indexManager.getSize());
        model.addAttribute("addressUrl", indexManager.getRespositoryUrl());
        return "ResourceTree";
    }

    @RequestMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    @RequestMapping("/search")
    public String doSearch(String keyword, Model model, HttpServletRequest request) {
        List<SearchResult> results = this.search.search(keyword);
        for (SearchResult searchResult : results) {
            renderingHighlight(searchResult);
        }
        results.parallelStream().forEach(r -> {
            ArtifactIndexInfo item = r.getItem();
            if (docService.existsInexDoc(item.groupId, item.artifactId)) {
                String indexDoc = docService.getIndexDoc(item.groupId, item.artifactId);
                indexDoc = indexDoc.substring(0, Math.min(100, indexDoc.length()));
                item.setDescribe(indexDoc);
            }
        });
        model.addAttribute("results", results);
        return "searchResultJson";
    }


    @RequestMapping("/version")
    public String getVersions(String groupId, String artifactId, Model model) {
        try {
            List<ArtifactInfo> items = indexManager.search(groupId, artifactId);
            items.forEach(a -> {
                int versionCount = versionCountStore.getVersionCount(a.getGroupId(), a.getArtifactId(), a.getVersion());
                a.getAttributes().put("versionCount", String.valueOf(versionCount));
            });
            model.addAttribute("groupId", groupId);
            model.addAttribute("artifactId", artifactId);
            model.addAttribute("items", items);
            if (docService.existsInexDoc(groupId, artifactId)) {
                String indexDocToHtml = docService.getIndexDocToHtml(groupId, artifactId);
                model.addAttribute("docHtml", indexDocToHtml);
            }

        } catch (IOException e) {
            throw new RuntimeException("版本查询失败", e);
        }
        return "versionList";
    }

    // 先不要删除，该方法用于配置告警
    @Deprecated
    @RequestMapping("/count/artifact")
    @ResponseBody
    public String artifactCount(String groupId, String artifactId) {
        return "OK";
    }

    @RequestMapping("/count/version")
    @ResponseBody
    public String versionCount(String groupId, String artifactId, String versionId) {
        versionCountStore.versionCount(groupId, artifactId, versionId);
        return "OK";
    }

    private void renderingHighlight(SearchResult s) {
        String name = s.getItem().getArtifactId() + ":" + s.getItem().getGroupId();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.toCharArray().length; i++) {
            final int final_i = i;
            if (Arrays.stream(s.getHighlight()).anyMatch(h -> h == final_i)) {
                builder.append("<em>");
                builder.append(name.charAt(i));
                builder.append("</em>");
            } else {
                builder.append(name.charAt(i));
            }
        }
        s.setHighlightText(builder.toString());
    }

    @RequestMapping("/search/class")
    public String searchByClass(String keyword, Model model) {
        Assert.hasText(keyword, "搜索条件不能为空");
        keyword = keyword.trim();
        List<ArtifactClass> list;
        // 首字母大写，且不包含 .    基于类名查找

        if (!keyword.matches(CLASS_REGEX)) { //非法的类名
            list = new ArrayList<>();
        } else if (keyword.split("\\.").length > 2) {
            list = mapper.findClassByFullName(keyword);
        } else { // 基于类名搜索
            list = mapper.findClassBySimpleName(keyword);
        }
        list.forEach(a -> {
            ArtifactIndexInfo artifact = versionCountStore.findArtifact(a.getGroupId(), a.getArtifactId());
            if (artifact != null) {
                a.setVersionModify(new Date(artifact.getLastModified()));//版本变更时间
            }
        });

        list.sort((o1, o2) -> {
            ArtifactIndexInfo a1 = versionCountStore.findArtifact(o1.getGroupId(), o1.getArtifactId());
            ArtifactIndexInfo a2 = versionCountStore.findArtifact(o2.getGroupId(), o2.getArtifactId());
            if (a1 == null || a2 == null) {
                return 1;
            }
            int result = a2.hot - a1.hot;
            if (result == 0) {//基于时间倒序
                result = a2.getLastModified() > a1.getLastModified() ? 1 : -1;
            }
            return result;
        });
        model.addAttribute("results", list);
        // 是否包含特殊字符
        return "classResultJson";
    }


    @RequestMapping("/list")
    public String list(Integer page, Integer size,Model model) {
        page = page == null || page < 1 ? 1 : page;
        size = size == null ? 100 : size;
        List<ArtifactIndexInfo> allArtifact = versionCountStore.getAllArtifact(false);
        List<ArtifactIndexInfo> items= allArtifact
                .stream()
                .sorted((a1,a2)->a2.getHot()-a1.getHot())
                .skip((page - 1) * size)
                .limit(size)
                .collect(Collectors.toList());
        model.addAttribute("items",items);
        model.addAttribute("page",page);
        model.addAttribute("upPage",page-1);
        model.addAttribute("nextPage",page+1);
        model.addAttribute("total",allArtifact.size());
        return "list";
    }


}
