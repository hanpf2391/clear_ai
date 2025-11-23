package com.hanpf.clearai.react.agent;

import com.hanpf.clearai.react.core.ReActAgentExecutor;
import java.util.List;

/**
 * 重构后的CLEAR AI智能代理 - 基于真正的ReAct架构
 *
 * 新架构特点：
 * - 使用ReActAgentExecutor作为核心执行引擎
 * - 完全自主的决策循环，直到AI判断任务完成
 * - 基于注解的工具系统，自动发现和调用
 * - 动态上下文注入，智能Prompt构建
 * - 对话状态管理，支持历史记录和恢复
 */
public class ClearAiAgent {

    private final ReActAgentExecutor executor;

    public ClearAiAgent() {
        // 创建ReAct执行器，包含完整的工具系统和状态管理
        this.executor = new ReActAgentExecutor();
    }

    /**
     * 统一的对话入口 - 处理所有用户输入
     *
     * 工作流程：
     * 1. 用户输入进入ReAct循环
     * 2. AI分析需求并制定执行计划
     * 3. 自主调用工具获取信息和执行操作
     * 4. 根据工具结果调整策略
     * 5. 循环直到任务完成，给出最终答案
     *
     * @param userInput 用户输入
     * @return AI的最终响应
     */
    public String chat(String userInput) {
        try {
            // 直接委托给ReAct执行器处理
            return executor.processInput(userInput);
        } catch (Exception e) {
            // 简化的错误处理，ReAct执行器内部已有详细的错误处理
            return "❌ 处理请求时出现问题：" + e.getMessage();
        }
    }

    /**
     * 带记忆功能的对话 - 扩展接口（暂未实现）
     *
     * 未来可以基于对话ID实现多会话管理：
     * - 不同用户使用不同的conversationId
     * - 支持历史对话的恢复和继续
     * - 实现长期记忆和用户偏好学习
     */
    public String chatWithMemory(String memoryId, String userInput) {
        // 当前版本简化实现：直接调用普通chat方法
        // 未来可以基于memoryId管理不同的对话状态
        return chat(userInput);
    }

    /**
     * 重置对话状态
     */
    public void resetConversation() {
        executor.resetConversation();
    }

    /**
     * 获取对话历史
     */
    public List<String> getConversationHistory() {
        return executor.getConversationHistory();
    }
}