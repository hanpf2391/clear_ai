package com.hanpf.clearai.react.prompt;

import com.hanpf.clearai.react.state.ConversationState;
import com.hanpf.clearai.react.tools.ToolDefinition;

import java.util.List;
import java.util.Set;

/**
 * 动态Prompt构建器 - 基于对话状态和上下文智能构建Prompt
 *
 * 核心特性：
 * 1. 上下文注入：动态注入对话历史、工具调用记录、当前状态
 * 2. 工具描述集成：自动生成可用工具的描述和使用说明
 * 3. 状态感知：根据当前循环次数和已有调用调整提示
 * 4. 长度控制：智能控制Prompt长度，避免超出模型限制
 * 5. 格式化输出：生成结构化的JSON决策格式要求
 */
public class DynamicPromptBuilder {

    // Prompt模板
    private static final String SYSTEM_PROMPT_TEMPLATE = """
        你是一个名为 CLEAR AI 的顶级智能电脑清理助手，基于 ReAct（Reasoning-Action）模式工作。

        核心工作原理：
        1. **思考 (Reasoning)**：分析用户需求，制定执行计划
        2. **行动 (Action)**：调用工具执行具体操作
        3. **观察 (Observation)**：根据工具结果调整策略
        4. **循环执行**：直到任务完成，给出最终答案

        可用工具：
        %s

        输出格式要求：
        请严格按照以下JSON格式返回决策：

        {
          "thought": "你的思考过程，解释为什么需要执行这个行动或给出这个答案",
          "action": {
            "tool_name": "工具名称",
            "parameters": {
              "参数名": "参数值"
            }
          }
        }

        或者如果是最终答案：

        {
          "thought": "你的思考过程，说明为什么任务已经完成",
          "final_answer": "给用户的最终回答"
        }

        重要规则：
        1. 【沟通优先原则】在获得重要发现或执行耗时操作前后，优先使用 send_intermediate_response 向用户汇报进展和下一步计划
        2. 每次只能调用一个工具
        3. 如果需要更多信息，调用相应的扫描工具
        4. 只有在任务完全完成时才使用 final_answer
        5. 保持专业、友好的语气
        6. 优先使用扫描工具了解情况，再执行清理操作
        7. 确保所有操作都有明确目的
        8. 【分步汇报】复杂任务应拆分为多个阶段，每阶段结束后与用户沟通确认
        """;

    private static final String CONTEXT_INJECTION_TEMPLATE = """

        === 对话上下文 ===
        当前状态：%s
        当前循环：%d

        === 对话历史 ===
        %s

        === 工具调用历史 ===
        %s

        === 当前用户请求 ===
        %s

        === 请基于以上上下文做出决策 ===
        """;

    private static final int MAX_PROMPT_LENGTH = 8000; // 最大Prompt长度限制

    /**
     * 构建完整的Prompt
     * @param state 对话状态
     * @param availableTools 可用工具列表
     * @return 构建好的Prompt
     */
    public String buildPrompt(ConversationState state, List<ToolDefinition> availableTools) {
        // 1. 构建系统提示（包含工具描述）
        String systemPrompt = buildSystemPrompt(availableTools);

        // 2. 构建上下文注入部分
        String contextInjection = buildContextInjection(state);

        // 3. 组合并控制长度
        String fullPrompt = systemPrompt + contextInjection;

        return controlPromptLength(fullPrompt);
    }

    /**
     * 构建系统提示（包含工具描述）
     */
    private String buildSystemPrompt(List<ToolDefinition> tools) {
        StringBuilder toolsDescription = new StringBuilder();

        for (ToolDefinition tool : tools) {
            toolsDescription.append(String.format("- **%s**: %s\n",
                tool.getName(), tool.getDescription()));

            if (!tool.getParameters().isEmpty()) {
                toolsDescription.append("  参数要求：\n");
                for (ToolDefinition.Parameter param : tool.getParameters()) {
                    String required = param.isRequired() ? "（必需）" : "（可选）";
                    toolsDescription.append(String.format("  - %s: %s %s\n",
                        param.getName(), param.getDescription(), required));
                }
            }
            toolsDescription.append("\n");
        }

        return String.format(SYSTEM_PROMPT_TEMPLATE, toolsDescription.toString());
    }

    /**
     * 构建上下文注入部分
     */
    private String buildContextInjection(ConversationState state) {
        String currentStateInfo = state.getConversationSummary();
        int currentLoop = state.getCurrentLoop();

        // 获取格式化的对话历史
        String conversationHistory = state.getFormattedHistory();

        // 构建工具调用历史摘要
        String toolCallHistory = buildToolCallHistory(state);

        // 获取最新的用户消息
        String latestUserMessage = "";
        List<String> userMessages = state.getUserMessages();
        if (!userMessages.isEmpty()) {
            latestUserMessage = userMessages.get(userMessages.size() - 1);
        }

        return String.format(CONTEXT_INJECTION_TEMPLATE,
            currentStateInfo, currentLoop, conversationHistory, toolCallHistory, latestUserMessage);
    }

    /**
     * 构建工具调用历史摘要
     */
    private String buildToolCallHistory(ConversationState state) {
        StringBuilder history = new StringBuilder();

        if (state.getToolResults().isEmpty()) {
            history.append("尚未调用任何工具\n");
        } else {
            for (String toolName : state.getToolResults().keySet()) {
                List<String> results = state.getToolHistory(toolName);
                history.append(String.format("- %s: 调用 %d 次\n", toolName, results.size()));

                // 只显示最后一次调用结果的摘要
                if (!results.isEmpty()) {
                    String lastResult = results.get(results.size() - 1);
                    String summary = lastResult.length() > 200 ?
                        lastResult.substring(0, 200) + "..." : lastResult;
                    history.append("  最近结果: ").append(summary).append("\n");
                }
            }
        }

        return history.toString();
    }

    /**
     * 控制Prompt长度
     */
    private String controlPromptLength(String prompt) {
        if (prompt.length() <= MAX_PROMPT_LENGTH) {
            return prompt;
        }

        // 如果Prompt过长，优先截断对话历史部分
        int systemPromptEnd = prompt.indexOf("=== 对话上下文 ===");
        if (systemPromptEnd == -1) {
            return prompt.substring(0, MAX_PROMPT_LENGTH - 100) + "\n...[内容过长已截断]";
        }

        String systemPrompt = prompt.substring(0, systemPromptEnd);
        String contextSection = prompt.substring(systemPromptEnd);

        int maxContextLength = MAX_PROMPT_LENGTH - systemPrompt.length() - 200;
        if (contextSection.length() > maxContextLength) {
            // 从后往前截断，保留最新的上下文
            String truncatedContext = contextSection.substring(
                Math.max(0, contextSection.length() - maxContextLength));

            // 确保从新行开始
            int firstNewline = truncatedContext.indexOf('\n');
            if (firstNewline != -1) {
                truncatedContext = truncatedContext.substring(firstNewline + 1);
            }

            contextSection = "...[历史对话已截断]\n" + truncatedContext;
        }

        return systemPrompt + contextSection;
    }

    /**
     * 构建初始欢迎Prompt
     */
    public String buildWelcomePrompt() {
        return """
            你是一个名为 CLEAR AI 的智能电脑清理助手。

            请以友好、专业的方式回应用户的清理需求。你可以：
            1. 分析电脑存储情况
            2. 识别垃圾文件和临时文件
            3. 提供清理建议和操作
            4. 回答电脑维护相关问题

            请直接用自然语言回复，不需要JSON格式。
            """;
    }
}