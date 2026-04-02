-- 创建数据库
CREATE DATABASE IF NOT EXISTS `voice_print` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `voice_print`;

-- 创建用户表
CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码',
  `created_at` bigint(20) DEFAULT NULL COMMENT '创建时间戳',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入测试用户 (密码: admin123)
INSERT INTO `users` (`username`, `password`, `created_at`) VALUES ('admin', 'admin123', UNIX_TIMESTAMP());

-- 创建历史记录表
CREATE TABLE IF NOT EXISTS `images` (
  `id` varchar(64) NOT NULL COMMENT '主键UUID',
  `text` varchar(500) DEFAULT NULL COMMENT '识别的语音文本',
  `url` varchar(500) DEFAULT NULL COMMENT '生成的图片本地HTTP访问URL',
  `time` varchar(50) DEFAULT NULL COMMENT '生成时间格式化字符串',
  `created_at` bigint(20) DEFAULT NULL COMMENT '时间戳，用于排序',
  `user_id` bigint(20) DEFAULT NULL COMMENT '关联的用户ID',
  `token_usage` int(11) DEFAULT 0 COMMENT '消耗的Token数量',
  `prompt` text COMMENT '最终发送给大模型的完整提示词',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片生成历史记录表';

-- 创建用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `password` varchar(100) DEFAULT NULL COMMENT '密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `openid` varchar(100) DEFAULT NULL COMMENT '微信openid',
  `created_at` bigint(20) DEFAULT NULL COMMENT '注册时间戳',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
