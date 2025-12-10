package com.hanpf.clearai.agent;

import com.hanpf.clearai.config.AIConfigManager;
import com.hanpf.clearai.agent.tools.LangChain4jCleaningTools;
import com.hanpf.clearai.agent.tools.LangChain4jSystemTools;
import com.hanpf.clearai.agent.tools.LangChain4jFileTools;
import com.hanpf.clearai.agent.tools.LangChain4jCommunicationTools;
import com.hanpf.clearai.utils.ClearAILogger;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;

import java.time.Duration;
import java.util.List;

/**
 * 基于LangChain4j的ClearAI智能代理
 *
 * 替代自实现的ReAct框架，使用LangChain4j原生的Agent功能：
 * - @Tool注解标记工具方法
 * - AiServices构建智能代理
 * - 内置ReAct循环和决策
 * - 自动对话记忆管理
 */
public class LangChain4jClearAiAgent {

    private final ChatLanguageModel chatModel;
    private final ClearAiAgentInterface agent;
    private final MessageWindowChatMemory chatMemory;

    /**
     * ClearAI Agent接口定义
     * 使用LangChain4j的@Tool注解标记工具方法
     */
    interface ClearAiAgentInterface {

        /**
         * 聊天接口 - LangChain4j会自动处理工具调用和ReAct循环
         */
        String chat(String userMessage);
    }

    public LangChain4jClearAiAgent() {
        // 创建ChatLanguageModel
        this.chatModel = createChatModel();

        // 创建对话记忆（保持最近20条消息）
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(20);

        // 构建LangChain4j Agent
        this.agent = AiServices.builder(ClearAiAgentInterface.class)
                .chatLanguageModel(chatModel)
                .chatMemory(chatMemory)
                .tools(
                    new LangChain4jCleaningTools(),
                    new LangChain4jSystemTools(),
                    new LangChain4jFileTools(),
                    new LangChain4jCommunicationTools()
                )
                .build();

        ClearAILogger.info("LangChain4j ClearAI Agent 初始化完成");
    }

    /**
     * 处理用户输入的主要入口
     * @param userInput 用户输入
     * @return AI响应
     */
    public String processUserInput(String userInput) {
        try {
            if (!AIConfigManager.isConfigComplete()) {
                return "❌ 错误：请先配置AI提供商的完整信息（API密钥、URL、模型等）";
            }

            ClearAILogger.info("=== 开始处理用户输入 ===");
            ClearAILogger.info("用户输入: " + userInput);
            ClearAILogger.info("Agent类型: LangChain4j ClearAI Agent");
            ClearAILogger.info("可用工具: 清理工具、系统工具、文件工具、通信工具");

            long startTime = System.currentTimeMillis();
            ClearAILogger.info("开始调用LangChain4j Agent...");

            // LangChain4j会自动处理ReAct循环、工具调用和决策
            String response = agent.chat(userInput);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            ClearAILogger.info("=== LangChain4j Agent处理完成 ===");
            ClearAILogger.info("总耗时: " + duration + "ms");
            ClearAILogger.info("响应长度: " + response.length() + " 字符");

            // 尝试检测是否调用了工具
            if (response.contains("目录扫描结果") || response.contains("系统信息") ||
                response.contains("文件分析") || response.contains("需要用户确认")) {
                ClearAILogger.info("✅ 检测到工具调用");
            } else {
                ClearAILogger.info("💬 纯文本响应");
            }

            return response;

        } catch (Exception e) {
            ClearAILogger.error("❌ 处理用户输入时出错: " + e.getMessage(), e);
            ClearAILogger.error("错误类型: " + e.getClass().getSimpleName());
            return "❌ 处理请求时出错：" + e.getMessage();
        }
    }

    /**
     * 重置对话记忆
     */
    public void resetConversation() {
        chatMemory.clear();
        ClearAILogger.info("LangChain4j Agent 对话记忆已重置");
    }

    /**
     * 获取当前对话历史
     */
    public List<String> getConversationHistory() {
        // LangChain4j的MessageWindowChatMemory提供了访问历史消息的方法
        return List.of("对话历史功能暂时不可用");
    }

    /**
     * 创建ChatLanguageModel
     */
    private ChatLanguageModel createChatModel() {
        try {
            return OpenAiChatModel.builder()
                    .baseUrl(AIConfigManager.getApiUrl())
                    .apiKey(AIConfigManager.getCurrentApiKey())
                    .modelName(AIConfigManager.getCurrentModel())
                    .temperature(AIConfigManager.getTemperature())
                    .maxTokens(AIConfigManager.getMaxTokens())
                    .timeout(Duration.ofSeconds(AIConfigManager.getTimeout()))
                    .logRequests(false) // 关闭请求日志，使用我们自己的日志系统
                    .logResponses(false)
                    .build();
        } catch (Exception e) {
            ClearAILogger.error("创建ChatLanguageModel失败: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create ChatLanguageModel: " + e.getMessage(), e);
        }
    }
}