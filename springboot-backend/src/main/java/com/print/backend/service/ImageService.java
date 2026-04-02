package com.print.backend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ImageService {
    private final String DASHSCOPE_API_KEY = "sk-873052d183a943f39d7b6e7c75dd04f1";
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    public String generate(String text) throws Exception {
        return generate(text, null);
    }

    public String generate(String text, int[] tokenUsageOut) throws Exception {
        String prompt = "黑白简笔画，简单线条，易于涂色，" + text;

        JSONObject bodyJson = new JSONObject();
        bodyJson.put("model", "wan2.2-t2i-flash");
        
        JSONObject input = new JSONObject();
        input.put("prompt", prompt);
        bodyJson.put("input", input);
        
        JSONObject parameters = new JSONObject();
        parameters.put("style", "<auto>");
        parameters.put("size", "720*1280");
        parameters.put("n", 1);
        bodyJson.put("parameters", parameters);

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), bodyJson.toJSONString());
        Request req = new Request.Builder()
                .url("https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis")
                .addHeader("Authorization", "Bearer " + DASHSCOPE_API_KEY)
                .addHeader("X-DashScope-Async", "enable")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response resp = client.newCall(req).execute()) {
            String respStr = resp.body().string();
            System.out.println("通义万象创建任务响应: " + respStr);
            
            JSONObject resJson = JSON.parseObject(respStr);
            String taskId = resJson.getJSONObject("output").getString("task_id");
            
            if (taskId == null) {
                throw new RuntimeException("创建任务失败: " + respStr);
            }

            String imageUrl = pollTaskResult(taskId);
            
            if (tokenUsageOut != null) {
                tokenUsageOut[0] = 100;
            }

            return imageUrl;
        }
    }

    private String pollTaskResult(String taskId) throws Exception {
        String getUrl = "https://dashscope.aliyuncs.com/api/v1/tasks/" + taskId;
        
        for (int i = 0; i < 60; i++) {
            Request req = new Request.Builder()
                    .url(getUrl)
                    .addHeader("Authorization", "Bearer " + DASHSCOPE_API_KEY)
                    .get()
                    .build();

            try (Response resp = client.newCall(req).execute()) {
                String respStr = resp.body().string();
                JSONObject resJson = JSON.parseObject(respStr);
                JSONObject output = resJson.getJSONObject("output");
                
                if (output == null) {
                    Thread.sleep(1000);
                    continue;
                }
                
                String status = output.getString("task_status");
                System.out.println("任务状态: " + status);
                
                if ("SUCCEEDED".equals(status)) {
                    String url = output.getJSONArray("results").getJSONObject(0).getString("url");
                    System.out.println("图片URL: " + url);
                    return downloadAndConvertToBase64(url);
                } else if ("FAILED".equals(status)) {
                    String msg = output.getString("message");
                    throw new RuntimeException("生成失败: " + msg);
                }
            }
            
            Thread.sleep(2000);
        }
        
        throw new RuntimeException("生成超时");
    }

    private String downloadAndConvertToBase64(String url) throws Exception {
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = client.newCall(req).execute()) {
            byte[] bytes = resp.body().bytes();
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bytes);
        }
    }
}
