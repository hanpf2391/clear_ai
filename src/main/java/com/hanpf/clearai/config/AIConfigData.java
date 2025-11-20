package com.hanpf.clearai.config;

/**
 * AI配置数据类
 * 用于JSON格式的外置配置文件
 */
public class AIConfigData {

    /**
     * AI环境配置
     */
    private AIEnvConfig env;

    /**
     * 权限配置
     */
    private PermissionsConfig permissions;

    // Getters and Setters
    public AIEnvConfig getEnv() {
        return env;
    }

    public void setEnv(AIEnvConfig env) {
        this.env = env;
    }

    public PermissionsConfig getPermissions() {
        return permissions;
    }

    public void setPermissions(PermissionsConfig permissions) {
        this.permissions = permissions;
    }

    /**
     * AI环境配置类
     */
    public static class AIEnvConfig {
        /**
         * API密钥
         */
        private String API_KEY;

        /**
         * API基础URL
         */
        private String BASE_URL;

        /**
         * Anthropic认证令牌
         */
        private String ANTHROPIC_AUTH_TOKEN;

        /**
         * Anthropic基础URL
         */
        private String ANTHROPIC_BASE_URL;

        /**
         * Claude代码禁用非必要流量
         */
        private Integer CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC = 1;

        /**
         * API请求超时时间（毫秒）
         */
        private Integer API_TIMEOUT_MS = 60000;

        /**
         * 主要模型名称
         */
        private String MODEL = "glm-4.5-air";

        /**
         * Anthropic模型
         */
        private String ANTHROPIC_MODEL = "glm-4.6";

        /**
         * Anthropic小型快速模型
         */
        private String ANTHROPIC_SMALL_FAST_MODEL = "glm-4.6";

        /**
         * 快速响应模型（可选）
         */
        private String SMALL_FAST_MODEL;

        /**
         * 温度参数
         */
        private Double TEMPERATURE = 0.7;

        /**
         * 最大token数
         */
        private Integer MAX_TOKENS = 1000;

        /**
         * AI提供商类型（可选，用于显示）
         */
        private String PROVIDER_NAME = "自定义";

        // Getters and Setters
        public String getAPI_KEY() {
            return API_KEY;
        }

        public void setAPI_KEY(String API_KEY) {
            this.API_KEY = API_KEY;
        }

        public String getBASE_URL() {
            return BASE_URL;
        }

        public void setBASE_URL(String BASE_URL) {
            this.BASE_URL = BASE_URL;
        }

        public String getANTHROPIC_AUTH_TOKEN() {
            return ANTHROPIC_AUTH_TOKEN;
        }

        public void setANTHROPIC_AUTH_TOKEN(String ANTHROPIC_AUTH_TOKEN) {
            this.ANTHROPIC_AUTH_TOKEN = ANTHROPIC_AUTH_TOKEN;
        }

        public String getANTHROPIC_BASE_URL() {
            return ANTHROPIC_BASE_URL;
        }

        public void setANTHROPIC_BASE_URL(String ANTHROPIC_BASE_URL) {
            this.ANTHROPIC_BASE_URL = ANTHROPIC_BASE_URL;
        }

        public Integer getCLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC() {
            return CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC;
        }

        public void setCLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC(Integer CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC) {
            this.CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC = CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC;
        }

        public Integer getAPI_TIMEOUT_MS() {
            return API_TIMEOUT_MS;
        }

        public void setAPI_TIMEOUT_MS(Integer API_TIMEOUT_MS) {
            this.API_TIMEOUT_MS = API_TIMEOUT_MS;
        }

        public String getMODEL() {
            return MODEL;
        }

        public void setMODEL(String MODEL) {
            this.MODEL = MODEL;
        }

        public String getANTHROPIC_MODEL() {
            return ANTHROPIC_MODEL;
        }

        public void setANTHROPIC_MODEL(String ANTHROPIC_MODEL) {
            this.ANTHROPIC_MODEL = ANTHROPIC_MODEL;
        }

        public String getANTHROPIC_SMALL_FAST_MODEL() {
            return ANTHROPIC_SMALL_FAST_MODEL;
        }

        public void setANTHROPIC_SMALL_FAST_MODEL(String ANTHROPIC_SMALL_FAST_MODEL) {
            this.ANTHROPIC_SMALL_FAST_MODEL = ANTHROPIC_SMALL_FAST_MODEL;
        }

        public String getSMALL_FAST_MODEL() {
            return SMALL_FAST_MODEL;
        }

        public void setSMALL_FAST_MODEL(String SMALL_FAST_MODEL) {
            this.SMALL_FAST_MODEL = SMALL_FAST_MODEL;
        }

        public Double getTEMPERATURE() {
            return TEMPERATURE;
        }

        public void setTEMPERATURE(Double TEMPERATURE) {
            this.TEMPERATURE = TEMPERATURE;
        }

        public Integer getMAX_TOKENS() {
            return MAX_TOKENS;
        }

        public void setMAX_TOKENS(Integer MAX_TOKENS) {
            this.MAX_TOKENS = MAX_TOKENS;
        }

        public String getPROVIDER_NAME() {
            return PROVIDER_NAME;
        }

        public void setPROVIDER_NAME(String PROVIDER_NAME) {
            this.PROVIDER_NAME = PROVIDER_NAME;
        }
    }

    /**
     * 权限配置类
     */
    public static class PermissionsConfig {
        /**
         * 允许的功能列表
         */
        private String[] allow = {};

        /**
         * 禁止的功能列表
         */
        private String[] deny = {};

        // Getters and Setters
        public String[] getAllow() {
            return allow;
        }

        public void setAllow(String[] allow) {
            this.allow = allow;
        }

        public String[] getDeny() {
            return deny;
        }

        public void setDeny(String[] deny) {
            this.deny = deny;
        }
    }
}