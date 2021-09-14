package coderead.maven.bean;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class ArtifactVersion {
    private Integer id;

    private String artifact;

    private String version;

    private Date lastModify;

    private Integer count;

    private AtomicInteger incrementCount=new AtomicInteger(0);

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
        this.artifact = artifact;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version == null ? null : version.trim();
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Date getDateLastModify() {
        return dateLastModify;
    }

    public void setDateLastModify(Date dateLastModify) {
        this.dateLastModify = dateLastModify;
    }

    public AtomicInteger getIncrementCount() {
        return incrementCount;
    }

    public void setIncrementCount(AtomicInteger incrementCount) {
        this.incrementCount = incrementCount;
    }
}