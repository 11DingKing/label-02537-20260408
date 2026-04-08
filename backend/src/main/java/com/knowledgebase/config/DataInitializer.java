package com.knowledgebase.config;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledgebase.entity.*;
import com.knowledgebase.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final KbKnowledgeBaseMapper kbMapper;
    private final KbDocumentMapper docMapper;
    private final KbKnowledgeChunkMapper chunkMapper;
    private final SysOperationLogMapper logMapper;

    @Override
    public void run(String... args) {
        try {
            initUsers();
            initKnowledgeBases();
            initDocumentsAndChunks();
            initOperationLogs();
        } catch (Exception e) {
            log.warn("初始化数据失败（可能表尚未创建）: {}", e.getMessage());
        }
    }

    private void initUsers() {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin"));
        if (count > 0) {
            log.info("数据已存在，跳过初始化");
            return;
        }

        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(BCrypt.hashpw("admin123"));
        admin.setNickname("系统管理员");
        admin.setStatus(1);
        userMapper.insert(admin);

        SysUser zhangsan = new SysUser();
        zhangsan.setUsername("zhangsan");
        zhangsan.setPassword(BCrypt.hashpw("123456"));
        zhangsan.setNickname("张三");
        zhangsan.setStatus(1);
        userMapper.insert(zhangsan);

        SysUser lisi = new SysUser();
        lisi.setUsername("lisi");
        lisi.setPassword(BCrypt.hashpw("123456"));
        lisi.setNickname("李四");
        lisi.setStatus(1);
        userMapper.insert(lisi);

        SysUser wangwu = new SysUser();
        wangwu.setUsername("wangwu");
        wangwu.setPassword(BCrypt.hashpw("123456"));
        wangwu.setNickname("王五");
        wangwu.setStatus(0);
        userMapper.insert(wangwu);

        log.info("初始化用户数据完成: admin/admin123, zhangsan/123456, lisi/123456, wangwu/123456(已禁用)");
    }

    private void initKnowledgeBases() {
        Long count = kbMapper.selectCount(null);
        if (count > 0) return;

        String[][] kbs = {
            {"公司规章制度", "包含员工手册、考勤制度、薪酬福利等公司内部管理规范文档"},
            {"产品技术文档", "产品需求文档、技术架构设计、接口规范、部署运维手册等技术资料"},
            {"市场营销资料", "品牌宣传材料、市场调研报告、竞品分析、营销策划方案等"},
            {"财务管理制度", "财务报销流程、预算管理办法、合同审批制度、税务合规指南"},
            {"培训学习资料", "新员工入职培训、技术分享文档、行业知识学习资料汇总"}
        };

        for (int i = 0; i < kbs.length; i++) {
            KbKnowledgeBase kb = new KbKnowledgeBase();
            kb.setName(kbs[i][0]);
            kb.setDescription(kbs[i][1]);
            kb.setUserId(1L);
            kb.setDocCount(0);
            kb.setChunkCount(0);
            kb.setStatus(1);
            kbMapper.insert(kb);
        }
        log.info("初始化知识库数据完成: {}个知识库", kbs.length);
    }

    private void initDocumentsAndChunks() {
        Long count = docMapper.selectCount(null);
        if (count > 0) return;

        // 知识库1: 公司规章制度
        long docId1 = insertDoc(1L, "员工手册2024版.docx", "docx", 256000L, 1L);
        insertChunks(docId1, 1L, new String[][]{
            {"第一章 总则", "本手册适用于公司全体正式员工及试用期员工。公司倡导诚信、创新、协作、共赢的核心价值观，致力于为员工提供公平、开放的工作环境。每位员工入职时应认真阅读本手册，了解公司各项规章制度。"},
            {"第二章 考勤管理", "工作时间为周一至周五，上午九点至下午六点，午休时间为十二点至一点半。员工应通过公司考勤系统进行打卡，迟到早退每月累计超过三次将影响当月绩效考核。请假需提前在系统中提交申请，经直属上级审批后生效。"},
            {"第三章 薪酬福利", "公司实行月薪制，每月十五日发放上月工资。薪酬结构包括基本工资、岗位津贴、绩效奖金三部分。公司为员工缴纳五险一金，提供年度体检、带薪年假、节日福利等。工作满一年的员工享有五天带薪年假，此后每增加一年工龄增加一天。"},
            {"第四章 行为规范", "员工应遵守职业道德，保守公司商业秘密。工作期间应着装整洁得体，保持办公区域干净整齐。禁止在办公区域大声喧哗、吸烟或从事与工作无关的活动。使用公司设备和资源应爱护节约，不得用于私人用途。"}
        });

        long docId2 = insertDoc(1L, "差旅报销管理办法.docx", "docx", 128000L, 1L);
        insertChunks(docId2, 1L, new String[][]{
            {"报销标准", "国内出差住宿标准：一线城市每晚不超过五百元，二线城市每晚不超过三百五十元，其他城市每晚不超过两百五十元。交通费用原则上选择经济舱或高铁二等座，特殊情况需提前审批。餐饮补贴标准为每天一百元，按实际出差天数计算。"},
            {"报销流程", "出差结束后五个工作日内提交报销申请。报销单需附上所有原始票据，包括交通票据、住宿发票、餐饮发票等。报销单经部门经理审核后提交财务部，财务部在收到完整材料后十个工作日内完成打款。"}
        });

        // 知识库2: 产品技术文档
        long docId3 = insertDoc(2L, "系统架构设计文档.docx", "docx", 512000L, 2L);
        insertChunks(docId3, 2L, new String[][]{
            {"系统概述", "本系统采用前后端分离的微服务架构，后端基于Spring Cloud构建，前端使用Vue3框架。系统包含用户服务、订单服务、商品服务、支付服务四个核心微服务，通过网关统一对外提供接口。数据库采用MySQL主从架构，缓存层使用Redis集群。"},
            {"技术选型", "后端技术栈：Java 17、Spring Boot 3.2、Spring Cloud 2023、MyBatis-Plus、Redis 7.0、RabbitMQ。前端技术栈：Vue 3、TypeScript、Element Plus、Pinia。基础设施：Docker容器化部署、Kubernetes编排、Nginx负载均衡、Jenkins持续集成。"},
            {"接口规范", "所有接口遵循RESTful设计规范，使用JSON格式传输数据。接口响应统一格式为：code表示状态码，message表示提示信息，data表示业务数据。认证采用JWT令牌机制，令牌有效期为二十四小时。接口限流策略为每个用户每秒最多十次请求。"}
        });

        long docId4 = insertDoc(2L, "数据库设计说明书.xlsx", "xlsx", 89000L, 2L);
        insertChunks(docId4, 2L, new String[][]{
            {"用户表设计", "用户表包含用户编号、用户名、密码哈希、昵称、手机号、邮箱、头像地址、账户状态、创建时间、更新时间等字段。用户名和手机号设置唯一索引，创建时间设置普通索引用于排序查询。密码使用BCrypt算法加密存储。"},
            {"订单表设计", "订单表包含订单编号、用户编号、商品总金额、实付金额、优惠金额、订单状态、收货地址、支付方式、支付时间、发货时间、完成时间等字段。订单编号采用雪花算法生成，订单状态包括待支付、已支付、已发货、已完成、已取消五种。"}
        });

        // 知识库3: 市场营销资料
        long docId5 = insertDoc(3L, "2024年度市场调研报告.docx", "docx", 384000L, 2L);
        insertChunks(docId5, 3L, new String[][]{
            {"行业概况", "根据中国互联网络信息中心发布的报告，截至2024年六月，我国网民规模达到十点九亿，互联网普及率达百分之七十七点五。企业数字化转型加速推进，云计算、大数据、人工智能等技术在各行业深入应用，企业级软件市场规模持续增长。"},
            {"竞品分析", "主要竞争对手包括三家企业。甲公司市场占有率约百分之三十五，产品功能全面但价格偏高。乙公司专注中小企业市场，价格优势明显但技术支持较弱。丙公司为新兴品牌，产品创新性强但市场认知度有待提升。我司应在产品差异化和服务质量上建立竞争优势。"},
            {"用户画像", "目标用户群体以二十五至四十五岁的企业管理者和技术人员为主，其中男性占比约百分之六十五。用户主要分布在一线和新一线城市，所在企业规模以五十至五百人的中型企业为主。用户最关注的需求依次为：系统稳定性、数据安全性、操作便捷性、售后服务质量。"}
        });

        // 知识库4: 财务管理制度
        long docId6 = insertDoc(4L, "财务报销制度汇编.xlsx", "xlsx", 156000L, 1L);
        insertChunks(docId6, 4L, new String[][]{
            {"日常报销", "日常办公用品采购单笔金额在五百元以下的，由部门经理审批即可。五百元至两千元的需部门总监审批。两千元以上的需提交副总经理审批。所有报销必须提供正规发票，白条和收据不予报销。报销单填写应字迹清晰，金额大小写一致。"},
            {"合同付款", "合同付款需提供合同原件、对方开具的发票、验收报告等材料。付款金额在五万元以下的由财务总监审批，五万元至二十万元的需总经理审批，二十万元以上的需董事会审批。付款方式优先采用银行转账，原则上不使用现金支付。"}
        });

        // 知识库5: 培训学习资料
        long docId7 = insertDoc(5L, "新员工入职培训手册.docx", "docx", 320000L, 1L);
        insertChunks(docId7, 5L, new String[][]{
            {"公司介绍", "公司成立于2015年，总部位于北京市海淀区中关村科技园。公司专注于企业级知识管理解决方案，目前拥有员工三百余人，在上海、深圳、成都设有分公司。公司先后获得国家高新技术企业认定、软件企业认定，拥有自主知识产权专利二十余项。"},
            {"开发流程", "公司采用敏捷开发模式，以两周为一个迭代周期。每个迭代包含需求评审、技术方案设计、编码开发、代码评审、测试验证、上线部署六个阶段。开发人员需遵循代码规范，提交代码前必须通过单元测试和代码审查。每日站会时间为上午十点，时长不超过十五分钟。"},
            {"常用工具", "项目管理使用禅道系统，代码托管使用GitLab，持续集成使用Jenkins，文档协作使用飞书文档。开发环境统一使用IntelliJ IDEA，数据库管理工具推荐使用Navicat或DataGrip。内部沟通使用飞书即时通讯，重要事项需通过邮件确认留痕。"}
        });

        // 更新知识库的文档数和知识块数
        updateKbCounts();
        log.info("初始化文档和知识块数据完成");
    }

    private long insertDoc(Long kbId, String originalName, String fileType, Long fileSize, Long userId) {
        KbDocument doc = new KbDocument();
        doc.setKbId(kbId);
        doc.setFilename("init_" + System.nanoTime() + "." + fileType);
        doc.setOriginalName(originalName);
        doc.setFilePath("/data/uploads/init/" + doc.getFilename());
        doc.setFileType(fileType);
        doc.setFileSize(fileSize);
        doc.setParseStatus(2);
        doc.setParseMessage("解析完成");
        doc.setChunkCount(0);
        doc.setUserId(userId);
        docMapper.insert(doc);
        return doc.getId();
    }

    private void insertChunks(long docId, long kbId, String[][] chunks) {
        for (int i = 0; i < chunks.length; i++) {
            KbKnowledgeChunk chunk = new KbKnowledgeChunk();
            chunk.setDocId(docId);
            chunk.setKbId(kbId);
            chunk.setContent(chunks[i][1]);
            chunk.setChunkIndex(i + 1);
            chunk.setSourceInfo(chunks[i][0]);
            chunkMapper.insert(chunk);
        }
        // 更新文档的知识块数
        KbDocument doc = docMapper.selectById(docId);
        if (doc != null) {
            doc.setChunkCount(chunks.length);
            docMapper.updateById(doc);
        }
    }

    private void updateKbCounts() {
        var kbList = kbMapper.selectList(null);
        for (KbKnowledgeBase kb : kbList) {
            Long docCount = docMapper.selectCount(
                new LambdaQueryWrapper<KbDocument>().eq(KbDocument::getKbId, kb.getId()));
            Long chunkCount = chunkMapper.selectCount(
                new LambdaQueryWrapper<KbKnowledgeChunk>().eq(KbKnowledgeChunk::getKbId, kb.getId()));
            kb.setDocCount(docCount.intValue());
            kb.setChunkCount(chunkCount.intValue());
            kbMapper.updateById(kb);
        }
    }

    private void initOperationLogs() {
        Long count = logMapper.selectCount(null);
        if (count > 0) return;

        LocalDateTime now = LocalDateTime.now();
        String[][] logs = {
            {"1", "admin", "认证", "用户登录", "POST /api/auth/login", "192.168.1.100", "1", "", "45"},
            {"1", "admin", "知识库", "创建知识库：公司规章制度", "POST /api/kb", "192.168.1.100", "1", "", "120"},
            {"1", "admin", "知识库", "创建知识库：产品技术文档", "POST /api/kb", "192.168.1.100", "1", "", "85"},
            {"1", "admin", "文档", "上传文档：员工手册2024版.docx", "POST /api/documents/upload", "192.168.1.100", "1", "", "1350"},
            {"1", "admin", "文档", "上传文档：差旅报销管理办法.docx", "POST /api/documents/upload", "192.168.1.100", "1", "", "890"},
            {"2", "zhangsan", "认证", "用户登录", "POST /api/auth/login", "192.168.1.101", "1", "", "38"},
            {"2", "zhangsan", "文档", "上传文档：系统架构设计文档.docx", "POST /api/documents/upload", "192.168.1.101", "1", "", "1580"},
            {"2", "zhangsan", "文档", "上传文档：数据库设计说明书.xlsx", "POST /api/documents/upload", "192.168.1.101", "1", "", "720"},
            {"1", "admin", "用户", "创建用户：张三", "POST /api/users", "192.168.1.100", "1", "", "95"},
            {"1", "admin", "用户", "创建用户：李四", "POST /api/users", "192.168.1.100", "1", "", "88"},
            {"3", "lisi", "认证", "用户登录", "POST /api/auth/login", "192.168.1.102", "1", "", "42"},
            {"3", "lisi", "知识库", "创建知识库：市场营销资料", "POST /api/kb", "192.168.1.102", "1", "", "76"},
            {"3", "lisi", "文档", "上传文档：2024年度市场调研报告.docx", "POST /api/documents/upload", "192.168.1.102", "1", "", "1120"},
            {"1", "admin", "知识库", "创建知识库：财务管理制度", "POST /api/kb", "192.168.1.100", "1", "", "68"},
            {"1", "admin", "文档", "上传文档：财务报销制度汇编.xlsx", "POST /api/documents/upload", "192.168.1.100", "1", "", "650"},
            {"1", "admin", "用户", "禁用用户：王五", "PUT /api/users/4/status", "192.168.1.100", "1", "", "55"},
            {"4", "wangwu", "认证", "用户登录", "POST /api/auth/login", "192.168.1.103", "0", "账户已被禁用", "30"},
            {"1", "admin", "知识库", "创建知识库：培训学习资料", "POST /api/kb", "192.168.1.100", "1", "", "72"},
            {"1", "admin", "文档", "上传文档：新员工入职培训手册.docx", "POST /api/documents/upload", "192.168.1.100", "1", "", "980"},
            {"2", "zhangsan", "文档", "查看知识块：系统架构设计文档.docx", "GET /api/documents/3/chunks", "192.168.1.101", "1", "", "35"},
        };

        for (int i = 0; i < logs.length; i++) {
            SysOperationLog opLog = new SysOperationLog();
            opLog.setUserId(Long.parseLong(logs[i][0]));
            opLog.setUsername(logs[i][1]);
            opLog.setModule(logs[i][2]);
            opLog.setOperation(logs[i][3]);
            opLog.setMethod(logs[i][4]);
            opLog.setParams("");
            opLog.setIp(logs[i][5]);
            opLog.setStatus(Integer.parseInt(logs[i][6]));
            opLog.setErrorMsg(logs[i][7]);
            opLog.setDuration(Long.parseLong(logs[i][8]));
            logMapper.insert(opLog);
        }
        log.info("初始化操作日志数据完成: {}条记录", logs.length);
    }
}
