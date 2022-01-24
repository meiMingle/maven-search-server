package coderead.maven;

import coderead.maven.search.IndexShortSearch;
import coderead.maven.service.MavenIndexManager;
import coderead.maven.service.ArtifactInfoStore;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

@SpringBootApplication
@Component
@EnableScheduling
public class MavenWebApplication {
    static final Logger logger = LoggerFactory.getLogger(MavenWebApplication.class);
    @Autowired
    IndexShortSearch indexShortSearch;
    @Autowired
    MavenIndexManager indexManager;
    @Autowired
    ArtifactInfoStore versionCountStore;

    public static void main(String[] args) {

        SpringApplication.run(MavenWebApplication.class, args);
    }

    // 索引更新计划,每周三，凌晨5点
    @Scheduled(cron = "${index.update.cron}")
    public void update() {
        try {
            logger.info("定时任务：远程索引更新");
            indexManager.update();
            logger.info("定时任务：索引加载");
            indexShortSearch.reload();
            logger.info("定时任务：索引更新成功");
        } catch (IOException e) {
            logger.error("索引更新失败", e);
        }
    }

    // 每2小时 的15分 15秒 保存状态
    @Scheduled(cron = "${index.save.cron}")
    public void saveHot() {
        try {
            logger.info("定时任务：索引状态更新");
            versionCountStore.storeVersionCount();// 保存版本使用数
            logger.info("定时任务：索引状态更新成功");
        } catch (IOException e) {
            logger.error("索引状态保存失败", e);
        }
    }
}
