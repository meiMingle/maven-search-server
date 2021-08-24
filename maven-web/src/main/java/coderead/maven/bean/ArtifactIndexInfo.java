package coderead.maven.bean;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * @author 鲁班大叔
 * @date 2021
 */
public class ArtifactIndexInfo implements Serializable {
    public String artifactId;
    public String groupId;
    public long lastModified;
    public String lastVersion;
    public boolean indexClass;// 是否已构建class索引
    public int hot;// 热度，基于版本使用次数累加而来
    public String describe;// 项目描述

    public ArtifactIndexInfo() {
    }

    // groupId artifactId lastModified lastVersion indexClass
    public static ArtifactIndexInfo parse(String line) throws IllegalArgumentException {
        Assert.hasText(line, "格式错误");
        String[] s = line.split(" ");
        Assert.isTrue(s.length == 5, "格式错误：" + line);
    /*    Assert.isTrue(Arrays.asList("true", "false")
                        .contains(s[4].toLowerCase()),
                String.format("格式错误：%s '%s'必须为boolean", line, s[4]));
*/
        ArtifactIndexInfo info = new ArtifactIndexInfo();
        info.groupId = s[0];
        info.artifactId = s[1];
        info.lastModified = Long.parseLong(s[2]);
        info.lastVersion = s[3];
        info.indexClass = Boolean.valueOf(s[4]);
        return info;
    }

    // 序列化
    public static String toLine(ArtifactIndexInfo info) {
        StringBuilder builder = new StringBuilder();
        builder.append(info.groupId);
        builder.append(" ");
        builder.append(info.artifactId);
        builder.append(" ");
        builder.append(info.lastModified);
        builder.append(" ");
        builder.append(info.lastVersion);
        builder.append(" ");
        builder.append(info.indexClass);
        return builder.toString();
    }

    public ArtifactIndexInfo(String groupId, String artifactId) {
        this.artifactId = artifactId;
        this.groupId = groupId;
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

    public boolean isIndexClass() {
        return indexClass;
    }

    public void setIndexClass(boolean indexClass) {
        this.indexClass = indexClass;
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
