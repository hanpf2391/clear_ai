package com.hanpf.clearai.config;

import com.hanpf.clearai.service.ChatService;
import com.hanpf.clearai.utils.ClearAILogger;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.output.Response;

import java.time.Duration;

/**
 * AI配置类 - 基于LangChain4j框架
 * 支持多平台AI服务
 */
public class AIConfig {

    /**
     * 创建ChatService实现 - 使用LangChain4j
     * @return ChatService实现
     */
    public static ChatService createChatService() {
        return new LangChain4jChatService();
    }

    /**
     * 获取当前AI提供商名称
     */
    public static String getCurrentProviderName() {
        return AIConfigManager.getProviderName();
    }

    /**
     * 检查API是否已配置好可以工作
     */
    public static boolean isConfigured() {
        return AIConfigManager.isApiKeyConfigured();
    }

    /**
     * 基于LangChain4j的ChatService实现
     */
    private static class LangChain4jChatService implements ChatService {

        private final ChatLanguageModel chatModel;

        public LangChain4jChatService() {
            this.chatModel = createChatModel();
        }

        @Override
        public String chat(String message) {
            try {
                if (!AIConfigManager.isConfigComplete()) {
                    return "错误：请先配置AI提供商的完整信息（API密钥、URL、模型等）";
                }

                // 构建详细的系统提示
                String systemPrompt = """
                    你是一个名为CLEAR AI的智能电脑清理助手。请以自然、友好的方式回应用户。

                    请注意：
                    1. 如果用户问清理相关问题，提供专业的清理建议
                    2. 如果用户问其他问题，礼貌回答并引导回清理功能
                    3. 保持对话自然，不要生硬
                    4. 可以进行简单的数学计算
                    5. 可以回答常识性问题，但要点明你的专长是电脑清理

                    请直接回复用户，不需要JSON格式。
                    """;
                SystemMessage systemMessage = SystemMessage.from(systemPrompt);

                // 构建用户消息
                UserMessage userMessage = UserMessage.from(message);

                // 记录AI请求到日志
                String fullPrompt = systemPrompt + "\n\n用户消息: " + message;
                ClearAILogger.logAIResponse(fullPrompt, "等待AI响应...");

                // 调用LangChain4j模型
                long startTime = System.currentTimeMillis();
                Response<AiMessage> response = chatModel.generate(systemMessage, userMessage);
                long endTime = System.currentTimeMillis();

                String result = response.content().text();

                // 记录AI响应到日志
                ClearAILogger.logAIResponse("AI调用完成",
                    "响应: " + result + "\n调用耗时: " + (endTime - startTime) + "ms");

                return result;

            } catch (Exception e) {
                ClearAILogger.error("LangChain4j调用失败: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
                return "错误：" + e.getMessage();
            }
        }

        @Override
        public String chatWithMemory(String memoryId, String message) {
            try {
                if (!AIConfigManager.isConfigComplete()) {
                    return "错误：请先配置AI提供商的完整信息（API密钥、URL、模型等）";
                }

                // 简化版本：直接调用chat方法，没有记忆功能
                // 可以根据需要扩展为带有记忆功能的实现
                return chat(message);

            } catch (Exception e) {
                return "错误：" + e.getMessage();
            }
        }

        /**
         * 创建LangChain4j的ChatLanguageModel
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
                    .build();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create ChatLanguageModel: " + e.getMessage(), e);
            }
        }
    }
}