CREATE DATABASE IF NOT EXISTS knowledge_base DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE knowledge_base;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    nickname VARCHAR(50) DEFAULT '' COMMENT '昵称',
    avatar VARCHAR(255) DEFAULT '' COMMENT '头像',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB COMMENT='系统用户表';

-- 知识库表
CREATE TABLE IF NOT EXISTS kb_knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '知识库名称',
    description VARCHAR(500) DEFAULT '' COMMENT '描述',
    user_id BIGINT NOT NULL COMMENT '创建人ID',
    doc_count INT DEFAULT 0 COMMENT '文档数量',
    chunk_count INT DEFAULT 0 COMMENT '知识块数量',
    status TINYINT DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='知识库表';

-- 文档表
CREATE TABLE IF NOT EXISTS kb_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_id BIGINT NOT NULL COMMENT '知识库ID',
    filename VARCHAR(255) NOT NULL COMMENT '存储文件名',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型: docx/xlsx/xls/doc',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小(bytes)',
    parse_status TINYINT DEFAULT 0 COMMENT '解析状态: 0待解析 1解析中 2成功 3失败',
    parse_message VARCHAR(500) DEFAULT '' COMMENT '解析信息',
    chunk_count INT DEFAULT 0 COMMENT '知识块数量',
    user_id BIGINT NOT NULL COMMENT '上传人ID',
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_kb_id (kb_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='文档表';

-- 知识块表
CREATE TABLE IF NOT EXISTS kb_knowledge_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id BIGINT NOT NULL COMMENT '文档ID',
    kb_id BIGINT NOT NULL COMMENT '知识库ID',
    content TEXT NOT NULL COMMENT '知识内容',
    chunk_index INT DEFAULT 0 COMMENT '块序号',
    source_info VARCHAR(255) DEFAULT '' COMMENT '来源信息(如Sheet名/段落)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_doc_id (doc_id),
    INDEX idx_kb_id (kb_id),
    FULLTEXT INDEX ft_content (content) WITH PARSER ngram
) ENGINE=InnoDB COMMENT='知识块表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT 0 COMMENT '操作人ID',
    username VARCHAR(50) DEFAULT '' COMMENT '操作人',
    module VARCHAR(50) DEFAULT '' COMMENT '模块',
    operation VARCHAR(100) DEFAULT '' COMMENT '操作描述',
    method VARCHAR(200) DEFAULT '' COMMENT '请求方法',
    params TEXT COMMENT '请求参数',
    ip VARCHAR(50) DEFAULT '' COMMENT 'IP地址',
    status TINYINT DEFAULT 1 COMMENT '状态: 1成功 0失败',
    error_msg VARCHAR(500) DEFAULT '' COMMENT '错误信息',
    duration BIGINT DEFAULT 0 COMMENT '耗时(ms)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='操作日志表';

-- 初始管理员由 DataInitializer 在应用启动时自动创建
-- 默认账号: admin / admin123
