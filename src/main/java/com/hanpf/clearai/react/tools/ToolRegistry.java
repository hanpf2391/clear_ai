package com.hanpf.clearai.react.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.hanpf.clearai.utils.ClearAILogger;
import com.hanpf.clearai.react.tools.builtin.CleaningTools;
import com.hanpf.clearai.react.tools.builtin.SystemTools;
import com.hanpf.clearai.react.tools.builtin.FileTools;
import com.hanpf.clearai.react.tools.builtin.CommunicationTools;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册表 - 管理所有ReAct工具的发现、注册和调用
 *
 * 核心功能：
 * 1. 自动发现：通过反射自动发现标记了@ReActTool注解的方法
 * 2. 工具注册：注册工具定义和元数据
 * 3. 动态调用：根据名称和参数动态调用工具
 * 4. 类型转换：自动处理JSON参数到Java参数的类型转换
 * 5. 异常处理：统一的工具调用异常处理
 */
public class ToolRegistry {

    private final Map<String, ToolDefinition> tools;
    private final List<Object> toolInstances;

    public ToolRegistry() {
        this.tools = new ConcurrentHashMap<>();
        this.toolInstances = new ArrayList<>();

        // 注册内置工具实例
        registerBuiltinTools();

        // 发现并注册所有工具
        discoverAndRegisterTools();
    }

    /**
     * 注册内置工具实例
     */
    private void registerBuiltinTools() {
        toolInstances.add(new CleaningTools());
        toolInstances.add(new SystemTools());
        toolInstances.add(new FileTools());
        toolInstances.add(new CommunicationTools());
    }

    /**
     * 发现并注册所有工具
     */
    public void discoverAndRegisterTools() {
        for (Object instance : toolInstances) {
            registerToolsFromInstance(instance);
        }

        ClearAILogger.info("工具注册完成，共注册 " + tools.size() + " 个工具");
    }

