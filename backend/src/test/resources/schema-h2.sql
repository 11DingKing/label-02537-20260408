-- H2 compatible schema for testing (drops and recreates each run)
DROP TABLE IF EXISTS sys_operation_log;
DROP TABLE IF EXISTS kb_knowledge_chunk;
DROP TABLE IF EXISTS kb_document;
DROP TABLE IF EXISTS kb_knowledge_base;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) DEFAULT '',
    avatar VARCHAR(255) DEFAULT '',
    status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE kb_knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT '',
    user_id BIGINT NOT NULL,
    doc_count INT DEFAULT 0,
    chunk_count INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE kb_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT DEFAULT 0,
    parse_status TINYINT DEFAULT 0,
    parse_message VARCHAR(500) DEFAULT '',
    chunk_count INT DEFAULT 0,
    user_id BIGINT NOT NULL,
    deleted TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE kb_knowledge_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    kb_id BIGINT NOT NULL,
    content CLOB NOT NULL,
    chunk_index INT DEFAULT 0,
    source_info VARCHAR(255) DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT 0,
    username VARCHAR(50) DEFAULT '',
    module VARCHAR(50) DEFAULT '',
    operation VARCHAR(100) DEFAULT '',
    method VARCHAR(200) DEFAULT '',
    params CLOB,
    ip VARCHAR(50) DEFAULT '',
    status TINYINT DEFAULT 1,
    error_msg VARCHAR(500) DEFAULT '',
    duration BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
