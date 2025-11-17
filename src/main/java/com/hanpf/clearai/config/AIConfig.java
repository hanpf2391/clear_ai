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
 * AI配置类
 * 使用HTTP直接调用智普AI API
 */
public class AIConfig {

    private static String apiKey = "43365548e8ba4e6d98bf9506dd436fdb.PJgEONyl2iT1PvY0";
    private static final String ZHIPU_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

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
     * 设置API密钥
     * @param newApiKey 智普AI API密钥
     */
    public static void setApiKey(String newApiKey) {
        apiKey = newApiKey;
        // 清空聊天记忆以便重新开始
        chatMemoryMap.clear();
    }

    /**
     * ChatService的实现类
     */
    private static class ChatServiceImpl implements ChatService {

        @Override
        public String chat(String message) {
            try {
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

                return callZhipuAI(messages);
            } catch (Exception e) {
                return "错误：" + e.getMessage();
            }
        }

        @Override
        public String chatWithMemory(String memoryId, String message) {
            try {
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

                String response = callZhipuAI(messages);

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

        private String callZhipuAI(List<Map<String, String>> messages) throws IOException {
            // 构建请求体JSON字符串
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{");
            jsonBuilder.append("\"model\":\"glm-4.5-air\",");
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
            jsonBuilder.append("\"max_tokens\":1000,");
            jsonBuilder.append("\"temperature\":0.7");
            jsonBuilder.append("}");

            String jsonInputString = jsonBuilder.toString();

            URL url = new URL(ZHIPU_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            try {
                // 设置请求属性
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);

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
                            throw new IOException("API调用失败: " + responseCode + " " + response.toString());
                        } else {
                            throw new IOException("API调用失败: " + responseCode);
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
            // 使用正则表达式提取content字段
            Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return unescapeJson(matcher.group(1));
            }
            throw new RuntimeException("无法解析API响应: " + json);
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