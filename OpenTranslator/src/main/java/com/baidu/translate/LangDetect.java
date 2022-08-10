package com.baidu.translate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LangDetect {
    private static final String DETECT_API_HOST = "fanyi.baidu.com";
    private static final String DETECT_API_PATH = "langdetect";

    public static final String langUnknown = "unknown";

    public static String detectLanguage(String text) {
        try {
            return HTTPUtil.request("http://" + DETECT_API_HOST + "/" + DETECT_API_PATH + "?query=" + text);
        } catch (IOException e) {
            return langUnknown;
        }
    }
}
