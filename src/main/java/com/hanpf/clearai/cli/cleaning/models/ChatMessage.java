package com.hanpf.clearai.cli.cleaning.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 聊天消息数据结构
 */
public class ChatMessage {
    private String role;
    private String content;
    private LocalDateTime timestamp;
    private String type; // "user", "assistant", "system", "analysis"

    public ChatMessage(String role, String content, String type) {
        this.role = role;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(String role, String content) {
        this(role, content, "message");
    }

    // Getters and setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s",
            getFormattedTimestamp(),
            role.toUpperCase(),
            content.length() > 50 ? content.substring(0, 50) + "..." : content);
    }
}