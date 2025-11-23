package com.hanpf.clearai.react.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.hanpf.clearai.service.ChatService;
import com.hanpf.clearai.config.AIConfig;
import com.hanpf.clearai.react.state.ConversationState;
import com.hanpf.clearai.react.state.StateManager;
import com.hanpf.clearai.react.prompt.DynamicPromptBuilder;
import com.hanpf.clearai.react.tools.ToolRegistry;
import com.hanpf.clearai.utils.ClearAILogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 用户确认所需异常 - 用于暂停ReAct循环等待用户输入
 */
class UserConfirmationRequiredException extends Exception {
    public UserConfirmationRequiredException(String message) {
        super(message);
    }
}

/**
 * ReAct代理执行器 - 对话状态机驱动的循环式智能核心
 *
 * 核心架构特点：
 * 1. 对话状态机驱动：维护对话历史、工具调用记录、当前执行状态
 * 2. 动态Prompt构建：每次循环都根据当前状态构建包含上下文的Prompt
 * 3. 自主决策循环：LLM自主决定何时调用工具、何时给出最终答案
 * 4. 结构化决策：LLM返回结构化JSON，包含thought、action、final_answer
 * 5. 工具调用管理：通过注解系统自动发现和调用Java工具
 * 6. 异常处理与恢复：具备完善的错误处理和状态恢复机制
 */
public class ReActAgentExecutor {

    private final ObjectMapper objectMapper;
    private final StateManager stateManager;
    private final DynamicPromptBuilder promptBuilder;
    private final ToolRegistry toolRegistry;
    private final ChatService aiService;

    // 执行配置
    private static final int MAX_REACT_LOOPS = 20; // 最大循环次数，防止无限循环
    private static final int AI_TIMEOUT_SECONDS = 30; // AI调用超时时间
    private static final boolean ENABLE_DEBUG_LOGGING = true; // 调试日志开关

    public ReActAgentExecutor() {
        this.objectMapper = new ObjectMapper();
        this.stateManager = new StateManager();
        this.promptBuilder = new DynamicPromptBuilder();
        this.toolRegistry = new ToolRegistry();
        this.aiService = AIConfig.createChatService();

        // 初始化工具注册表
        toolRegistry.discoverAndRegisterTools();

        if (ENABLE_DEBUG_LOGGING) {
            ClearAILogger.info("ReAct代理执行器初始化完成，已注册工具数量: " + toolRegistry.getToolCount());
        }
    }

    /**
     * 统一的对话入口 - 处理所有用户输入
     * @param userInput 用户输入
     * @return AI响应结果
     */
    public String processInput(String userInput) {
        try {
            // 创建新的对话状态或继续现有对话
            ConversationState state = stateManager.getCurrentState();
            state.addUserMessage(userInput);

            // 执行ReAct循环
            String result = executeReActLoop(userInput, state);

            // 更新状态管理器
            stateManager.updateState(state);

            return result;

        } catch (Exception e) {
            ClearAILogger.error("ReAct执行器处理输入时出错: " + e.getMessage(), e);
            return "❌ 处理请求时出错：" + e.getMessage();
        }
    }

    /**
     * ReAct核心循环 - 状态机驱动的自主决策
     */
    private String executeReActLoop(String userInput, ConversationState state) throws Exception {
        if (ENABLE_DEBUG_LOGGING) {
            ClearAILogger.info("开始ReAct循环，用户输入: " + userInput);
        }

        for (int loop = 0; loop < MAX_REACT_LOOPS; loop++) {
            if (ENABLE_DEBUG_LOGGING) {
                ClearAILogger.info("ReAct循环 #" + (loop + 1));
            }

            // 1. 动态构建包含完整上下文的Prompt
            String prompt = promptBuilder.buildPrompt(state, toolRegistry.getAvailableTools());

            if (ENABLE_DEBUG_LOGGING) {
                ClearAILogger.info("构建的Prompt长度: " + prompt.length() + " 字符");
            }

            // 2. 调用LLM获取决策
            String aiDecision = callAIWithTimeout(prompt);

            if (ENABLE_DEBUG_LOGGING) {
                ClearAILogger.info("AI决策原始输出: " + aiDecision);
            }

            // 3. 解析结构化决策
            ReActDecision decision = parseDecision(aiDecision);
            state.addDecision(decision);

            // 4. 执行决策
            if (decision.isFinalAnswer()) {
                // 最终答案，结束循环
                if (ENABLE_DEBUG_LOGGING) {
                    ClearAILogger.info("LLM给出最终答案，结束ReAct循环");
                }
                return decision.getFinalAnswer();
            } else if (decision.hasAction()) {
                // 执行工具调用
                try {
                    String toolResult = executeToolAction(decision.getAction());
                    state.addToolResult(decision.getAction().getToolName(), toolResult);

                    if (ENABLE_DEBUG_LOGGING) {
                        ClearAILogger.info("工具执行完成，结果长度: " + toolResult.length() + " 字符");
                    }
                } catch (UserConfirmationRequiredException e) {
                    // 用户确认工具被调用，需要暂停并返回当前状态
                    if (ENABLE_DEBUG_LOGGING) {
                        ClearAILogger.info("用户确认工具调用，暂停ReAct循环等待用户输入");
                    }
                    return "🔄 等待用户确认，请继续对话...";
                }
            } else {
                // 无效决策
                String errorMsg = "❌ AI给出了无效的决策格式";
                ClearAILogger.error(errorMsg);
                return errorMsg;
            }
        }

        // 达到最大循环次数
        String timeoutMsg = "⏰ 任务执行超时，已达到最大循环次数限制";
        ClearAILogger.warn(timeoutMsg);
        return timeoutMsg;
    }

