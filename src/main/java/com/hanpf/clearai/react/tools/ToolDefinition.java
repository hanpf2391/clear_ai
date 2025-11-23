package com.hanpf.clearai.react.tools;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * 工具定义数据结构 - 描述一个可调用的工具
 */
public class ToolDefinition {

    private final String name;
    private final String description;
    private final List<Parameter> parameters;
    private final Method targetMethod;
    private final Object targetInstance;

    public ToolDefinition(String name, String description, List<Parameter> parameters,
                         Method targetMethod, Object targetInstance) {
        this.name = name;
        this.description = description;
        this.parameters = new ArrayList<>(parameters);
        this.targetMethod = targetMethod;
        this.targetInstance = targetInstance;
    }

    /**
     * 工具参数定义
     */
    public static class Parameter {
        private final String name;
        private final String type;
        private final String description;
        private final boolean required;
        private final Object defaultValue;

        public Parameter(String name, String type, String description, boolean required, Object defaultValue) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.required = required;
            this.defaultValue = defaultValue;
        }

        // Getters
        public String getName() { return name; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public boolean isRequired() { return required; }
        public Object getDefaultValue() { return defaultValue; }

        @Override
        public String toString() {
            return String.format("Parameter{name='%s', type='%s', required=%s}", name, type, required);
        }
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<Parameter> getParameters() { return new ArrayList<>(parameters); }
    public Method getTargetMethod() { return targetMethod; }
    public Object getTargetInstance() { return targetInstance; }

    @Override
    public String toString() {
        return String.format("ToolDefinition{name='%s', description='%s', params=%d}",
            name, description, parameters.size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolDefinition that = (ToolDefinition) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}