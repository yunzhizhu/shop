package com.example.shop.annotation;

import java.lang.annotation.*;

/**
 * 系统日志注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SystemLog {

    /**
     * 操作描述
     */
    String operation() default "";

    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 动作类型
     */
    String action() default "";

    /**
     * 是否记录参数
     */
    boolean recordParams() default true;

    /**
     * 是否记录返回值
     */
    boolean recordResult() default false;
}
