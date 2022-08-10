package com.baidu.translate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HTTPUtil {
    public static String request(String url) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL _url = new URL(url);
            connection = (HttpURLConnection) _url.openConnection();
            connection.setRequestMethod("GET");

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                }
            }
            return sb.toString();
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }
}
