package com.print.backend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

@Service
public class TranslateService {

    private final String BAIDU_APP_ID = "20260324002579765";
    private final String BAIDU_SECRET_KEY = "CpRCqAw0FaP21kI7RXp8";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public String translateToEnglish(String chineseText) throws Exception {
        if (chineseText == null || chineseText.trim().isEmpty()) {
            return "";
        }

        String salt = String.valueOf(System.currentTimeMillis());
        String sign = generateSign(chineseText, salt);

        String url = "https://fanyi-api.baidu.com/api/trans/vip/translate" +
                "?q=" + encodeUrl(chineseText) +
                "&from=zh" +
                "&to=en" +
                "&appid=" + BAIDU_APP_ID +
                "&salt=" + salt +
                "&sign=" + sign;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            String respStr = response.body().string();
            System.out.println("百度翻译响应: " + respStr);

            JSONObject json = JSON.parseObject(respStr);
            
            if (json.containsKey("error_code")) {
                String errorMsg = json.getString("error_msg");
                throw new RuntimeException("翻译失败: " + errorMsg);
            }

            if (json.containsKey("trans_result")) {
                JSONObject transResult = json.getJSONArray("trans_result").getJSONObject(0);
                String dst = transResult.getString("dst");
                System.out.println("翻译结果: " + chineseText + " -> " + dst);
                return dst;
            }

            throw new RuntimeException("翻译结果为空");
        }
    }

    private String generateSign(String query, String salt) throws Exception {
        String signStr = BAIDU_APP_ID + query + salt + BAIDU_SECRET_KEY;
        return md5(signStr);
    }

    private String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private String encodeUrl(String text) {
        try {
            return java.net.URLEncoder.encode(text, "UTF-8");
        } catch (Exception e) {
            return text;
        }
    }
}
