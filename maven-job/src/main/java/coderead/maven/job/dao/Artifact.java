package coderead.maven.job.dao;

import java.util.Date;

public class Artifact {
    private Integer id;

    private String artifact;

    private String lastVersion;

    private Date lastModify;

    private byte indexClass;    //0 待构建  1构建中 2构建成功 3构建失败

    private String describe;

    private Date dateLastModify;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact == null ? null : artifact.trim();
    }

    public String getLastVersion() {
        return lastVersion;
    }

    public void setLastVersion(String lastVersion) {
        this.lastVersion = lastVersion == null ? null : lastVersion.trim();
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public Byte getIndexClass() {
        return indexClass;
    }

    public void setIndexClass(Byte indexClass) {
        this.indexClass = indexClass;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe == null ? null : describe.trim();
    }

    public Date getDateLastModify() {
        return dateLastModify;
    }

    public void setDateLastModify(Date dateLastModify) {
        this.dateLastModify = dateLastModify;
    }

    public String getGroupId() {
        return artifact.split(":")[0];
    }

    public Object getArtifactId() {
        return artifact.split(":")[1];
    }
}