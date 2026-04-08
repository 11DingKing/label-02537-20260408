package com.knowledgebase.aspect;

import com.knowledgebase.common.OperationLog;
import com.knowledgebase.entity.SysOperationLog;
import com.knowledgebase.mapper.SysOperationLogMapper;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperationLogMapper logMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint point, OperationLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        SysOperationLog logEntity = new SysOperationLog();
        logEntity.setModule(opLog.module());
        logEntity.setOperation(opLog.operation());
        logEntity.setMethod(point.getSignature().getDeclaringTypeName() + "." + point.getSignature().getName());

        // 过滤掉不可序列化的参数
        String params = Arrays.stream(point.getArgs())
                .filter(a -> a != null)
                .filter(a -> !(a instanceof MultipartFile))
                .filter(a -> !(a instanceof HttpServletRequest))
                .filter(a -> !(a instanceof HttpServletResponse))
                .map(a -> {
                    try { return JSONUtil.toJsonStr(a); }
                    catch (Exception e) { return String.valueOf(a); }
                })
                .collect(Collectors.joining(","));
        logEntity.setParams(params.length() > 2000 ? params.substring(0, 2000) : params);

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            logEntity.setIp(request.getRemoteAddr());
            Object userId = request.getAttribute("userId");
            Object username = request.getAttribute("username");
            if (userId != null) logEntity.setUserId((Long) userId);
            if (username != null) logEntity.setUsername((String) username);
        }

        try {
            Object result = point.proceed();
            logEntity.setStatus(1);
            return result;
        } catch (Throwable e) {
            logEntity.setStatus(0);
            logEntity.setErrorMsg(e.getMessage() != null && e.getMessage().length() > 500
                    ? e.getMessage().substring(0, 500) : e.getMessage());
            throw e;
        } finally {
            logEntity.setDuration(System.currentTimeMillis() - start);
            try {
                logMapper.insert(logEntity);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }
    }
}
