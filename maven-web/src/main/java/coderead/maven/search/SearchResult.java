package coderead.maven.search;


import coderead.maven.bean.ArtifactIndexInfo;

import java.beans.Transient;

/**
 * @author Tommy
 * Created by Tommy on 2019/10/30
 **/
public class SearchResult implements java.io.Serializable {

    private int highlight[];    //高亮显示命中字符
    private String highlightText;

    private WordIndex mathIndex; // 最后匹配的词
    private ArtifactIndexInfo item;
    public SearchResult() {
    }

    public SearchResult(ArtifactIndexInfo item) {
        this.item = item;
    }

    public int[] getHighlight() {
        return highlight;
    }

    public void setHighlight(int[] highlight) {
        this.highlight = highlight;
    }


    public ArtifactIndexInfo getItem() {
        return item;
    }

    public void setItem(ArtifactIndexInfo item) {
        this.item = item;
    }
    @Transient
    public WordIndex getMathIndex() {
        return mathIndex;
    }
    @Transient
    public void setMathIndex(WordIndex mathIndex) {
        this.mathIndex = mathIndex;
    }

    public String getHighlightText() {
        return highlightText;
    }

    public void setHighlightText(String highlightText) {
        this.highlightText = highlightText;
    }

    //r1 r2
    public int compareTo(SearchResult target) {
        if (target==this||target.item==this.item) {
            return 0;
        }

        int result = target.item.hot - this.item.hot;// 热度越高越靠前

        if (result==0) { // 匹配越靠左,越靠前
            result= this.mathIndex.getIndex()-target.mathIndex.getIndex();
        }//dubbo-qos
        if (result == 0) {// 字符越短，越靠前
            result = this.item.artifactId.length() - target.item.artifactId.length();
        }
        if (result == 0) {// 数字越短，级别越高
            result = getGroupLive(this.item.groupId) - getGroupLive(target.item.groupId);
        }
        if (result == 0) {// 更新时间越近，越靠前
            result = target.item.lastModified > this.item.lastModified ? 1 : -1;
        }
        return result;
    }

    private int getGroupLive(String groupId) {
        for (int i = 0; i < groupIds.length; i++) {
            if (groupId.startsWith(groupIds[i])) {
                return i;
            }
        }
        return groupIds.length;
    }

    private final static String[] groupIds = {"org.apache", "org.springframework", "org"};

}
