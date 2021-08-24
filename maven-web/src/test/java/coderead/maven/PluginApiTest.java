package coderead.maven;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import okhttp3.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;

/**
 * @author 鲁班大叔
 * @date 2021
 */
public class PluginApiTest {
    @Test
    public void test() {
        String url = "http://127.0.0.1:8080/api";

        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody body=new FormBody.Builder()
                .addEncoded("groupId","io.openbouquet")
                .addEncoded("artifactId","HikariCP")
                .add("versionId","2.4.2-RC2+SquidSolutions")
                .build();
        final Request request = new Request.Builder()
                .cacheControl(new CacheControl.Builder()
                        .noStore()
                        .build())
                .post(body)
                .url(url + "/count/version")
                .build();
        final Call call = okHttpClient.newCall(request);
        try {
            call.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