    /**
     * 带超时的AI调用
     */
    private String callAIWithTimeout(String prompt) throws Exception {
        CompletableFuture<String> aiCall = CompletableFuture.supplyAsync(() -> {
            try {
                return aiService.chat(prompt);
            } catch (Exception e) {
                throw new RuntimeException("AI调用失败", e);
            }
        });

        try {
            return aiCall.get(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            aiCall.cancel(true);
            throw new Exception("AI调用超时或失败: " + e.getMessage());
        }
    }

    /**
     * 解析AI返回的结构化决策
     */
    private ReActDecision parseDecision(String aiResponse) throws Exception {
        ReActDecision decision = new ReActDecision();

        try {
            // 清理响应，提取JSON部分
            String jsonStr = extractJsonFromResponse(aiResponse);

            if (jsonStr.isEmpty()) {
                // JSON无效，将响应作为最终答案处理
                decision.setFinalAnswer(aiResponse.trim());
                return decision;
            }

            JsonNode json = objectMapper.readTree(jsonStr);

            // 解析思考过程
            if (json.has("thought")) {
                decision.setThought(json.get("thought").asText());
            }

            // 解析最终答案
            if (json.has("final_answer")) {
                decision.setFinalAnswer(json.get("final_answer").asText());
                return decision;
            }

            // 解析工具调用
            if (json.has("action")) {
                JsonNode actionNode = json.get("action");
                ReActAction action = new ReActAction();
                action.setToolName(actionNode.get("tool_name").asText());

                if (actionNode.has("parameters")) {
                    action.setParameters(actionNode.get("parameters"));
                }

                decision.setAction(action);
            }

            return decision;

        } catch (Exception e) {
            // JSON解析失败时，将响应作为最终答案处理
            ClearAILogger.warn("JSON解析失败，使用文本响应: " + e.getMessage());
            decision.setFinalAnswer(aiResponse.trim());
            return decision;
        }
    }

    /**
     * 从AI响应中提取JSON部分
     */
    private String extractJsonFromResponse(String response) {
        try {
            // 寻找JSON开始和结束标记
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}");

            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = response.substring(jsonStart, jsonEnd + 1);

                // 修复常见的JSON转义问题，特别是Windows路径
                jsonStr = fixCommonJsonIssues(jsonStr);

                // 验证是否是有效的JSON
                ObjectMapper validator = new ObjectMapper();
                validator.readTree(jsonStr);
                return jsonStr;
            }

            throw new IllegalArgumentException("响应中未找到有效的JSON格式");

        } catch (Exception e) {
            // JSON格式错误时返回空字符串，让后续处理使用文本响应
            ClearAILogger.error("JSON提取和验证失败: " + e.getMessage());
            return "";
        }
    }

    /**
     * 修复常见的JSON格式问题
     */
    private String fixCommonJsonIssues(String jsonStr) {
        // 修复Windows路径中的反斜杠问题 - 将单反斜杠转为双反斜杠
        // 但要小心不要破坏JSON中已有的转义序列
        jsonStr = jsonStr.replaceAll("(?<!\\\\)\\\\(?!['\"\\\\bfnrt/])", "\\\\\\\\");

        // 修复其他常见问题
        jsonStr = jsonStr.replaceAll("\"tool_name\":\\s*\"([^\"]+)\"", "\"tool_name\":\"$1\"");
        jsonStr = jsonStr.replaceAll("\"final_answer\":\\s*\"([^\"]+)\"", "\"final_answer\":\"$1\"");

        return jsonStr;
    }

    /**
     * 执行工具调用
     */
    private String executeToolAction(ReActAction action) throws Exception {
        String toolName = action.getToolName();

        if (!toolRegistry.hasTool(toolName)) {
            return "❌ 未找到工具: " + toolName;
        }

        try {
            String result = toolRegistry.executeTool(toolName, action.getParameters());

            // 检查是否是通信工具，如果是则特殊处理
            if (isCommunicationTool(toolName)) {
                boolean shouldPause = handleCommunicationTool(toolName, action.getParameters(), result);
                if (shouldPause) {
                    // 对于需要用户确认的工具，返回特殊标记让ReAct循环暂停
                    throw new UserConfirmationRequiredException("等待用户确认");
                }
            }

            return result;
        } catch (UserConfirmationRequiredException e) {
            // 重新抛出用户确认异常
            throw e;
        } catch (Exception e) {
            String errorMsg = "工具执行失败: " + e.getMessage();
            ClearAILogger.error(errorMsg, e);
            return errorMsg;
        }
    }

    /**
     * 检查是否是通信工具
     */
    private boolean isCommunicationTool(String toolName) {
        return toolName.equals("send_intermediate_response") ||
               toolName.equals("request_user_confirmation") ||
               toolName.equals("report_progress") ||
               toolName.equals("highlight_finding");
    }

    /**
     * 处理通信工具 - 直接向用户显示消息
     * @return 是否需要暂停ReAct循环等待用户输入
     */
    private boolean handleCommunicationTool(String toolName, JsonNode parameters, String toolResult) {
        try {
            String message = "";

            switch (toolName) {
                case "send_intermediate_response":
                    if (parameters != null && parameters.has("message")) {
                        message = parameters.get("message").asText();
                        System.out.println("\n📢 " + message);
                        System.out.flush();
                    }
                    return false; // 不需要暂停

                case "request_user_confirmation":
                    if (parameters != null && parameters.has("question")) {
                        String question = parameters.get("question").asText();
                        message = "❓ 请确认: " + question;

                        if (parameters.has("options")) {
                            String options = parameters.get("options").asText();
                            message += "\n\n可选项:\n";
                            String[] optionArray = options.split(",");
                            for (int i = 0; i < optionArray.length; i++) {
                                message += String.format("%d. %s\n", i + 1, optionArray[i].trim());
                            }
                        }

                        System.out.println("\n" + message);
                        System.out.print("👤 您的回复: ");
                        System.out.flush();
                    }
                    return true; // 需要暂停等待用户输入

                case "report_progress":
                    if (parameters != null) {
                        StringBuilder progressMsg = new StringBuilder();

                        String currentStep = parameters.has("current_step") ?
                            parameters.get("current_step").asText() : "未知";
                        String totalSteps = parameters.has("total_steps") ?
                            parameters.get("total_steps").asText() : "未知";
                        String details = parameters.has("details") ?
                            parameters.get("details").asText() : "";

                        // 构建进度显示
                        if (!"未知".equals(totalSteps)) {
                            try {
                                int current = Integer.parseInt(currentStep);
                                int total = Integer.parseInt(totalSteps);
                                int percentage = (current * 100) / total;

                                progressMsg.append(String.format("⏳ 进度: [%s] %d%% (%d/%d)\n",
                                    "=".repeat(Math.max(0, percentage / 10)), percentage, current, total));
                            } catch (NumberFormatException e) {
                                progressMsg.append(String.format("⏳ 步骤: %s / %s\n", currentStep, totalSteps));
                            }
                        } else {
                            progressMsg.append(String.format("⏳ 当前步骤: %s\n", currentStep));
                        }

                        progressMsg.append(String.format("📍 %s", currentStep));

                        if (!details.trim().isEmpty()) {
                            progressMsg.append(String.format("\n📝 %s", details));
                        }

                        System.out.println("\n" + progressMsg.toString());
                        System.out.flush();
                    }
                    return false; // 不需要暂停

                case "highlight_finding":
                    if (parameters != null && parameters.has("finding")) {
                        String finding = parameters.get("finding").asText();
                        String impact = parameters.has("impact") ?
                            parameters.get("impact").asText() : "";
                        String suggestion = parameters.has("suggestion") ?
                            parameters.get("suggestion").asText() : "";

                        StringBuilder highlightMsg = new StringBuilder();
                        highlightMsg.append("⚠️ 重要发现:\n");
                        highlightMsg.append(String.format("🔍 %s\n", finding));

                        if (!impact.trim().isEmpty()) {
                            highlightMsg.append(String.format("💡 影响: %s\n", impact));
                        }

                        if (!suggestion.trim().isEmpty()) {
                            highlightMsg.append(String.format("💭 建议: %s", suggestion));
                        }

                        System.out.println("\n" + highlightMsg.toString());
                        // 刷新输出流确保消息立即显示
                        System.out.flush();
                    }
                    return false; // 不需要暂停

                default:
                    // 默认情况下直接显示工具结果
                    System.out.println("\n📢 " + toolResult);
                    System.out.flush();
                    return false; // 默认不需要暂停
            }
        } catch (Exception e) {
            ClearAILogger.error("处理通信工具时出错: " + e.getMessage(), e);
            // 即使出错也要显示基本消息
            System.out.println("\n📢 " + toolResult);
            return false; // 出错时不需要暂停
        }
    }

    /**
     * 重置对话状态
     */
    public void resetConversation() {
        stateManager.reset();
        if (ENABLE_DEBUG_LOGGING) {
            ClearAILogger.info("ReAct代理对话状态已重置");
        }
    }

    /**
     * 获取当前对话历史
     */
    public List<String> getConversationHistory() {
        return new ArrayList<>(stateManager.getCurrentState().getConversationHistory());
    }
}