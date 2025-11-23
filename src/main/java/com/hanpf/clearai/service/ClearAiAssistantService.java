package com.hanpf.clearai.service;

/**
 * 清理助手AI服务接口（暂时简化版本）
 * 基于LangChain4j的注解驱动服务
 * 手动配置版本，适配setting.json配置
 */
public interface ClearAiAssistantService {

    /**
     * 普通聊天方法 - 不带记忆
     * @param message 用户消息
     * @return AI回复
     */
    String chat(String message);

    /**
     * 获取ChatService实例（暂时简化）
     * @return ChatService实例
     */
    default Object getChatService() {
        return this;
    }
}