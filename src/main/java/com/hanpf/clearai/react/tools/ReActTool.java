package com.hanpf.clearai.react.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ReAct工具注解 - 标记可被AI调用的工具方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReActTool {

    /**
     * 工具名称，如果不指定则使用方法名
     */
    String name() default "";

    /**
     * 工具描述，用于AI理解工具用途
     */
    String description();

    /**
     * 工具类别，用于工具分类管理
     */
    String category() default "general";
}