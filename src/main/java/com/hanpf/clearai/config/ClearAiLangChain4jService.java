package com.hanpf.clearai.config;

import com.hanpf.clearai.service.ChatService;
import com.hanpf.clearai.service.ClearAiAssistantService;

/**
 * ClearAI的LangChain4j服务实现（暂时简化版本）
 * 手动配置版本，替代Spring Boot自动配置
 * 适配现有的ChatService接口
 */
public class ClearAiLangChain4jService implements ChatService {

    private ClearAiAssistantService clearAiAssistantService;

    public ClearAiLangChain4jService(ClearAiAssistantService service) {
        this.clearAiAssistantService = service;
    }

    @Override
    public String chat(String message) {
        return clearAiAssistantService.chat(message);
    }

    @Override
    public String chatWithMemory(String memoryId, String message) {
        // 暂时调用基础聊天方法
        return clearAiAssistantService.chat(message);
    }

    /**
     * 检查服务是否可用（暂时简化版本）
     */
    public boolean isAvailable() {
        return clearAiAssistantService != null;
    }
}