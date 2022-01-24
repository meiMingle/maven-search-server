package coderead.maven.control.vo;

import java.io.Serializable;
public class SimpleSearchResultNew extends SimpleSearchResult implements Serializable {
    public  int hot;// 下载热度
    public  String describe;// 项目描述


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