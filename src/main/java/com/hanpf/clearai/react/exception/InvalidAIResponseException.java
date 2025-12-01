package com.hanpf.clearai.react.exception;

/**
 * AI响应格式异常
 *
 * 当AI返回的响应不符合标准的ReAct JSON格式时抛出此异常。
 * 这是为了确保parseDecision方法只处理严格符合规范的响应，
 * 避免"过度容错"导致的系统混乱。
 */
public class InvalidAIResponseException extends Exception {

    private final String aiResponse;
    private final String reason;

    public InvalidAIResponseException(String message, String aiResponse, String reason) {
        super(message);
        this.aiResponse = aiResponse;
        this.reason = reason;
    }

    public InvalidAIResponseException(String message, String aiResponse) {
        this(message, aiResponse, "未知错误");
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return String.format("InvalidAIResponseException: %s\n原因: %s\nAI响应: %s",
            getMessage(), reason, aiResponse);
    }
}