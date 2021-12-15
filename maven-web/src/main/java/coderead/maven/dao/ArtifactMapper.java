package coderead.maven.dao;

import coderead.maven.bean.Artifact;
import coderead.maven.bean.ArtifactClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public class ArtifactMapper {
    public static byte unIndex = 0;
    public static byte indexing = 1;
    public static byte succeed = 2;
    public static byte fail = 3;

    @Autowired
    JdbcTemplate template;

    public List<ArtifactClass> findClassBySimpleName(String simpleClassName) {
        List<ArtifactClass> list =
                template.query("SELECT * FROM artifact_class WHERE fullClassName like concat(?,' %') limit 100",
                        new BeanPropertyRowMapper<>(ArtifactClass.class),
                        simpleClassName);
        return list;
    }

    public List<ArtifactClass> findClassByFullName(String fullClassName) {
        Assert.isTrue(fullClassName.split("\\.").length > 2, String.format("错误的类名:%s", fullClassName));
        int i = fullClassName.lastIndexOf(".");
        String dataClassName = fullClassName.substring(i + 1) + " " + fullClassName.substring(0, i);
        List<ArtifactClass> list =
                template.query("SELECT * FROM artifact_class WHERE fullClassName =? limit 100",
                        new BeanPropertyRowMapper<>(ArtifactClass.class),
                        dataClassName);
        return list;
    }

    public Artifact getArtifact(String artifact) {
        String sql = "select  * from artifact where artifact=?";
        List<Artifact> query = template.query(sql, new BeanPropertyRowMapper<>(Artifact.class), artifact);
        if (query.isEmpty()) {
            return null;
        }
        Assert.isTrue(query.size() == 1, "artifact 应该只有一条记录，但出现了多条 ：" + artifact);
        return query.get(0);
    }

    public List<Artifact> getArtifacts(List<String> artifacts) {
        Assert.notNull(artifacts, "artifacts 参数不能为空");
        Assert.notEmpty(artifacts, "artifacts 参数集合不能为空");
        Assert.isTrue(artifacts.stream().allMatch(StringUtils::hasText),"artifacts 参数列表中存在空项");
        String placeholder = artifacts.stream().map(s -> "?").collect(Collectors.joining(","));
        String sql = String.format("select  * from artifact where artifact in(%s)", placeholder);
        return template.query(sql, new BeanPropertyRowMapper<>(Artifact.class), artifacts.toArray());
    }


}