package com.hanpf.clearai.cli.cleaning;

import com.hanpf.clearai.cli.cleaning.models.AnalysisResult;
import com.hanpf.clearai.cli.cleaning.models.ChatMessage;
import com.hanpf.clearai.cli.cleaning.models.FileInfo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话管理器 - 管理聊天历史和上下文
 */
public class ChatSessionManager {
    private final List<ChatMessage> history;
    private FileAnalysisContext currentContext;
    private final int maxHistorySize;
    private final String sessionId;

    public ChatSessionManager() {
        this.history = new ArrayList<>();
        this.maxHistorySize = 20; // 保留最近20条消息
        this.sessionId = "session_" + System.currentTimeMillis();
    }

    /**
     * 添加用户消息
     */
    public void addUserMessage(String content) {
        ChatMessage message = new ChatMessage("user", content);
        addToHistory(message);
    }

    /**
     * 添加AI助手消息
     */
    public void addAssistantMessage(String content) {
        ChatMessage message = new ChatMessage("assistant", content);
        addToHistory(message);
    }

    /**
     * 添加系统消息
     */
    public void addSystemMessage(String content) {
        ChatMessage message = new ChatMessage("system", content, "system");
        addToHistory(message);
    }

    /**
     * 添加分析结果
     */
    public void addAnalysisResult(String content) {
        ChatMessage message = new ChatMessage("assistant", content, "analysis");
        addToHistory(message);
    }

    /**
     * 设置分析上下文
     */
    public void setAnalysisContext(FileAnalysisContext context) {
        this.currentContext = context;
    }

    /**
     * 获取当前分析上下文
     */
    public FileAnalysisContext getAnalysisContext() {
        return currentContext;
    }

    /**
     * 构建包含上下文的Prompt
     */
    public String buildPrompt(String userInput) {
        StringBuilder promptBuilder = new StringBuilder();

        // 添加系统消息
        promptBuilder.append("System: 你是一个AI文件清理助手，帮助用户分析和清理文件。");
        promptBuilder.append("你需要理解用户的指令，并结合之前的分析结果给出建议。\n\n");

        // 如果有分析上下文，添加上下文信息
        if (currentContext != null) {
            promptBuilder.append("Context - 当前文件分析上下文:\n");
            promptBuilder.append("扫描路径: ").append(currentContext.getScanPath()).append("\n");
            promptBuilder.append("总文件数: ").append(currentContext.getTotalFiles()).append("\n");
            promptBuilder.append("总大小: ").append(currentContext.getTotalSize()).append("\n");

            if (currentContext.getAnalysisResult() != null) {
                AnalysisResult result = currentContext.getAnalysisResult();
                promptBuilder.append("分析摘要: \n");
                promptBuilder.append("- 放心删除的文件: ").append(result.getSafeFilesCount()).append(" 个\n");
                promptBuilder.append("- 需要确认的文件: ").append(result.getReviewFilesCount()).append(" 个\n");
            }
            promptBuilder.append("\n");
        }

        // 添加最近的对话历史
        promptBuilder.append("Recent conversation:\n");
        List<ChatMessage> recentHistory = getRecentHistory(5); // 只保留最近5条
        for (ChatMessage message : recentHistory) {
            promptBuilder.append(message.getRole().toUpperCase())
                      .append(": ")
                      .append(message.getContent())
                      .append("\n");
        }

        // 添加当前用户输入
        promptBuilder.append("USER: ").append(userInput).append("\n");
        promptBuilder.append("ASSISTANT: ");

        return promptBuilder.toString();
    }

    /**
     * 获取最近的对话历史
     */
    private List<ChatMessage> getRecentHistory(int count) {
        int size = history.size();
        int start = Math.max(0, size - count);
        return history.subList(start, size);
    }

    /**
     * 获取所有对话历史
     */
    public List<ChatMessage> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * 清除对话历史
     */
    public void clearHistory() {
        history.clear();
    }

    /**
     * 检查是否有活跃的分析上下文
     */
    public boolean hasActiveAnalysisContext() {
        return currentContext != null && currentContext.getAnalysisResult() != null;
    }

    /**
     * 获取最近的分析结果
     */
    public AnalysisResult getLastAnalysisResult() {
        return hasActiveAnalysisContext() ? currentContext.getAnalysisResult() : null;
    }

    /**
     * 获取会话统计信息
     */
    public SessionStats getSessionStats() {
        return new SessionStats(
            sessionId,
            history.size(),
            hasActiveAnalysisContext() ? currentContext.getTotalFiles() : 0,
            hasActiveAnalysisContext() ? currentContext.getTotalSize() : "0B"
        );
    }

