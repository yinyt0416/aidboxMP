package com.print.backend.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.print.backend.entity.ImageRecord;
import com.print.backend.entity.User;
import com.print.backend.mapper.ImageRecordMapper;
import com.print.backend.mapper.UserMapper;
import com.print.backend.service.AsrService;
import com.print.backend.service.DbService;
import com.print.backend.service.ImageService;
import com.print.backend.service.TranslateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private AsrService asrService;
    @Autowired
    private TranslateService translateService;
    @Autowired
    private ImageService imageService;
    @Autowired
    private ImageRecordMapper imageMapper;
    @Autowired
    private UserMapper userMapper;

    private final String WX_APPID = "wx701e3e3f4792052b";
    private final String WX_SECRET = "b55bce6e131dfb3203f7cffa955d98b3";
    private final okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

    @GetMapping("/health")
    public Object health() {
        return new JSONObject().fluentPut("ok", true).fluentPut("service", "springboot-backend");
    }

    @PostMapping("/auth/login")
    public Object login(@RequestBody(required = false) JSONObject req, HttpSession session) {
        try {
            if (req == null) {
                return ResponseEntity.status(400).body(new JSONObject().fluentPut("message", "请求体不能为空"));
            }

            String code = req.getString("code");
            String username = req.getString("username");
            String password = req.getString("password");

            // 微信小程序登录
            if (code != null && !code.isEmpty()) {
                String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + WX_APPID + "&secret=" + WX_SECRET + "&js_code=" + code + "&grant_type=authorization_code";
                okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
                try (okhttp3.Response response = client.newCall(request).execute()) {
                    String resBody = response.body().string();
                    JSONObject wxRes = com.alibaba.fastjson2.JSON.parseObject(resBody);
                    if (wxRes.containsKey("errcode") && wxRes.getInteger("errcode") != 0) {
                        System.err.println("微信登录失败: " + resBody);
                        return ResponseEntity.status(400).body(new JSONObject().fluentPut("message", "微信登录失败: " + wxRes.getString("errmsg")));
                    }

                    String openid = wxRes.getString("openid");
                    if (openid == null || openid.isEmpty()) {
                        System.err.println("微信登录失败: 未获取到openid, response=" + resBody);
                        return ResponseEntity.status(400).body(new JSONObject().fluentPut("message", "微信登录失败: 未获取到openid"));
                    }

                    String nickname = req.getString("nickname") != null ? req.getString("nickname") : "微信用户";
                    String avatar = req.getString("avatar") != null ? req.getString("avatar") : "";

                    QueryWrapper<User> qw = new QueryWrapper<>();
                    qw.eq("openid", openid);
                    User wxUser = userMapper.selectOne(qw);

                    if (wxUser == null) {
                        wxUser = new User();
                        wxUser.setOpenid(openid);
                        wxUser.setUsername("wx_" + openid.substring(0, Math.min(16, openid.length())));
                        wxUser.setNickname(nickname);
                        wxUser.setAvatar(avatar);
                        wxUser.setCreatedAt(System.currentTimeMillis());
                        try {
                            userMapper.insert(wxUser);
                        } catch (Exception e) {
                            System.err.println("创建用户失败: " + e.getMessage());
                            return ResponseEntity.status(500).body(new JSONObject().fluentPut("message", "创建用户失败: " + e.getMessage()));
                        }
                    } else {
                        boolean needUpdate = false;
                        if (nickname != null && !nickname.isEmpty() 
                                && !nickname.equals("微信用户") && !nickname.equals("用户")
                                && !nickname.equals(wxUser.getNickname())) {
                            wxUser.setNickname(nickname);
                            needUpdate = true;
                        }
                        if (avatar != null && !avatar.isEmpty() && !avatar.equals(wxUser.getAvatar())) {
                            wxUser.setAvatar(avatar);
                            needUpdate = true;
                        }
                        if (needUpdate) {
                            userMapper.updateById(wxUser);
                        }
                    }

                    String token = "token_" + wxUser.getId() + "_" + System.currentTimeMillis();

                    // 存入 session
                    session.setAttribute("userId", wxUser.getId());

                    JSONObject userJson = new JSONObject()
                            .fluentPut("id", wxUser.getId())
                            .fluentPut("username", wxUser.getUsername())
                            .fluentPut("nickname", wxUser.getNickname())
                            .fluentPut("avatar", wxUser.getAvatar())
                            .fluentPut("openid", openid);

                    System.out.println("login return user: " + userJson.toJSONString());

                    return new JSONObject()
                            .fluentPut("token", token)
                            .fluentPut("user", userJson);
                }
            }

            // 用户名密码登录
            if (username != null && password != null) {
                QueryWrapper<User> qw = new QueryWrapper<>();
                qw.eq("username", username);
                User user = userMapper.selectOne(qw);

                if (user == null) {
                    return ResponseEntity.status(401).body(new JSONObject().fluentPut("message", "用户名或密码错误"));
                }

                // 直接比对明文密码
                if (!password.equals(user.getPassword())) {
                    return ResponseEntity.status(401).body(new JSONObject().fluentPut("message", "用户名或密码错误"));
                }

                // 生成 token
                String token = "token_" + user.getId() + "_" + System.currentTimeMillis();

                // 存入 session
                session.setAttribute("userId", user.getId());
                session.setAttribute("username", user.getUsername());

                JSONObject userJson = new JSONObject()
                        .fluentPut("id", user.getId())
                        .fluentPut("username", user.getUsername());

                return new JSONObject()
                        .fluentPut("token", token)
                        .fluentPut("user", userJson);
            }

            return ResponseEntity.status(400).body(new JSONObject().fluentPut("message", "缺少登录参数"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new JSONObject().fluentPut("message", "登录异常: " + e.getMessage()));
        }
    }

    @PostMapping("/auth/logout")
    public Object logout(HttpSession session) {
        session.invalidate();
        return new JSONObject().fluentPut("success", true);
    }

    @GetMapping("/auth/me")
    public Object me(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(new JSONObject().fluentPut("message", "未登录"));
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return ResponseEntity.status(401).body(new JSONObject().fluentPut("message", "用户不存在"));
        }

        JSONObject userJson = new JSONObject()
                .fluentPut("id", user.getId())
                .fluentPut("username", user.getUsername())
                .fluentPut("nickname", user.getNickname())
                .fluentPut("avatar", user.getAvatar());

        return new JSONObject().fluentPut("user", userJson);
    }

    @PostMapping("/user/update")
    public Object updateUserInfo(@RequestBody JSONObject req, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String openid = req.getString("openid");
        
        System.out.println("updateUserInfo called, openid: " + openid + ", session userId: " + userId);
        
        if (userId == null && openid != null && !openid.isEmpty()) {
            QueryWrapper<User> qw = new QueryWrapper<>();
            qw.eq("openid", openid);
            User user = userMapper.selectOne(qw);
            if (user != null) {
                userId = user.getId();
                System.out.println("found userId by openid: " + userId);
            }
        }
        
        if (userId == null) {
            System.out.println("userId is null, returning 401");
            return ResponseEntity.status(401).body(new JSONObject().fluentPut("message", "未登录"));
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return ResponseEntity.status(401).body(new JSONObject().fluentPut("message", "用户不存在"));
        }

        String nickname = req.getString("nickname");
        String avatar = req.getString("avatar");
        
        System.out.println("nickname: " + nickname + ", avatar: " + avatar);
        System.out.println("db nickname: " + user.getNickname() + ", db avatar: " + user.getAvatar());
        
        boolean needUpdate = false;
        if (nickname != null && !nickname.isEmpty() && !nickname.equals(user.getNickname())) {
            user.setNickname(nickname);
            needUpdate = true;
        }
        if (avatar != null && !avatar.isEmpty() && !avatar.equals(user.getAvatar())) {
            user.setAvatar(avatar);
            needUpdate = true;
        }
        
        System.out.println("needUpdate: " + needUpdate);
        
        if (needUpdate) {
            int rows = userMapper.updateById(user);
            System.out.println("updateById returned: " + rows + " rows affected");
            System.out.println("user updated: id=" + user.getId() + ", nickname=" + user.getNickname() + ", avatar=" + user.getAvatar());
        }

        JSONObject userJson = new JSONObject()
                .fluentPut("id", user.getId())
                .fluentPut("username", user.getUsername())
                .fluentPut("nickname", user.getNickname())
                .fluentPut("avatar", user.getAvatar())
                .fluentPut("openid", user.getOpenid());

        return new JSONObject().fluentPut("success", true).fluentPut("user", userJson);
    }

    @PostMapping("/user/avatar")
    public Object uploadAvatar(@RequestParam("avatar") MultipartFile avatar, 
                               @RequestParam(required = false) String openid,
                               HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            
            System.out.println("uploadAvatar called, openid: " + openid + ", session userId: " + userId);
            
            if (userId == null && openid != null && !openid.isEmpty()) {
                QueryWrapper<User> qw = new QueryWrapper<>();
                qw.eq("openid", openid);
                User user = userMapper.selectOne(qw);
                if (user != null) {
                    userId = user.getId();
                    System.out.println("found userId by openid: " + userId);
                }
            }
            
            if (userId == null) {
                System.out.println("userId is null, returning 401");
                return ResponseEntity.status(401).body(new JSONObject().fluentPut("message", "未登录"));
            }

            if (avatar == null || avatar.isEmpty()) {
                return ResponseEntity.status(400).body(new JSONObject().fluentPut("message", "请选择头像"));
            }

            java.io.File uploadDir = new java.io.File("/var/www/aidbox/uploads/avatars");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String originalFilename = avatar.getOriginalFilename();
            String extension = "png";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            }
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            java.io.File destFile = new java.io.File(uploadDir, fileName);
            avatar.transferTo(destFile);

            String avatarUrl = "https://aidbox.top/uploads/avatars/" + fileName;
            System.out.println("avatarUrl: " + avatarUrl);

            User user = userMapper.selectById(userId);
            if (user != null) {
                System.out.println("before update, user avatar: " + user.getAvatar());
                user.setAvatar(avatarUrl);
                int rows = userMapper.updateById(user);
                System.out.println("avatar updateById returned: " + rows + " rows affected");
            }

            return new JSONObject()
                    .fluentPut("success", true)
                    .fluentPut("avatar", avatarUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new JSONObject().fluentPut("message", "上传失败: " + e.getMessage()));
        }
    }

    @PostMapping("/asr/recognize")
    public Object recognize(@RequestParam("audio") MultipartFile audio) {
        try {
            String text = asrService.recognize(audio);
            return new JSONObject().fluentPut("text", text);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new JSONObject().fluentPut("message", e.getMessage()));
        }
    }

    @PostMapping("/images/generate")
    public Object generateImage(@RequestBody JSONObject req, javax.servlet.http.HttpServletRequest request, HttpSession session) {
        try {
            String text = req.getString("text");
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.status(400).body(new JSONObject().fluentPut("message", "text不能为空"));
            }
            Integer tokenUsage = req.getInteger("tokenUsage");
            Long userId = (Long) session.getAttribute("userId");

            // 如果session没有userId，尝试通过openid获取（支持微信用户）
            if (userId == null) {
                String openid = req.getString("openid");
                System.out.println("DEBUG - openid from request: " + openid);
                if (openid != null && !openid.isEmpty()) {
                    QueryWrapper<User> qw = new QueryWrapper<>();
                    qw.eq("openid", openid);
                    User user = userMapper.selectOne(qw);
                    System.out.println("DEBUG - user found: " + (user != null ? user.getId() : "null"));
                    if (user != null) {
                        userId = user.getId();
                    }
                }
            }

            // 获取 token 使用量
            int[] tokenUsageOut = new int[1];
            String base64Str = imageService.generate(text, tokenUsageOut);
            int actualTokenUsage = tokenUsageOut[0];

            String[] parts = base64Str.split(",");
            if (parts.length != 2) {
                throw new RuntimeException("非法的 base64 图片格式");
            }
            byte[] imageBytes = java.util.Base64.getDecoder().decode(parts[1]);

            java.io.File uploadDir = new java.io.File("/var/www/aidbox/uploads");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            String fileName = UUID.randomUUID().toString().replace("-", "") + ".png";
            java.io.File destFile = new java.io.File(uploadDir, fileName);
            java.nio.file.Files.write(destFile.toPath(), imageBytes);

            // 直接使用 https（因为微信小程序需要 https 图片）
            String scheme = "https";
            int port = request.getServerPort();
            String serverName = request.getServerName();
            String baseUrl = String.format("%s://%s", scheme, serverName);
            if (port != 80 && port != 443) {
                baseUrl += ":" + port;
            }
            String imageUrl = baseUrl + "/uploads/" + fileName;

            ImageRecord record = new ImageRecord();
            record.setText(text);
            record.setUrl(imageUrl);
            record.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            record.setCreatedAt(System.currentTimeMillis());
            record.setUserId(userId);
            record.setTokenUsage(tokenUsage != null ? tokenUsage : actualTokenUsage);
            record.setPrompt(text);

            imageMapper.insert(record);

            return record;
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new JSONObject().fluentPut("message", e.getMessage()));
        }
    }

    @GetMapping("/images")
    public Object getImages(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int pageSize,
                            @RequestParam(required = false) Long userId,
                            @RequestParam(required = false) String openid) throws Exception {

        // 如果传了openid，先查用户ID
        if (openid != null && !openid.isEmpty() && userId == null) {
            QueryWrapper<User> userQw = new QueryWrapper<>();
            userQw.eq("openid", openid);
            User user = userMapper.selectOne(userQw);
            if (user != null) {
                userId = user.getId();
            }
        }

        Page<ImageRecord> p = new Page<>(page, pageSize);
        QueryWrapper<ImageRecord> qw = new QueryWrapper<>();
        if (userId != null) {
            qw.eq("user_id", userId);
        }
        qw.orderByDesc("created_at");
        Page<ImageRecord> result = imageMapper.selectPage(p, qw);

        return new JSONObject()
                .fluentPut("list", result.getRecords())
                .fluentPut("page", page)
                .fluentPut("pageSize", pageSize)
                .fluentPut("total", result.getTotal())
                .fluentPut("hasMore", result.getCurrent() < result.getPages());
    }

    @GetMapping("/images/{id}")
    public Object getImage(@PathVariable String id) throws Exception {
        ImageRecord img = imageMapper.selectById(id);
        if (img != null) {
            return img;
        }
        return ResponseEntity.status(404).body(new JSONObject().fluentPut("message", "记录不存在"));
    }

    @DeleteMapping("/images/{id}")
    public Object deleteImage(@PathVariable String id) throws Exception {
        ImageRecord record = imageMapper.selectById(id);
        if (record != null && record.getUrl() != null) {
            try {
                String[] urlParts = record.getUrl().split("/uploads/");
                if (urlParts.length == 2) {
                    String fileName = urlParts[1];
                    java.io.File fileToDelete = new java.io.File("/var/www/aidbox/uploads", fileName);
                    if (fileToDelete.exists()) {
                        fileToDelete.delete();
                    }
                }
            } catch (Exception e) {
                System.err.println("删除物理图片失败: " + e.getMessage());
            }
        }

        int rows = imageMapper.deleteById(id);
        if (rows > 0) {
            return new JSONObject().fluentPut("success", true);
        }
        return ResponseEntity.status(404).body(new JSONObject().fluentPut("message", "记录不存在"));
    }

    @PostMapping("/admin/migrate-urls")
    public Object migrateImageUrls() {
        try {
            QueryWrapper<ImageRecord> qw = new QueryWrapper<>();
            qw.like("url", "http://aidbox.top");
            java.util.List<ImageRecord> records = imageMapper.selectList(qw);

            int count = 0;
            for (ImageRecord record : records) {
                String oldUrl = record.getUrl();
                String newUrl = oldUrl.replace("http://aidbox.top", "https://aidbox.top");
                record.setUrl(newUrl);
                imageMapper.updateById(record);
                count++;
            }

            return new JSONObject()
                    .fluentPut("success", true)
                    .fluentPut("migrated", count);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new JSONObject().fluentPut("message", e.getMessage()));
        }
    }

    // ==================== 用户管理接口 ====================

    @GetMapping("/users")
    public Object getUsers(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(new JSONObject().fluentPut("message", "未登录"));
        }

        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.orderByDesc("id");
        java.util.List<User> users = userMapper.selectList(qw);

        java.util.List<JSONObject> result = new java.util.ArrayList<>();
        for (User u : users) {
            // 统计该用户的图片数量和token使用量
            QueryWrapper<ImageRecord> imgQw = new QueryWrapper<>();
            imgQw.eq("user_id", u.getId());
            Long imageCount = imageMapper.selectCount(imgQw);

            imgQw.select("IFNULL(SUM(token_usage), 0) as totalToken");
            Map<String, Object> tokenResult = imageMapper.selectMaps(imgQw).stream().findFirst().orElse(null);
            Long totalToken = 0L;
            if (tokenResult != null && tokenResult.get("totalToken") != null) {
                totalToken = ((Number) tokenResult.get("totalToken")).longValue();
            }

            // 统计打印次数
            QueryWrapper<ImageRecord> printQw = new QueryWrapper<>();
            printQw.eq("user_id", u.getId());
            printQw.eq("printed", 1);
            Long printCount = imageMapper.selectCount(printQw);

            JSONObject obj = new JSONObject()
                    .fluentPut("id", u.getId())
                    .fluentPut("username", u.getUsername())
                    .fluentPut("nickname", u.getNickname())
                    .fluentPut("openid", u.getOpenid())
                    .fluentPut("createdAt", u.getCreatedAt())
                    .fluentPut("imageCount", imageCount)
                    .fluentPut("totalToken", totalToken)
                    .fluentPut("printCount", printCount);
            result.add(obj);
        }
        return new JSONObject().fluentPut("list", result);
    }

    @GetMapping("/users/{id}/images")
    public Object getUserImages(@PathVariable Long id, @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int pageSize, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(new JSONObject().fluentPut("message", "未登录"));
        }

        Page<ImageRecord> p = new Page<>(page, pageSize);
        QueryWrapper<ImageRecord> qw = new QueryWrapper<>();
        qw.eq("user_id", id);
        qw.orderByDesc("created_at");
        Page<ImageRecord> result = imageMapper.selectPage(p, qw);

        return new JSONObject()
                .fluentPut("list", result.getRecords())
                .fluentPut("page", page)
                .fluentPut("pageSize", pageSize)
                .fluentPut("total", result.getTotal())
                .fluentPut("hasMore", result.getCurrent() < result.getPages());
    }

    @PostMapping("/users")
    public Object createUser(@RequestBody JSONObject req, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(new JSONObject().fluentPut("message", "未登录"));
        }

        String username = req.getString("username");
        String password = req.getString("password");
        String nickname = req.getString("nickname");

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.status(400).body(new JSONObject().fluentPut("message", "用户名不能为空"));
        }
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.status(400).body(new JSONObject().fluentPut("message", "密码不能为空"));
        }

        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("username", username);
        if (userMapper.selectOne(qw) != null) {
            return ResponseEntity.status(400).body(new JSONObject().fluentPut("message", "用户名已存在"));
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(nickname != null ? nickname : username);
        user.setCreatedAt(System.currentTimeMillis());
        userMapper.insert(user);

        return new JSONObject()
                .fluentPut("id", user.getId())
                .fluentPut("username", user.getUsername())
                .fluentPut("nickname", user.getNickname())
                .fluentPut("createdAt", user.getCreatedAt());
    }

    @PutMapping("/users/{id}")
    public Object updateUser(@PathVariable Long id, @RequestBody JSONObject req, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(new JSONObject().fluentPut("message", "未登录"));
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            return ResponseEntity.status(404).body(new JSONObject().fluentPut("message", "用户不存在"));
        }

        String password = req.getString("password");
        String nickname = req.getString("nickname");

        if (password != null && !password.trim().isEmpty()) {
            user.setPassword(password);
        }
        if (nickname != null) {
            user.setNickname(nickname);
        }
        userMapper.updateById(user);

        return new JSONObject()
                .fluentPut("id", user.getId())
                .fluentPut("username", user.getUsername())
                .fluentPut("nickname", user.getNickname())
                .fluentPut("createdAt", user.getCreatedAt());
    }

    @DeleteMapping("/users/{id}")
    public Object deleteUser(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(new JSONObject().fluentPut("message", "未登录"));
        }

        if (id.equals(userId)) {
            return ResponseEntity.status(400).body(new JSONObject().fluentPut("message", "不能删除当前登录用户"));
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            return ResponseEntity.status(404).body(new JSONObject().fluentPut("message", "用户不存在"));
        }

        userMapper.deleteById(id);
        return new JSONObject().fluentPut("success", true);
    }

    // ==================== 打印记录接口 ====================

    @PostMapping("/prints")
    public Object recordPrint(@RequestBody JSONObject req, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        System.out.println("recordPrint called, session userId: " + userId);
        if (userId == null) {
            String openid = req.getString("openid");
            System.out.println("trying openid: " + openid);
            if (openid != null && !openid.isEmpty()) {
                QueryWrapper<User> qw = new QueryWrapper<>();
                qw.eq("openid", openid);
                User user = userMapper.selectOne(qw);
                if (user != null) {
                    userId = user.getId();
                    System.out.println("found userId by openid: " + userId);
                }
            }
            if (userId == null) {
                System.out.println("userId still null, returning 401");
                return ResponseEntity.status(401).body(new JSONObject().fluentPut("message", "未登录"));
            }
        }

        String imageId = req.getString("imageId");
        System.out.println("imageId: " + imageId);
        if (imageId != null && !imageId.isEmpty()) {
            ImageRecord image = imageMapper.selectById(imageId);
            System.out.println("image found: " + (image != null));
            if (image != null) {
                System.out.println("image.userId: " + image.getUserId() + ", current userId: " + userId);
                if (image.getUserId() != null && image.getUserId().equals(userId)) {
                    image.setPrinted(1);
                    int currentCount = image.getPrintCount() != null ? image.getPrintCount() : 0;
                    image.setPrintCount(currentCount + 1);
                    imageMapper.updateById(image);
                    System.out.println("printed updated to 1, printCount: " + (currentCount + 1));
                } else {
                    System.out.println("userId mismatch, skipping update");
                }
            }
        }

        return new JSONObject().fluentPut("success", true);
    }

    @GetMapping("/prints/count")
    public Object getPrintCount(@RequestParam(required = false) String openid, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        System.out.println("getPrintCount called, session userId: " + userId + ", openid: " + openid);
        
        if (userId == null && openid != null && !openid.isEmpty()) {
            QueryWrapper<User> qw = new QueryWrapper<>();
            qw.eq("openid", openid);
            User user = userMapper.selectOne(qw);
            if (user != null) {
                userId = user.getId();
                System.out.println("found userId by openid: " + userId);
            }
        }
        
        if (userId == null) {
            System.out.println("userId still null, returning 401");
            return ResponseEntity.status(401).body(new JSONObject().fluentPut("message", "未登录"));
        }

        QueryWrapper<ImageRecord> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        qw.select("IFNULL(SUM(print_count), 0) as totalPrintCount");
        java.util.Map<String, Object> result = imageMapper.selectMaps(qw).stream().findFirst().orElse(null);
        Long count = 0L;
        if (result != null && result.get("totalPrintCount") != null) {
            count = ((Number) result.get("totalPrintCount")).longValue();
        }
        System.out.println("print count for userId " + userId + ": " + count);

        return new JSONObject().fluentPut("count", count);
    }

    @PostMapping("/translate")
    public Object translate(@RequestBody JSONObject req) {
        try {
            String text = req.getString("text");
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.status(400).body(new JSONObject().fluentPut("message", "text不能为空"));
            }

            String english = translateService.translateToEnglish(text);
            return new JSONObject()
                    .fluentPut("success", true)
                    .fluentPut("original", text)
                    .fluentPut("translated", english);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new JSONObject().fluentPut("message", "翻译失败: " + e.getMessage()));
        }
    }
}