    /**
     * 从实例中注册所有标记的方法
     */
    private void registerToolsFromInstance(Object instance) {
        Class<?> clazz = instance.getClass();

        for (Method method : clazz.getDeclaredMethods()) {
            ReActTool toolAnnotation = method.getAnnotation(ReActTool.class);
            if (toolAnnotation != null) {
                try {
                    ToolDefinition toolDef = createToolDefinition(method, toolAnnotation, instance);
                    tools.put(toolDef.getName(), toolDef);
                    ClearAILogger.debug("注册工具: " + toolDef.getName() + " - " + toolDef.getDescription());
                } catch (Exception e) {
                    ClearAILogger.error("注册工具失败: " + method.getName() + ", 错误: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 创建工具定义
     */
    private ToolDefinition createToolDefinition(Method method, ReActTool annotation, Object instance) {
        String toolName = annotation.name().isEmpty() ? method.getName() : annotation.name();
        String description = annotation.description();

        // 解析方法参数
        List<ToolDefinition.Parameter> parameters = new ArrayList<>();
        Parameter[] methodParams = method.getParameters();

        for (int i = 0; i < methodParams.length; i++) {
            Parameter methodParam = methodParams[i];
            ToolParam paramAnnotation = methodParam.getAnnotation(ToolParam.class);

            String paramName = paramAnnotation != null && !paramAnnotation.name().isEmpty() ?
                paramAnnotation.name() : methodParam.getName();
            String paramDescription = paramAnnotation != null ? paramAnnotation.description() : "";
            boolean required = paramAnnotation != null ? paramAnnotation.required() : true;
            String defaultValue = paramAnnotation != null ? paramAnnotation.defaultValue() : "";

            String paramType = getParameterTypeName(methodParam.getType());

            parameters.add(new ToolDefinition.Parameter(
                paramName, paramType, paramDescription, required, defaultValue));
        }

        return new ToolDefinition(toolName, description, parameters, method, instance);
    }

    /**
     * 获取参数类型名称
     */
    private String getParameterTypeName(Class<?> type) {
        if (type == String.class) return "string";
        if (type == Integer.class || type == int.class) return "integer";
        if (type == Boolean.class || type == boolean.class) return "boolean";
        if (type == Long.class || type == long.class) return "long";
        if (type == Double.class || type == double.class) return "double";
        return type.getSimpleName();
    }

    /**
     * 执行工具调用
     */
    public String executeTool(String toolName, JsonNode parameters) throws Exception {
        ToolDefinition toolDef = tools.get(toolName);
        if (toolDef == null) {
            throw new IllegalArgumentException("未找到工具: " + toolName);
        }

        Method method = toolDef.getTargetMethod();
        Object instance = toolDef.getTargetInstance();

        // 准备方法参数
        Object[] args = prepareMethodParameters(toolDef, parameters);

        try {
            // 调用方法
            Object result = method.invoke(instance, args);

            // 处理返回值
            if (result == null) {
                return "工具执行完成，无返回值";
            }

            return result.toString();

        } catch (Exception e) {
            String errorMsg = "工具执行失败: " + e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            ClearAILogger.error("工具调用异常: " + toolName + ", " + errorMsg);
            throw new Exception(errorMsg);
        }
    }

    /**
     * 准备方法参数
     */
    private Object[] prepareMethodParameters(ToolDefinition toolDef, JsonNode parameters) {
        List<ToolDefinition.Parameter> paramDefs = toolDef.getParameters();
        Object[] args = new Object[paramDefs.size()];

        for (int i = 0; i < paramDefs.size(); i++) {
            ToolDefinition.Parameter paramDef = paramDefs.get(i);
            String paramName = paramDef.getName();

            Object value = null;
            if (parameters != null && parameters.has(paramName)) {
                JsonNode paramNode = parameters.get(paramName);
                value = convertJsonNodeToJavaType(paramNode, paramDef.getType());
            } else if (paramDef.isRequired()) {
                throw new IllegalArgumentException("缺少必需参数: " + paramName);
            } else if (paramDef.getDefaultValue() != null && !paramDef.getDefaultValue().toString().isEmpty()) {
                value = convertStringToJavaType(paramDef.getDefaultValue().toString(), paramDef.getType());
            }

            args[i] = value;
        }

        return args;
    }

    /**
     * JSON节点转Java类型
     */
    private Object convertJsonNodeToJavaType(JsonNode node, String targetType) {
        if (node.isNull()) return null;

        switch (targetType.toLowerCase()) {
            case "string":
                return node.asText();
            case "integer":
                return node.asInt();
            case "long":
                return node.asLong();
            case "double":
                return node.asDouble();
            case "boolean":
                return node.asBoolean();
            default:
                return node.asText(); // 默认转为字符串
        }
    }

    /**
     * 字符串转Java类型
     */
    private Object convertStringToJavaType(String value, String targetType) {
        if (value == null || value.isEmpty()) return null;

        switch (targetType.toLowerCase()) {
            case "string":
                return value;
            case "integer":
                try { return Integer.parseInt(value); } catch (NumberFormatException e) { return 0; }
            case "long":
                try { return Long.parseLong(value); } catch (NumberFormatException e) { return 0L; }
            case "double":
                try { return Double.parseDouble(value); } catch (NumberFormatException e) { return 0.0; }
            case "boolean":
                return Boolean.parseBoolean(value);
            default:
                return value;
        }
    }

    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }

    /**
     * 获取所有可用工具
     */
    public List<ToolDefinition> getAvailableTools() {
        return new ArrayList<>(tools.values());
    }

    /**
     * 获取工具数量
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * 获取工具定义
     */
    public ToolDefinition getToolDefinition(String toolName) {
        return tools.get(toolName);
    }

    /**
     * 获取所有工具名称
     */
    public Set<String> getToolNames() {
        return new HashSet<>(tools.keySet());
    }
}