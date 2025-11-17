package com.hanpf.clearai.service;

/**
 * AI聊天服务接口
 * 简化版本，不使用注解，直接通过编程方式调用
 */
public interface ChatService {

    /**
     * 普通聊天方法
     * @param message 用户消息
     * @return AI回复
     */
    String chat(String message);

    /**
     * 带会话记忆的聊天方法
     * @param memoryId 会话ID，用于区分不同的用户会话
     * @param message 用户消息
     * @return AI回复
     */
    String chatWithMemory(String memoryId, String message);
}