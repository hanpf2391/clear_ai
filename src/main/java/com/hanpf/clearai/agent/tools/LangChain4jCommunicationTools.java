package com.hanpf.clearai.agent.tools;

import dev.langchain4j.agent.tool.Tool;

/**
 * 基于LangChain4j的通信工具集
 */
public class LangChain4jCommunicationTools {

    /**
     * 发送中间响应
     */
    @Tool("向用户发送非最终性的状态更新或阶段性报告")
    public String sendIntermediateResponse(String message) {
        return "📢 " + message;
    }

    /**
     * 请求用户确认
     */
    @Tool("向用户请求确认或选择，用于需要用户决策的场景")
    public String requestUserConfirmation(String question) {
        return "❓ 需要用户确认: " + question;
    }

    /**
     * 报告进度
     */
    @Tool("向用户报告当前工作进度")
    public String reportProgress(String currentStep, String totalSteps, String details) {
        if (details == null || details.isEmpty()) {
            return "📊 进度: " + currentStep + "/" + totalSteps;
        }
        return "📊 进度: " + currentStep + "/" + totalSteps + " - " + details;
    }

    /**
     * 突出显示重要发现
     */
    @Tool("向用户突出显示重要发现或关键信息")
    public String highlightFinding(String finding, String impact, String suggestion) {
        StringBuilder highlight = new StringBuilder();
        highlight.append("🔍 重要发现: ").append(finding).append("\n");

        if (impact != null && !impact.isEmpty()) {
            highlight.append("📈 影响: ").append(impact).append("\n");
        }

        if (suggestion != null && !suggestion.isEmpty()) {
            highlight.append("💡 建议: ").append(suggestion);
        }

        return highlight.toString();
    }
}