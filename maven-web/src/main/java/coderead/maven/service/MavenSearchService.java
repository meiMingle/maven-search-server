package coderead.maven.service;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import coderead.maven.bean.ArtifactClass;
import coderead.maven.bean.ArtifactIndexInfo;
import coderead.maven.dao.ArtifactMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author 鲁班大叔
 * @date 2021
 */
@Service
public class MavenSearchService {
    static final String CLASS_REGEX = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*";
    @Autowired
    ArtifactMapper mapper;
    @Autowired
    ArtifactInfoStore versionCountStore;

    public List<ArtifactClass> searchByClass(String keyword) {
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

        return list;
    }
}
