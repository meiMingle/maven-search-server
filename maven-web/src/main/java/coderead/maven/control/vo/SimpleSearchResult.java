package coderead.maven.control.vo;

import java.io.Serializable;

public class SimpleSearchResult implements Serializable {
    public int matchIndex[];    // 匹配索引
    public String matchText;   // 匹配文本
    public String artifactId;
    public String groupId;
    public long lastModified;
    public String lastVersion;

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
















}