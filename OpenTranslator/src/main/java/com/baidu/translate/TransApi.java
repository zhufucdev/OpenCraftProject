package com.baidu.translate;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TransApi {
    static final OkHttpClient client = new OkHttpClient();
    private static final String TRANS_API_HOST = "fanyi-api.baidu.com";
    private static final String TRANS_API_PATHS = "api/trans/vip/translate";

    private String appid;
    private String securityKey;

    public TransApi(String appid, String securityKey) {
        this.appid = appid;
        this.securityKey = securityKey;
    }

    public String getTransResult(String query, String from, String to) {
        Request request = new Request.Builder()
                .url(buildParams(query, from, to))
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()){
                return response.body().string();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private HttpUrl buildParams(String query, String from, String to) {
        // 随机数
        String salt = String.valueOf(System.currentTimeMillis());
        // 签名
        String src = appid + query + salt + securityKey; // 加密前的原文

        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host(TRANS_API_HOST)
                .addEncodedPathSegments(TRANS_API_PATHS)
                .addQueryParameter("q",query)
                .addQueryParameter("from",from)
                .addQueryParameter("to",to)
                .addQueryParameter("appid",appid)
                .addQueryParameter("salt",salt)
                .addQueryParameter("sign",MD5.md5(src))
                .build();
        return url;
    }

}
