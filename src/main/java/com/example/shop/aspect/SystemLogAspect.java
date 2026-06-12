package com.example.shop.aspect;

import com.example.shop.annotation.SystemLog;
import com.example.shop.entity.SysLog;
import com.example.shop.mapper.SysLogMapper;
import com.example.shop.utils.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 系统日志切面
 */
@Slf4j
@Aspect
@Component
public class SystemLogAspect {

    @Autowired
    private SysLogMapper sysLogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 定义切点
     */
    @Pointcut("@annotation(com.example.shop.annotation.SystemLog)")
    public void systemLogPointcut() {}

    /**
     * 环绕通知
     */
    @Around("systemLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        SystemLog systemLog = method.getAnnotation(SystemLog.class);
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        
        // 创建日志对象
        SysLog logEntity = new SysLog();
        logEntity.setOperation(systemLog.operation());
        logEntity.setModule(systemLog.module());
        logEntity.setAction(systemLog.action());
        logEntity.setMethod(method.getDeclaringClass().getName() + "." + method.getName());
        
        // 设置用户信息
        String username = SecurityUtil.getCurrentUsername();
        Long userId = SecurityUtil.getCurrentUserId();
        logEntity.setUsername(username);
        logEntity.setUserId(userId);
        
        // 设置IP地址
        if (request != null) {
            logEntity.setIp(getClientIpAddress(request));
        }
        
        // 记录参数
        if (systemLog.recordParams()) {
            try {
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    // 过滤敏感参数
                    Object[] filteredArgs = filterSensitiveParams(args);
                    logEntity.setParams(objectMapper.writeValueAsString(filteredArgs));
                }
            } catch (Exception e) {
                log.warn("记录参数失败: {}", e.getMessage());
                logEntity.setParams("参数记录失败");
            }
        }
        
        Object result = null;
        try {
            // 执行目标方法
            result = joinPoint.proceed();
            
            // 记录成功状态
            logEntity.setStatus(1);
            
        } catch (Exception e) {
            // 记录失败状态和错误信息
            logEntity.setStatus(0);
            logEntity.setErrorMsg(e.getMessage());
            
            throw e;
        } finally {
            // 计算执行时间
            long endTime = System.currentTimeMillis();
            logEntity.setExecuteTime(endTime - startTime);
            
            // 异步保存日志
            saveLogAsync(logEntity);
        }
        
        return result;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 过滤敏感参数
     */
    private Object[] filterSensitiveParams(Object[] args) {
        Object[] filteredArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg != null) {
                String argStr = arg.toString();
                // 过滤密码等敏感信息
                if (argStr.contains("password") || argStr.contains("Password")) {
                    filteredArgs[i] = "***";
                } else {
                    filteredArgs[i] = arg;
                }
            } else {
                filteredArgs[i] = null;
            }
        }
        return filteredArgs;
    }

    /**
     * 异步保存日志
     */
    private void saveLogAsync(SysLog logEntity) {
        try {
            sysLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("保存系统日志失败: {}", e.getMessage(), e);
        }
    }
}
