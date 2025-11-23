package com.hanpf.clearai.config;

import com.hanpf.clearai.service.ClearAiAssistantService;

/**
 * ClearAI LangChain4j 配置类（暂时简化版本）
 */
public class ClearAiLangChain4jConfig {

    // 暂时简化，避免编译错误
    public ClearAiAssistantService createAssistantService() {
        return new ClearAiAssistantService() {
            @Override
            public String chat(String message) {
                return "暂时无法处理AI请求，配置问题待修复";
            }
        };
    }
}