# 知识库管理系统 (Knowledge Base System)

基于 Doc/Excel 文件的 Java 知识库系统，支持文档上传、自动解析、知识块提取和全文检索。

## How to Run

```bash
# 一键启动（需要 Docker 20.10+ 和 Docker Compose 2.0+）
docker-compose up --build -d
```

首次启动需要拉取镜像和编译，大约需要 3-5 分钟。

**访问地址：**
- 前端管理后台：http://localhost:8081
- 后端 API 接口：http://localhost:8080/api
- API 文档 (Swagger)：http://localhost:8080/api/swagger-ui.html

**常用命令：**
```bash
docker-compose ps           # 查看容器状态
docker-compose logs -f      # 查看日志
docker-compose down         # 停止服务
docker-compose down -v      # 停止并清除数据
```

## Services

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| 前端 | kb-frontend-admin | 8081 → 80 | Vue3 + Element Plus + Nginx |
| 后端 | kb-backend | 8080 | Spring Boot 3 + MyBatis-Plus |
| 数据库 | kb-mysql | 3306 | MySQL 8.0 (utf8mb4) |

## 测试账号

| 用户名 | 密码 | 昵称 | 状态 |
|--------|------|------|------|
| admin | admin123 | 系统管理员 | 启用 |
| zhangsan | 123456 | 张三 | 启用 |
| lisi | 123456 | 李四 | 启用 |
| wangwu | 123456 | 王五 | 已禁用 |

## 题目内容

针对 doc 和 excel 文件搭建知识库，实现文档上传、自动解析、知识块提取和全文检索功能。

---

## 技术栈

| 层级 | 技术选型 |
|------|----------|
| 后端 | Java 17 + Spring Boot 3.2 + MyBatis-Plus 3.5 + MySQL 8.0 + Apache POI 5.2 |
| 前端 | Vue 3.4 + Vite 5 + Element Plus 2.7 + Pinia 2.1 + Axios |
| 搜索 | MySQL FULLTEXT 全文检索 (ngram 分词) |
| 部署 | Docker + Docker Compose + Nginx |

## 支持的文件格式

| 格式 | 说明 | 解析方式 |
|------|------|----------|
| .doc | Word 97-2003 | Apache POI HWPFDocument |
| .docx | Word 2007+ | Apache POI XWPFDocument |
| .xls | Excel 97-2003 | Apache POI HSSFWorkbook |
| .xlsx | Excel 2007+ | Apache POI XSSFWorkbook |

## 初始化示例数据

系统启动后会自动创建以下示例数据：

- 5 个知识库：公司规章制度、产品技术文档、市场营销资料、财务管理制度、培训学习资料
- 7 份文档：包含 doc、docx、xlsx、xls 格式，均已解析完成
- 20 个知识块：真实的中文业务内容，可直接用于搜索测试
- 20 条操作日志：涵盖登录、创建、上传、禁用等多种操作类型

## 本地开发环境

### 后端

- Java 17 + Spring Boot 3.2 + MyBatis-Plus 3.5
- 需要 JDK 17+、Maven 3.8+、MySQL 8.0+

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE knowledge_base CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 执行初始化脚本
mysql -u root -p knowledge_base < backend/src/main/resources/schema.sql

# 启动服务
cd backend && mvn spring-boot:run
```

| 环境变量 | 说明 | 默认值 |
|----------|------|--------|
| DB_HOST | 数据库地址 | localhost |
| DB_PORT | 数据库端口 | 3306 |
| DB_NAME | 数据库名 | knowledge_base |
| DB_USER | 数据库用户 | root |
| DB_PASS | 数据库密码 | root123 |
| JWT_SECRET | JWT 密钥 | (内置默认值) |
| UPLOAD_PATH | 文件上传路径 | /data/uploads |

### 前端

- Vue 3.4 + Vite 5 + Element Plus 2.7 + Pinia 2.1
- 需要 Node.js 18.x+、npm 9.x+

```bash
cd frontend-admin
npm install
npm run dev       # 开发环境 http://localhost:8081
npm run build     # 构建生产版本
npm run test      # 运行单元测试
```

## 功能测试指南

### 1. 登录功能
- 使用 `admin` / `admin123` 登录，验证跳转到仪表盘
- 使用 `wangwu` / `123456` 登录，验证提示"账户已被禁用"

### 2. 仪表盘
- 验证统计卡片显示：知识库数量(5)、文档总数(7)、知识块数量(20)、今日操作数

### 3. 知识库管理
- 验证显示 5 个知识库
- 测试新建、编辑、删除知识库功能

### 4. 文档管理
- 验证显示 7 份文档，文件类型图标正确（W=doc/docx蓝色，X=xlsx/xls绿色）
- 测试上传 .doc、.docx、.xlsx、.xls 文件
- 点击"知识块"查看解析内容

### 5. 知识检索
- 输入关键词（如"员工"、"报销"、"架构"）搜索
- 验证结果高亮显示关键词

### 6. 用户管理
- 验证显示 4 个用户
- 验证超级管理员(admin)不可删除、不可禁用

### 7. 操作日志
- 验证日志包含：操作人、模块、操作描述、IP、状态、耗时

---

label-02537
