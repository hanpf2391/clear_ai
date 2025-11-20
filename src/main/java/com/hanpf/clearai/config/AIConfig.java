package com.hanpf.clearai.config;

import com.hanpf.clearai.service.ChatService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI配置类 - 支持多平台AI服务
 * 可配置使用不同的AI提供商
 */
public class AIConfig {

    // 存储不同会话的聊天记忆
    private static final Map<String, List<Map<String, String>>> chatMemoryMap = new HashMap<>();

    /**
     * 创建ChatService实现
     * @return ChatService实现
     */
    public static ChatService createChatService() {
        return new ChatServiceImpl();
    }

    /**
     * 获取指定会话的聊天记忆
     * @param memoryId 会话ID
     * @return 消息列表
     */
    private static List<Map<String, String>> getChatMemory(String memoryId) {
        return chatMemoryMap.computeIfAbsent(memoryId, k -> new ArrayList<>());
    }

    /**
     * 设置API密钥 - 已废弃，请直接编辑setting.json文件
     * @param newApiKey AI API密钥
     * @deprecated 配置信息现在只能通过setting.json文件修改
     */
    @Deprecated
    public static void setApiKey(String newApiKey) {
        throw new UnsupportedOperationException("请通过编辑setting.json文件来修改API密钥");
    }

    /**
     * 设置AI提供商 - 已废弃，请直接编辑setting.json文件
     * @param provider AI提供商
     * @deprecated 配置信息现在只能通过setting.json文件修改
     */
    @Deprecated
    public static void setProvider(AIProvider provider) {
        throw new UnsupportedOperationException("请通过编辑setting.json文件来修改AI提供商");
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
     * ChatService的实现类
     */
    private static class ChatServiceImpl implements ChatService {

        @Override
        public String chat(String message) {
            try {
                if (!AIConfigManager.isConfigComplete()) {
                    return "错误：请先配置AI提供商的完整信息（API密钥、URL、模型等）";
                }

                // 构建消息列表
                List<Map<String, String>> messages = new ArrayList<>();
                Map<String, String> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", "你是一个智能清理助手，帮助用户分析和清理他们的计算机。你的回答应该简洁、专业且有用。");
                messages.add(systemMessage);

                Map<String, String> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", message);
                messages.add(userMessage);

                return callAI(messages);
            } catch (Exception e) {
                return "错误：" + e.getMessage();
            }
        }

        @Override
        public String chatWithMemory(String memoryId, String message) {
            try {
                if (!AIConfigManager.isConfigComplete()) {
                    return "错误：请先配置AI提供商的完整信息（API密钥、URL、模型等）";
                }

                List<Map<String, String>> messages = getChatMemory(memoryId);

                // 如果是新的会话，添加系统消息
                if (messages.isEmpty()) {
                    Map<String, String> systemMessage = new HashMap<>();
                    systemMessage.put("role", "system");
                    systemMessage.put("content", "你是一个智能清理助手，帮助用户分析和清理他们的计算机。记住之前的对话内容，提供连贯的建议。");
                    messages.add(systemMessage);
                }

                // 添加用户消息
                Map<String, String> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", message);
                messages.add(userMessage);

                String response = callAI(messages);

                // 添加AI回复到记忆中
                Map<String, String> assistantMessage = new HashMap<>();
                assistantMessage.put("role", "assistant");
                assistantMessage.put("content", response);
                messages.add(assistantMessage);

                return response;
            } catch (Exception e) {
                return "错误：" + e.getMessage();
            }
        }

        private String callAI(List<Map<String, String>> messages) throws IOException {
            // 构建请求体JSON字符串
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{");
            jsonBuilder.append("\"model\":\"").append(AIConfigManager.getCurrentModel()).append("\",");
            jsonBuilder.append("\"messages\":[");

            for (int i = 0; i < messages.size(); i++) {
                Map<String, String> message = messages.get(i);
                jsonBuilder.append("{");
                jsonBuilder.append("\"role\":\"").append(escapeJson(message.get("role"))).append("\",");
                jsonBuilder.append("\"content\":\"").append(escapeJson(message.get("content"))).append("\"");
                jsonBuilder.append("}");
                if (i < messages.size() - 1) {
                    jsonBuilder.append(",");
                }
            }

            jsonBuilder.append("],");
            jsonBuilder.append("\"max_tokens\":").append(AIConfigManager.getMaxTokens()).append(",");
            jsonBuilder.append("\"temperature\":").append(AIConfigManager.getTemperature());
            jsonBuilder.append("}");

            String jsonInputString = jsonBuilder.toString();

            URL url = new URL(AIConfigManager.getApiUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            try {
                // 设置请求属性
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");

                // 根据API URL判断使用哪种认证方式
                String apiUrl = AIConfigManager.getApiUrl();
                if (apiUrl != null && apiUrl.contains("bigmodel.cn")) {
                    // 智普AI使用特定的认证格式
                    connection.setRequestProperty("Authorization", AIConfigManager.getCurrentApiKey());
                } else {
                    // 其他服务使用Bearer token格式
                    connection.setRequestProperty("Authorization", "Bearer " + AIConfigManager.getCurrentApiKey());
                }

                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(AIConfigManager.getTimeout() * 1000);
                connection.setReadTimeout(AIConfigManager.getTimeout() * 1000);

                // 发送请求体
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // 获取响应
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    try (InputStream is = connection.getErrorStream()) {
                        if (is != null) {
                            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                            StringBuilder response = new StringBuilder();
                            String responseLine;
                            while ((responseLine = br.readLine()) != null) {
                                response.append(responseLine.trim());
                            }
                            throw new IOException("AI API调用失败: " + responseCode + " " + response.toString());
                        } else {
                            throw new IOException("AI API调用失败: " + responseCode);
                        }
                    }
                }

                // 读取响应
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // 使用正则表达式解析JSON响应
                    return extractContentFromJson(response.toString());
                }
            } finally {
                connection.disconnect();
            }
        }

        private String escapeJson(String text) {
            if (text == null) return "";
            return text.replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                      .replace("\n", "\\n")
                      .replace("\r", "\\r")
                      .replace("\t", "\\t");
        }

        private String extractContentFromJson(String json) {
            try {
                // 首先检查是否是错误响应
                if (json.contains("\"code\":") || json.contains("\"msg\":")) {
                    // 尝试提取错误消息
                    Pattern errorPattern = Pattern.compile("\"msg\"\\s*:\\s*\"([^\"]*)\"");
                    Matcher errorMatcher = errorPattern.matcher(json);
                    if (errorMatcher.find()) {
                        return "API错误: " + unescapeJson(errorMatcher.group(1));
                    }
                    Pattern codePattern = Pattern.compile("\"code\"\\s*:\\s*(\\d+)");
                    Matcher codeMatcher = codePattern.matcher(json);
                    if (codeMatcher.find()) {
                        return "API错误，错误代码: " + codeMatcher.group(1);
                    }
                    return "API返回错误响应: " + json;
                }

                // 尝试提取正常的content字段
                Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"([^\"]*)\"");
                Matcher matcher = pattern.matcher(json);
                if (matcher.find()) {
                    return unescapeJson(matcher.group(1));
                }

                // 如果都没找到，返回原始响应
                return "无法解析API响应，原始内容: " + json;
            } catch (Exception e) {
                return "解析API响应时出错: " + e.getMessage() + "，原始响应: " + json;
            }
        }

        private String unescapeJson(String text) {
            return text.replace("\\\"", "\"")
                      .replace("\\n", "\n")
                      .replace("\\r", "\r")
                      .replace("\\t", "\t")
                      .replace("\\\\", "\\");
        }
    }
}