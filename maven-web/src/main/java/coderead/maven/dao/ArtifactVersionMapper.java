package coderead.maven.dao;

import coderead.maven.bean.ArtifactVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Repository
public class ArtifactVersionMapper {

    @Autowired
    JdbcTemplate template;

    /**
     * @param minCount 最小引用次数
     * @return
     */
    public List<ArtifactVersion> getVersion(int minCount) { // 最少引用次数
        String sql = "SELECT *  FROM artifact_version WHERE `count`>?";
        List<ArtifactVersion> versions = template.query(sql, new BeanPropertyRowMapper<>(ArtifactVersion.class) , minCount);
        return versions;
    }

    public void updateVersion(List<ArtifactVersion> versionCounts) {
        String sql = "update artifact_version SET `count`=`count`+? WHERE artifact=? AND version=?";
        int[][] results = template.batchUpdate(sql, versionCounts, 200, (ps, argument) -> {
            ps.setInt(1, argument.getIncrementCount().intValue());
            ps.setString(2, argument.getArtifact());
            ps.setString(3, argument.getVersion());
        });
        int[] ints = Arrays.stream(results).
                flatMapToInt(Arrays::stream).toArray();
        /**
         * 更新失败的则新增
         */
        List<ArtifactVersion> newVersions = new ArrayList<>();
        for (int i = 0; i < ints.length; i++) {
            if (ints[i] <= 0) {
                newVersions.add(versionCounts.get(i));
            }
        }
        String insertSql = "INSERT INTO `artifact_version` (`artifact`, `version`, `count`) VALUES (?, ?, ?)";
        template.batchUpdate(insertSql, newVersions, 200, (ps, argument) -> {
            ps.setString(1, argument.getArtifact());
            ps.setString(2, argument.getVersion());
            ps.setInt(3, argument.getCount());
        });
    }


}