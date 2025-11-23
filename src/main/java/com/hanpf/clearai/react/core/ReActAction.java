package com.hanpf.clearai.react.core;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * ReAct动作数据结构
 * 表示要执行的工具调用
 */
public class ReActAction {

    private String toolName;      // 工具名称
    private JsonNode parameters;   // 工具参数

    public ReActAction() {}

    public ReActAction(String toolName, JsonNode parameters) {
        this.toolName = toolName;
        this.parameters = parameters;
    }

    // 获取字符串参数值
    public String getStringParameter(String paramName) {
        if (parameters != null && parameters.has(paramName)) {
            return parameters.get(paramName).asText();
        }
        return null;
    }

    // 获取布尔参数值
    public boolean getBooleanParameter(String paramName, boolean defaultValue) {
        if (parameters != null && parameters.has(paramName)) {
            return parameters.get(paramName).asBoolean(defaultValue);
        }
        return defaultValue;
    }

    // 获取整数参数值
    public int getIntParameter(String paramName, int defaultValue) {
        if (parameters != null && parameters.has(paramName)) {
            return parameters.get(paramName).asInt(defaultValue);
        }
        return defaultValue;
    }

    // 检查参数是否存在
    public boolean hasParameter(String paramName) {
        return parameters != null && parameters.has(paramName);
    }

    // Getters and Setters
    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public JsonNode getParameters() {
        return parameters;
    }

    public void setParameters(JsonNode parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "ReActAction{toolName='" + toolName + "', parameters=" + parameters + "}";
    }
}