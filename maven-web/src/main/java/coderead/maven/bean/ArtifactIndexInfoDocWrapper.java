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
public class ArtifactIndexInfoDocWrapper extends ArtifactIndexInfo implements Serializable {
   private boolean haveDocDone;

    public boolean isHaveDocDone() {
        return haveDocDone;
    }

    public void setHaveDocDone(boolean haveDocDone) {
        this.haveDocDone=haveDocDone;
    }
}
