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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ArtifactMapper {
    public static byte unIndex = 0;
    public static byte indexing = 1;
    public static byte succeed = 2;
    public static byte fail = 3;

    @Autowired
    JdbcTemplate template;

    public List<ArtifactClass> findClassBySimpleName(String simpleClassName){
        List<ArtifactClass> list =
                template.query("SELECT * FROM artifact_class WHERE fullClassName like concat(?,' %') limit 100",
                        new BeanPropertyRowMapper<>(ArtifactClass.class),
                        simpleClassName);
        return list;
    }

    public List<ArtifactClass> findClassByFullName(String fullClassName){
        Assert.isTrue(fullClassName.split("\\.").length>2, String.format("错误的类名:%s", fullClassName));
        int i = fullClassName.lastIndexOf(".");
       String dataClassName= fullClassName.substring(i + 1) + " " + fullClassName.substring(0, i);
        List<ArtifactClass> list =
                template.query("SELECT * FROM artifact_class WHERE fullClassName =? limit 100",
                        new BeanPropertyRowMapper<>(ArtifactClass.class),
                        dataClassName);
        return list;
    }

}