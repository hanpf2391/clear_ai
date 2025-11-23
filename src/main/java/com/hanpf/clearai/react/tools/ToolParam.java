package com.hanpf.clearai.react.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 工具参数注解 - 描述工具方法的参数
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolParam {

    /**
     * 参数名称，如果不指定则使用参数变量名
     */
    String name() default "";

    /**
     * 参数描述，用于AI理解参数用途
     */
    String description() default "";

    /**
     * 是否必需参数
     */
    boolean required() default true;

    /**
     * 默认值（字符串形式）
     */
    String defaultValue() default "";
}