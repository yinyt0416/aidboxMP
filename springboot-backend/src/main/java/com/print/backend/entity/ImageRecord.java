package com.print.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("images")
public class ImageRecord {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String text;
    private String url;
    private String time;
    private Long createdAt;
    private Long userId;
    private Integer tokenUsage;
    private String prompt;
    private Integer printed;  // 0: 未打印, 1: 已打印
    private Integer printCount;  // 打印次数
}
