package com.hanpf.clearai.config;

/**
 * AI服务提供商枚举
 * 定义支持的AI平台及其配置信息
 */
public enum AIProvider {
    // 智普AI
    ZHIPU("智普AI", "https://open.bigmodel.cn/api/paas/v4/chat/completions", "glm-4.5-air"),

    // OpenAI
    OPENAI("OpenAI", "https://api.openai.com/v1/chat/completions", "gpt-3.5-turbo"),

    // 百度文心一言
    BAIDU("百度文心一言", "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions", "ernie-bot-turbo"),

    // 阿里云通义千问
    ALIBABA("阿里云通义千问", "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation", "qwen-turbo"),

    // 腾讯混元
    TENCENT("腾讯混元", "https://hunyuan.tencentcloudapi.com/", "hunyuan-lite");

    private final String displayName;
    private final String apiUrl;
    private final String defaultModel;

    AIProvider(String displayName, String apiUrl, String defaultModel) {
        this.displayName = displayName;
        this.apiUrl = apiUrl;
        this.defaultModel = defaultModel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    /**
     * 根据名称查找AI提供商
     * @param name 提供商名称
     * @return 对应的AI提供商，如果没找到则返回智普AI
     */
    public static AIProvider fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return ZHIPU; // 默认使用智普AI
        }

        for (AIProvider provider : values()) {
            if (provider.name().equalsIgnoreCase(name) ||
                provider.displayName.equalsIgnoreCase(name)) {
                return provider;
            }
        }
        return ZHIPU; // 如果没找到，默认使用智普AI
    }
}