    /**
     * 添加消息到历史记录
     */
    private void addToHistory(ChatMessage message) {
        history.add(message);

        // 限制历史记录大小
        while (history.size() > maxHistorySize) {
            history.remove(0);
        }
    }

    /**
     * 识别用户意图类型
     */
    public IntentType identifyIntent(String userInput) {
        String lowerInput = userInput.toLowerCase();

        // 清理相关的关键词
        if (containsAny(lowerInput, "清理", "删除", "清理", "删除", "clean", "delete", "remove")) {
            if (containsAny(lowerInput, "扫描", "分析", "查看", "scan", "analyze", "show")) {
                return IntentType.SCAN_ANALYZE;
            } else if (hasActiveAnalysisContext()) {
                return IntentType.CLEANUP_INSTRUCTION;
            } else {
                return IntentType.CLEANUP_REQUEST;
            }
        }

        // 查看相关的关键词
        if (containsAny(lowerInput, "查看", "显示", "列出", "show", "list", "display")) {
            return IntentType.SCAN_ANALYZE;
        }

        // 帮助相关的关键词
        if (containsAny(lowerInput, "帮助", "help", "怎么用", "使用说明")) {
            return IntentType.HELP;
        }

        return IntentType.GENERAL_CHAT;
    }

    /**
     * 检查字符串是否包含任意关键词
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 会话统计信息
     */
    public static class SessionStats {
        private final String sessionId;
        private final int messageCount;
        private final int lastAnalysisFileCount;
        private final String lastAnalysisSize;

        public SessionStats(String sessionId, int messageCount, int lastAnalysisFileCount, String lastAnalysisSize) {
            this.sessionId = sessionId;
            this.messageCount = messageCount;
            this.lastAnalysisFileCount = lastAnalysisFileCount;
            this.lastAnalysisSize = lastAnalysisSize;
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public int getMessageCount() { return messageCount; }
        public int getLastAnalysisFileCount() { return lastAnalysisFileCount; }
        public String getLastAnalysisSize() { return lastAnalysisSize; }
    }

    /**
     * 意图类型枚举
     */
    public enum IntentType {
        SCAN_ANALYZE,         // 扫描和分析
        CLEANUP_REQUEST,      // 清理请求
        CLEANUP_INSTRUCTION,  // 清理指令
        GENERAL_CHAT,         // 一般对话
        HELP                  // 帮助
    }

    /**
     * 文件分析上下文
     */
    public static class FileAnalysisContext {
        private final String scanPath;
        private final List<FileInfo> allFiles;
        private final AnalysisResult analysisResult;
        private final long timestamp;

        public FileAnalysisContext(String scanPath, List<FileInfo> allFiles, AnalysisResult analysisResult) {
            this.scanPath = scanPath;
            this.allFiles = new ArrayList<>(allFiles);
            this.analysisResult = analysisResult;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public String getScanPath() { return scanPath; }
        public List<FileInfo> getAllFiles() { return new ArrayList<>(allFiles); }
        public AnalysisResult getAnalysisResult() { return analysisResult; }
        public long getTimestamp() { return timestamp; }

        public int getTotalFiles() { return allFiles.size(); }

        public String getTotalSize() {
            long total = allFiles.stream().mapToLong(FileInfo::getSize).sum();
            return formatFileSize(total);
        }

        public String getFormattedTimestamp() {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault()
            );
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        private String formatFileSize(long size) {
            if (size < 1024) return size + "B";
            if (size < 1024 * 1024) return String.format("%.1fKB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.1fMB", size / (1024.0 * 1024.0));
            return String.format("%.1fGB", size / (1024.0 * 1024.0 * 1024.0));
        }

        public boolean isExpired(long maxAgeMs) {
            return System.currentTimeMillis() - timestamp > maxAgeMs;
        }
    }

    /**
     * 导出会话历史
     */
    public String exportHistory() {
        StringBuilder export = new StringBuilder();
        export.append("# AI清理助手会话历史\n");
        export.append("会话ID: ").append(sessionId).append("\n");
        export.append("导出时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");

        for (ChatMessage message : history) {
            export.append("## ").append(message.getRole().toUpperCase()).append("\n");
            export.append("时间: ").append(message.getFormattedTimestamp()).append("\n");
            export.append("内容: ").append(message.getContent()).append("\n\n");
        }

        return export.toString();
    }
}