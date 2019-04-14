package com.baidu.translate;

import okhttp3.*;

import java.io.IOException;

public class LangDetect {
    private static final String DETECT_API_HOST = "fanyi.baidu.com";
    private static final String DETECT_API_PATH = "langdetect";

    public static final String langUnknown = "unknown";
    public static String detectLanguage(String text) {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host(DETECT_API_HOST)
                .addPathSegment(DETECT_API_PATH)
                .addQueryParameter("query",text)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();
        try {
            Response response = TransApi.client.newCall(request).execute();
            if (response.isSuccessful()){
                return response.body().string();
            } else {
                return langUnknown;
            }
        } catch (IOException e){
            return langUnknown;
        }
    }
}
