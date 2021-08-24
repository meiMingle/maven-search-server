package coderead.maven.bean;

import java.util.Date;
import java.util.Random;

public class ArtifactClass implements java.io.Serializable{
    private Integer id;
    private String fullClassName;
    private String artifact;

    private String version;
    private String describe;
    private Date versionModify;

    private Date dataLastModify;

    static final Random random = new Random();

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

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe == null ? null : describe.trim();
    }

    public Date getDataLastModify() {
        return dataLastModify;
    }

    public void setDataLastModify(Date dataLastModify) {
        this.dataLastModify = dataLastModify;
    }

    public String getGroupId() {
        return artifact.split(":")[0];
    }

    public String getArtifactId() {
        return artifact.split(":")[1];
    }

    public String getFullClassName() {
        return fullClassName;
    }

    public void setFullClassName(String fullClassName) {
        this.fullClassName = fullClassName;
    }

    public Date getVersionModify() {
        return versionModify;
    }

    public void setVersionModify(Date versionModify) {
        this.versionModify = versionModify;
    }

    public String getClassname() {
        if (fullClassName == null) {
            return null;
        } else {
            String[] s = fullClassName.split(" ");
            return s[1] + "." + s[0];
        }
    }

    public void setClassname(String classname) {
        if (classname == null) {
            fullClassName = null;
            return;
        }
        int i = classname.lastIndexOf(".");
        String randomText = Integer.toString(random.nextInt(100000), Character.MAX_RADIX);
        fullClassName = classname.substring(i + 1) + " " + classname.substring(0, i) /*+ " " + randomText*/;
    }
}