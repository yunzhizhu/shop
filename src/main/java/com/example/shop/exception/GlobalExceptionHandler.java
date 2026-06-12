package com.example.shop.exception;

import com.example.shop.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.error("业务异常: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常处理
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidationException(Exception e, HttpServletRequest request) {
        log.error("参数校验异常: {} - {}", request.getRequestURI(), e.getMessage());

        String message = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
            if (!fieldErrors.isEmpty()) {
                message = fieldErrors.get(0).getDefaultMessage();
            }
        } else if (e instanceof BindException) {
            BindException ex = (BindException) e;
            List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
            if (!fieldErrors.isEmpty()) {
                message = fieldErrors.get(0).getDefaultMessage();
            }
        }

        return Result.badRequest(message);
    }

    /**
     * 缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.error("缺少请求参数: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.badRequest("缺少必要的请求参数: " + e.getParameterName());
    }

    /**
     * 参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.error("参数类型不匹配: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.badRequest("参数类型不匹配: " + e.getName());
    }

    /**
     * HTTP消息不可读异常（JSON格式错误等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.error("HTTP消息不可读: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.badRequest("请求体格式错误");
    }

    /**
     * HTTP请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.error("HTTP请求方法不支持: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.error(405, "请求方法不支持: " + e.getMethod());
    }

    /**
     * 404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.error("404异常: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.notFound("请求的资源不存在");
    }

    /**
     * 文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.error("文件上传大小超限: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.badRequest("上传文件大小超出限制");
    }

    /**
     * 数据库约束违反异常
     */
    @ExceptionHandler({DataIntegrityViolationException.class, SQLIntegrityConstraintViolationException.class})
    public Result<Void> handleDataIntegrityViolationException(Exception e, HttpServletRequest request) {
        log.error("数据库约束违反: {} - {}", request.getRequestURI(), e.getMessage());

        String message = "数据操作失败";
        if (e.getMessage().contains("Duplicate entry")) {
            message = "数据已存在，不能重复添加";
        } else if (e.getMessage().contains("foreign key constraint")) {
            message = "存在关联数据，无法删除";
        } else if (e.getMessage().contains("cannot be null")) {
            message = "必填字段不能为空";
        }

        return Result.badRequest(message);
    }

    /**
     * 重复键异常
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        log.error("重复键异常: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.badRequest("数据已存在，不能重复添加");
    }

    /**
     * 认证异常处理
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        log.error("认证异常: {} - {}", request.getRequestURI(), e.getMessage());

        if (e instanceof BadCredentialsException) {
            return Result.unauthorized("用户名或密码错误");
        }

        return Result.unauthorized("认证失败");
    }

    /**
     * 权限异常处理
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.error("权限异常: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.forbidden("权限不足");
    }

    /**
     * 空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<Void> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
        return Result.error("系统内部错误");
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.error("非法参数异常: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.badRequest("参数错误: " + e.getMessage());
    }

    /**
     * 系统异常处理
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
        return Result.error("系统内部错误");
    }
}
