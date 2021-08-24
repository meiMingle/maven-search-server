package coderead.maven.job.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Repository
public class ArtifactMapper {
    public static byte unIndex = 0;
    public static byte indexing = 1;
    public static byte succeed = 2;
    public static byte fail = 3;

    @Autowired
    JdbcTemplate template;

    /**
     * 基于索引的构建状态 查找id
     *
     * @param startId
     * @param indexClassState
     * @param size
     * @return
     */
    public List<Artifact> findArtifactByState(Long startId,
                                              Byte indexClassState,
                                              int size) {
        startId = startId == null ? 0L : startId;
        List<Artifact> result =
                template.query("select  * from  artifact where id>? and indexClass=? limit ?",
                        new BeanPropertyRowMapper<>(Artifact.class), startId, indexClassState, size);
        //select  * from  artifact where id
        return result;
    }

    /**
     * 批量更新 状态
     *
     * @param ids
     * @param indexClassState
     */
    public void setArtifactSate(Integer[] ids, Byte indexClassState) {
        template.batchUpdate("update  artifact set indexClass=? where id=?", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setByte(1, indexClassState);
                ps.setInt(2, ids[i]);
            }

            @Override
            public int getBatchSize() {
                return ids.length;
            }
        });
    }


    /**
     *
     */
    @Transactional
    public synchronized void updateArtifactClass(List<ArtifactClass> list) {
        long begin = System.currentTimeMillis();
        //删除指定组件下所有Class
        template.update("delete from artifact_class where artifact=?", list.get(0).getArtifact());
        System.err.println(String.format("数量:%s 删除用时:%s", list.size(),(System.currentTimeMillis()-begin)));


        // 批量新增
        template.batchUpdate("insert into artifact_class (fullClassName, artifact, version) values (?,?,?)", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                String fullClassName = list.get(i).getFullClassName();
                fullClassName=fullClassName.substring(0,Math.min(fullClassName.length(),250));// 超出部分裁掉
                ps.setString(1, fullClassName);
                ps.setString(2, list.get(i).getArtifact());//
                ps.setString(3, list.get(i).getVersion());
            }
            @Override
            public int getBatchSize() {
                return list.size();
            }
        });
        System.err.println(String.format("数量:%s 新增用时:%s", list.size(),(System.currentTimeMillis()-begin)));

        // 修改状态
        template.update("update artifact set indexClass=? where artifact=?", succeed,
                list.get(0).getArtifact());

        System.err.println(String.format("数量:%s 总用时:%s", list.size(),(System.currentTimeMillis()-begin)));
    }

    //


}