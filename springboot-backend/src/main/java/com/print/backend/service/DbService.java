package com.print.backend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class DbService {
    private final String DB_PATH = "data/store.json";

    @PostConstruct
    public void init() throws Exception {
        File dir = new File("data");
        if (!dir.exists()) dir.mkdirs();
        File dbFile = new File(DB_PATH);
        if (!dbFile.exists()) {
            JSONObject initObj = new JSONObject();
            initObj.put("images", new JSONArray());
            Files.write(Paths.get(DB_PATH), initObj.toJSONString().getBytes());
        }
    }

    public synchronized JSONObject readDb() throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(DB_PATH)));
        return JSON.parseObject(content);
    }

    public synchronized void writeDb(JSONObject db) throws Exception {
        Files.write(Paths.get(DB_PATH), db.toJSONString().getBytes());
    }
}
