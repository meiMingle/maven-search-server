package coderead.maven.control;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import coderead.maven.bean.ArtifactClass;
import coderead.maven.bean.ArtifactIndexInfo;
import coderead.maven.bean.ArtifactIndexInfoDocWrapper;
import coderead.maven.dao.ArtifactMapper;
import coderead.maven.search.IndexShortSearch;
import coderead.maven.search.SearchResult;
import coderead.maven.service.ArtifactDocService;
import coderead.maven.service.ArtifactInfoStore;
import coderead.maven.service.MavenIndexManager;
import org.apache.maven.index.ArtifactInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 鲁班大叔
 * @date 2021
 */
@Controller
public class MavenSearchControlM {

    @Autowired
    ArtifactInfoStore versionCountStore;


    @Value("${doc.root}")
    String docRoot;


    @RequestMapping("/list-m")
    public String list(Integer page, Integer size,Model model,String filter) {
        page = page == null || page < 1 ? 1 : page;
        size = size == null ? 100 : size;
        List<ArtifactIndexInfo> allArtifact = versionCountStore.getAllArtifact(false);

        List<ArtifactIndexInfo> items= allArtifact
                .stream()
                .sorted((a1,a2)->a2.getHot()-a1.getHot())
                .skip((page - 1) * size)
                .limit(size)
                .map( item->{
                    ArtifactIndexInfoDocWrapper docWrapper=new ArtifactIndexInfoDocWrapper();
                    BeanUtils.copyProperties( item,docWrapper );
                    docWrapper.setHaveDocDone(
                            isDocumented( docRoot+docWrapper.getGroupId()+File.separator+docWrapper.getArtifactId()+File.separator+"index.md" )
                    );
                    return docWrapper;} )
                .collect(Collectors.toList());
        model.addAttribute("items",items);
        model.addAttribute("page",page);
        model.addAttribute("upPage",page-1);
        model.addAttribute("nextPage",page+1);
        model.addAttribute("total",allArtifact.size());
        return "list-m";
    }

    public boolean isDocumented(String path){
        return new File( path ).exists();
    }


    @GetMapping("/mkdir/{group}/{artifactId}")
    @ResponseBody
    public String mkdir(@PathVariable String group,@PathVariable String artifactId)  {
        String fullPath = docRoot + group + File.separator + artifactId + File.separator + "index.md";
        String parentPath = docRoot + group + File.separator + artifactId;
        File file=new File(  fullPath);
        if (!file.exists()){
            if (new File( parentPath).mkdirs()) {
                try {
                    if (new File( fullPath ).createNewFile()) {
                          return "created!";
                    }else{
                        return "fail";
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return "fail";
                }

            }else{
                return "fail";
            }
        }else{
            return "alreadyExists";
        }

    }


}
