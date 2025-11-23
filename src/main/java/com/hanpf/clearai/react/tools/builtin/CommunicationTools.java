package com.hanpf.clearai.react.tools.builtin;

import com.hanpf.clearai.react.tools.ReActTool;
import com.hanpf.clearai.react.tools.ToolParam;
import com.hanpf.clearai.utils.ClearAILogger;

/**
 * 通信工具集 - 提供与用户交互的专用工具
 */
public class CommunicationTools {

    /**
     * 发送阶段性报告给用户 - 在重要发现时使用
     */
    @ReActTool(
        name = "send_intermediate_response",
        description = "向用户发送非最终性的状态更新或阶段性报告。用于在关键发现时保持用户互动，让用户了解当前进展和下一步计划。",
        category = "communication"
    )
    public String sendIntermediateResponse(
        @ToolParam(name = "message", description = "要发送给用户的消息内容", required = true) String message
    ) {
        try {
            // 这个方法会被ReAct执行器特殊处理
            // 实际的打印逻辑在ReActAgentExecutor中处理
            // 返回一个标准响应，让AI知道通信已完成
            return "消息已发送给用户";
        } catch (Exception e) {
            ClearAILogger.error("发送中间响应失败", e);
            return "发送消息时出错: " + e.getMessage();
        }
    }

    /**
     * 请求用户确认 - 在需要用户决策时使用
     */
    @ReActTool(
        name = "request_user_confirmation",
        description = "向用户请求确认或选择，用于需要用户决策的场景。AI应该暂停执行，等待用户回应。",
        category = "communication"
    )
    public String requestUserConfirmation(
        @ToolParam(name = "question", description = "需要用户确认的问题", required = true) String question,
        @ToolParam(name = "options", description = "可选项列表，格式如'选项1,选项2,选项3'", required = false) String options
    ) {
        try {
            // 构建确认信息
            StringBuilder confirmationMessage = new StringBuilder();
            confirmationMessage.append("❓ 请确认:\n");
            confirmationMessage.append(question);

            if (options != null && !options.trim().isEmpty()) {
                confirmationMessage.append("\n\n可选项:\n");
                String[] optionArray = options.split(",");
                for (int i = 0; i < optionArray.length; i++) {
                    confirmationMessage.append(String.format("%d. %s\n", i + 1, optionArray[i].trim()));
                }
                confirmationMessage.append("\n请输入选项编号或直接回复:");
            }

            // 同样，实际处理在ReActAgentExecutor中
            return "需要用户确认: " + question;
        } catch (Exception e) {
            ClearAILogger.error("请求用户确认失败", e);
            return "请求确认时出错: " + e.getMessage();
        }
    }

    /**
     * 报告进度 - 让用户知道工作进展
     */
    @ReActTool(
        name = "report_progress",
        description = "向用户报告当前工作进度，保持用户对执行过程的感知。",
        category = "communication"
    )
    public String reportProgress(
        @ToolParam(name = "current_step", description = "当前执行的步骤", required = true) String currentStep,
        @ToolParam(name = "total_steps", description = "总步骤数", required = false, defaultValue = "未知") String totalSteps,
        @ToolParam(name = "details", description = "步骤详情", required = false) String details
    ) {
        try {
            StringBuilder progressMessage = new StringBuilder();

            // 添加进度条
            if (!"未知".equals(totalSteps)) {
                try {
                    int current = Integer.parseInt(currentStep);
                    int total = Integer.parseInt(totalSteps);
                    int percentage = (current * 100) / total;

                    progressMessage.append(String.format("⏳ 进度: [%s] %d%% (%d/%d)\n",
                        "=".repeat(percentage / 10), percentage, current, total));
                } catch (NumberFormatException e) {
                    progressMessage.append(String.format("⏳ 步骤: %s / %s\n", currentStep, totalSteps));
                }
            } else {
                progressMessage.append(String.format("⏳ 当前步骤: %s\n", currentStep));
            }

            progressMessage.append(String.format("📍 当前操作: %s", currentStep));

            if (details != null && !details.trim().isEmpty()) {
                progressMessage.append(String.format("\n📝 详情: %s", details));
            }

            // 实际处理在ReActAgentExecutor中
            return "进度报告完成";
        } catch (Exception e) {
            ClearAILogger.error("报告进度失败", e);
            return "报告进度时出错: " + e.getMessage();
        }
    }

    /**
     * 显示重要发现 - 突出显示关键信息
     */
    @ReActTool(
        name = "highlight_finding",
        description = "向用户突出显示重要发现或关键信息，用于强调需要用户注意的内容。",
        category = "communication"
    )
    public String highlightFinding(
        @ToolParam(name = "finding", description = "重要发现的内容", required = true) String finding,
        @ToolParam(name = "impact", description = "影响程度说明", required = false) String impact,
        @ToolParam(name = "suggestion", description = "建议的后续操作", required = false) String suggestion
    ) {
        try {
            StringBuilder highlightMessage = new StringBuilder();

            // 使用特殊符号突出显示
            highlightMessage.append("⚠️ 重要发现:\n");
            highlightMessage.append(String.format("🔍 %s\n", finding));

            if (impact != null && !impact.trim().isEmpty()) {
                highlightMessage.append(String.format("💡 影响: %s\n", impact));
            }

            if (suggestion != null && !suggestion.trim().isEmpty()) {
                highlightMessage.append(String.format("💭 建议: %s", suggestion));
            }

            // 实际处理在ReActAgentExecutor中
            return "重要发现已突出显示";
        } catch (Exception e) {
            ClearAILogger.error("突出显示发现失败", e);
            return "显示重要发现时出错: " + e.getMessage();
        }
    }
}