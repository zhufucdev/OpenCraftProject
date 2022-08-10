package com.baidu.translate;

public class TransApi {
    private static final String TRANS_API_HOST = "fanyi-api.baidu.com";
    private static final String TRANS_API_PATHS = "api/trans/vip/translate";

    private final String appid;
    private final String securityKey;

    public TransApi(String appid, String securityKey) {
        this.appid = appid;
        this.securityKey = securityKey;
    }

    public String getTransResult(String query, String from, String to) {
        String url = buildParams(query, from, to);

        try {
            return HTTPUtil.request(url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String buildParams(String query, String from, String to) {
        String salt = String.valueOf(System.currentTimeMillis());
        String src = appid + query + salt + securityKey;
        return "https://" + TRANS_API_HOST + "/" + TRANS_API_PATHS + "?q" + query + "&from=" + from
                + "&to=" + to + "&appid=" + appid + "&salt=" + salt + "&sign=" + MD5.md5(src);
    }

}
