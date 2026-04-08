# 知识库系统 - 项目设计文档

## 1. 系统架构

```mermaid
flowchart TD
    subgraph Frontend["前端 - 管理后台 (Vue3 + Element Plus)"]
        A[登录页] --> B[仪表盘]
        B --> C[知识库管理]
        B --> D[文档管理]
        B --> E[知识检索]
        B --> F[用户管理]
        B --> G[操作日志]
    end

    subgraph Backend["后端 - Spring Boot 3"]
        H[AuthController] --> L[JWT Filter]
        I[DocumentController] --> M[DocumentService]
        J[KnowledgeController] --> N[KnowledgeService]
        K[UserController] --> O[UserService]
        P[LogController] --> Q[LogService]
        M --> R[DocumentParser]
        R --> S[DocxParser]
        R --> T[ExcelParser]
        M --> U[KnowledgeExtractor]
    end

    subgraph Storage["存储层"]
        V[(MySQL 8.0)]
        W[(文件存储 /uploads)]
    end

    Frontend -->|Axios HTTP| Backend
    Backend --> V
    Backend --> W
```

## 2. ER 图

```mermaid
erDiagram
    sys_user {
        bigint id PK
        varchar username UK
        varchar password
        varchar nickname
        varchar avatar
        tinyint status
        datetime created_at
        datetime updated_at
    }

    kb_knowledge_base {
        bigint id PK
        varchar name
        varchar description
        bigint user_id FK
        int doc_count
        tinyint status
        datetime created_at
        datetime updated_at
    }

    kb_document {
        bigint id PK
        bigint kb_id FK
        varchar filename
        varchar original_name
        varchar file_path
        varchar file_type
        bigint file_size
        tinyint parse_status
        varchar parse_message
        bigint user_id FK
        datetime created_at
        datetime updated_at
    }

    kb_knowledge_chunk {
        bigint id PK
        bigint doc_id FK
        bigint kb_id FK
        text content
        int chunk_index
        varchar source_info
        datetime created_at
    }

    sys_operation_log {
        bigint id PK
        bigint user_id FK
        varchar username
        varchar module
        varchar operation
        varchar method
        varchar params
        varchar ip
        tinyint status
        varchar error_msg
        datetime created_at
    }

    sys_user ||--o{ kb_knowledge_base : "创建"
    sys_user ||--o{ kb_document : "上传"
    kb_knowledge_base ||--o{ kb_document : "包含"
    kb_document ||--o{ kb_knowledge_chunk : "解析为"
    sys_user ||--o{ sys_operation_log : "产生"
```

## 3. 接口清单

### AuthController `/api/auth`
| Method | Path | Description |
|--------|------|-------------|
| POST | /login | 用户登录，返回JWT |
| POST | /logout | 用户登出 |
| GET | /info | 获取当前用户信息 |

### UserController `/api/users`
| Method | Path | Description |
|--------|------|-------------|
| GET | / | 分页查询用户列表 |
| POST | / | 新增用户 |
| PUT | /{id} | 修改用户 |
| DELETE | /{id} | 删除用户 |
| PUT | /{id}/status | 启用/禁用用户 |

### KnowledgeBaseController `/api/kb`
| Method | Path | Description |
|--------|------|-------------|
| GET | / | 分页查询知识库列表 |
| POST | / | 创建知识库 |
| PUT | /{id} | 修改知识库 |
| DELETE | /{id} | 删除知识库 |
| GET | /{id} | 知识库详情 |

### DocumentController `/api/documents`
| Method | Path | Description |
|--------|------|-------------|
| GET | / | 分页查询文档列表 |
| POST | /upload | 上传文档(doc/excel) |
| DELETE | /{id} | 删除文档 |
| POST | /{id}/reparse | 重新解析文档 |
| GET | /{id}/chunks | 查看文档知识块 |

### SearchController `/api/search`
| Method | Path | Description |
|--------|------|-------------|
| GET | / | 全文检索知识内容 |

### LogController `/api/logs`
| Method | Path | Description |
|--------|------|-------------|
| GET | / | 分页查询操作日志 |

## 4. UI/UX 规范

| 属性 | 值 |
|------|-----|
| 主色调 | #4A90D9 (知性蓝) |
| 辅助色 | #67C23A (成功绿), #E6A23C (警告橙), #F56C6C (危险红) |
| 背景色 | #F0F2F5 (页面), #FFFFFF (卡片) |
| 字体 | -apple-system, "PingFang SC", "Helvetica Neue", sans-serif |
| 标题字号 | 20px / 16px / 14px |
| 正文字号 | 14px, 行高 1.6 |
| 卡片圆角 | 8px |
| 卡片阴影 | 0 2px 12px rgba(0,0,0,0.08) |
| 间距体系 | 8px / 16px / 24px / 32px |
| 按钮圆角 | 6px |
