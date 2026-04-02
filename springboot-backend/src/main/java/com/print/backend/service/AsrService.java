package com.print.backend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class AsrService {

    private final String BAIDU_API_KEY = "2YuhpTgEqyIy0V8PDyJzQkBx";
    private final String BAIDU_SECRET_KEY = "tw21a2b07ugG8CdCfpEitFY4dZJZsdER";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public String recognize(MultipartFile file) throws Exception {
        byte[] audioData = file.getBytes();
        System.out.println("收到音频文件大小: " + audioData.length);

        if (audioData.length < 1000) {
            throw new RuntimeException("音频文件太短");
        }

        // 跳过 WAV 文件头（44字节）
        byte[] rawData = audioData;
        if (audioData.length > 44 && audioData[0] == 'R' && audioData[1] == 'I' && audioData[2] == 'F' && audioData[3] == 'F') {
            rawData = new byte[audioData.length - 44];
            System.arraycopy(audioData, 44, rawData, 0, rawData.length);
            System.out.println("跳过WAV头后音频大小: " + rawData.length);
        }

        return recognizeWithBaidu(rawData);
    }

    private String recognizeWithBaidu(byte[] rawData) throws Exception {
        // 获取百度 access_token
        String tokenUrl = "https://aip.baidubce.com/oauth/2.0/token";
        String tokenParams = "grant_type=client_credentials&client_id=" + BAIDU_API_KEY + "&client_secret=" + BAIDU_SECRET_KEY;

        RequestBody tokenBody = RequestBody.create(null, new byte[0]);
        Request tokenReq = new Request.Builder()
            .url(tokenUrl + "?" + tokenParams)
            .post(tokenBody)
            .build();

        String accessToken;
        try (Response tokenResp = client.newCall(tokenReq).execute()) {
            String respStr = tokenResp.body().string();
            System.out.println("百度Token响应: " + respStr);
            JSONObject json = JSON.parseObject(respStr);
            accessToken = json.getString("access_token");
            if (accessToken == null || accessToken.isEmpty()) {
                throw new RuntimeException("获取百度access_token失败: " + respStr);
            }
        }

        // 调用百度 ASR
        String asrUrl = "https://vop.baidu.com/server_api";
        String base64Audio = Base64.getEncoder().encodeToString(rawData);

        JSONObject asrBody = new JSONObject();
        asrBody.put("format", "pcm");
        asrBody.put("rate", 16000);
        asrBody.put("channel", 1);
        asrBody.put("cuid", "aidbox-backend");
        asrBody.put("token", accessToken);
        asrBody.put("dev_pid", 1537);  // 中文普通话
        asrBody.put("speech", base64Audio);
        asrBody.put("len", rawData.length);

        RequestBody asrRequestBody = RequestBody.create(
            MediaType.parse("application/json"),
            asrBody.toJSONString()
        );

        Request asrRequest = new Request.Builder()
            .url(asrUrl)
            .post(asrRequestBody)
            .build();

        try (Response asrResp = client.newCall(asrRequest).execute()) {
            byte[] respBytes = asrResp.body().bytes();
            String respStr = new String(respBytes, StandardCharsets.UTF_8);
            System.out.println("百度ASR响应: " + respStr);

            JSONObject result = JSON.parseObject(respStr);
            Integer errNo = result.getInteger("err_no");

            if (errNo != null && errNo == 0) {
                // 百度返回 result 是数组格式字符串，如 ["识别文字"]
                String text = result.getString("result");
                if (text != null && !text.isEmpty()) {
                    // 去掉方括号和引号
                    text = text.replaceAll("^\\[|\\]$", "").replaceAll("^\"|\"$", "");
                    System.out.println("识别结果: " + text);
                    return text;
                }
                throw new RuntimeException("百度识别结果为空");
            } else {
                String errMsg = result.getString("err_msg");
                throw new RuntimeException("百度报错: err_no=" + errNo + ", msg=" + errMsg);
            }
        }
    }
}
