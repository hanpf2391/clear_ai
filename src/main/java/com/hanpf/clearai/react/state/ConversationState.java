package com.hanpf.clearai.react.state;

import com.hanpf.clearai.react.core.ReActDecision;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 对话状态数据结构
 * 维护ReAct循环所需的全部上下文信息
 */
public class ConversationState {

    private final String conversationId;
    private final LocalDateTime createdAt;

    // 对话历史
    private final List<String> conversationHistory;

    // 用户消息历史
    private final List<String> userMessages;

    // AI决策历史
    private final List<ReActDecision> decisionHistory;

    // 工具调用记录
    private final Map<String, List<String>> toolResults;

    // 当前循环次数
    private int currentLoop;

    // 对话元数据
    private final Map<String, Object> metadata;

    public ConversationState() {
        this.conversationId = UUID.randomUUID().toString().substring(0, 8);
        this.createdAt = LocalDateTime.now();
        this.conversationHistory = new ArrayList<>();
        this.userMessages = new ArrayList<>();
        this.decisionHistory = new ArrayList<>();
        this.toolResults = new HashMap<>();
        this.metadata = new HashMap<>();
        this.currentLoop = 0;
    }

    /**
     * 添加用户消息
     */
    public void addUserMessage(String message) {
        userMessages.add(message);
        conversationHistory.add("User: " + message);
    }

    /**
     * 添加AI决策
     */
    public void addDecision(ReActDecision decision) {
        decisionHistory.add(decision);
        currentLoop++;

        if (decision.isFinalAnswer()) {
            conversationHistory.add("CLEAR AI: " + decision.getFinalAnswer());
        } else if (decision.hasAction()) {
            conversationHistory.add("CLEAR AI: 调用工具 " + decision.getAction().getToolName());
        }
    }

    /**
     * 添加工具执行结果
     */
    public void addToolResult(String toolName, String result) {
        toolResults.computeIfAbsent(toolName, k -> new ArrayList<>()).add(result);

        // 限制结果长度，避免上下文过长
        String truncatedResult = result.length() > 1000 ?
            result.substring(0, 1000) + "...\n[结果过长已截断]" : result;

        conversationHistory.add("Tool[" + toolName + "]: " + truncatedResult);
    }

    /**
     * 获取工具的调用历史
     */
    public List<String> getToolHistory(String toolName) {
        return toolResults.getOrDefault(toolName, new ArrayList<>());
    }

    /**
     * 检查工具是否已被调用过
     */
    public boolean hasToolBeenCalled(String toolName) {
        return toolResults.containsKey(toolName) && !toolResults.get(toolName).isEmpty();
    }

    /**
     * 获取对话摘要（用于Prompt构建）
     */
    public String getConversationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("对话ID: ").append(conversationId).append("\n");
        summary.append("开始时间: ").append(createdAt).append("\n");
        summary.append("当前循环: ").append(currentLoop).append("\n");

        if (!userMessages.isEmpty()) {
            summary.append("用户消息数: ").append(userMessages.size()).append("\n");
        }

        if (!toolResults.isEmpty()) {
            summary.append("已调用工具: ").append(String.join(", ", toolResults.keySet())).append("\n");
        }

        return summary.toString();
    }

    /**
     * 获取格式化的对话历史（用于Prompt构建）
     */
    public String getFormattedHistory() {
        StringBuilder history = new StringBuilder();

        // 只保留最近的部分对话，避免Prompt过长
        int maxHistory = Math.min(15, conversationHistory.size());
        int startIndex = Math.max(0, conversationHistory.size() - maxHistory);

        for (int i = startIndex; i < conversationHistory.size(); i++) {
            history.append(conversationHistory.get(i)).append("\n");
        }

        return history.toString();
    }

    /**
     * 设置元数据
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * 获取元数据
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    // Getters
    public String getConversationId() {
        return conversationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<String> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }

    public List<String> getUserMessages() {
        return new ArrayList<>(userMessages);
    }

    public List<ReActDecision> getDecisionHistory() {
        return new ArrayList<>(decisionHistory);
    }

    public Map<String, List<String>> getToolResults() {
        return new HashMap<>(toolResults);
    }

    public int getCurrentLoop() {
        return currentLoop;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    @Override
    public String toString() {
        return "ConversationState{" +
                "conversationId='" + conversationId + '\'' +
                ", currentLoop=" + currentLoop +
                ", userMessages=" + userMessages.size() +
                ", toolCalls=" + toolResults.size() +
                '}';
    }
}