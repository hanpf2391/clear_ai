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
import com.hanpf.clearai.react.ui.ReActProgressDisplay;
import com.hanpf.clearai.react.exception.InvalidAIResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.io.PrintWriter;
import java.io.StringWriter;

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
    private final ReActProgressDisplay progressDisplay;

    // 执行配置
    private static final int MAX_REACT_LOOPS = 20; // 最大循环次数，防止无限循环
    private static final int AI_TIMEOUT_SECONDS = 120; // AI调用超时时间（增加到2分钟以支持复杂的AI驱动文件分析）
    private static final boolean ENABLE_DEBUG_LOGGING = true; // 调试日志开关
    private static final boolean ENABLE_PROGRESS_DISPLAY = true; // 进度显示开关

    public ReActAgentExecutor() {
        this.objectMapper = new ObjectMapper();
        this.stateManager = new StateManager();
        this.promptBuilder = new DynamicPromptBuilder();
        this.toolRegistry = new ToolRegistry();
        this.aiService = AIConfig.createChatService();

        // 初始化进度显示器 - 使用简洁模式（关闭详细进度显示）
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter, true);
        this.progressDisplay = new ReActProgressDisplay(printWriter, false); // 关闭详细显示

        // 注意：工具已在ToolRegistry构造函数中注册，无需重复注册
        // toolRegistry.discoverAndRegisterTools(); // 移除重复调用

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

        // 任务完成检测 - 只有用户明确表示完成时才停止
        if (isTaskCompleted(state)) {
            if (ENABLE_DEBUG_LOGGING) {
                ClearAILogger.info("检测到用户明确的完成信号，结束ReAct循环");
            }
            return "✅ 对话结束。感谢您使用CLEAR AI！";
        }

        // 计算实际需要的最大循环次数
        int maxLoops = calculateMaxLoops(userInput, state);

        for (int loop = 0; loop < maxLoops; loop++) {
            if (ENABLE_PROGRESS_DISPLAY) {
                progressDisplay.startLoop(loop + 1, MAX_REACT_LOOPS);
            }

            if (ENABLE_DEBUG_LOGGING) {
                ClearAILogger.info("ReAct循环 #" + (loop + 1));
            }

            // 1. 动态构建包含完整上下文的Prompt
            if (ENABLE_PROGRESS_DISPLAY) {
                progressDisplay.startStep("PROMPT_BUILD", "🔨 构建包含上下文的Prompt...");
            }

            String prompt = promptBuilder.buildPrompt(state, toolRegistry.getAvailableTools());

            if (ENABLE_DEBUG_LOGGING) {
                ClearAILogger.info("构建的Prompt长度: " + prompt.length() + " 字符");
            }

            if (ENABLE_PROGRESS_DISPLAY) {
                progressDisplay.completeStep(String.format("Prompt长度: %d字符", prompt.length()));
            }

            // 2. 调用LLM获取决策
            if (ENABLE_PROGRESS_DISPLAY) {
                progressDisplay.startStep("AI_CALL", "🤖 调用AI获取决策...");
            }

            String aiDecision = callAIWithTimeout(prompt);

            if (ENABLE_DEBUG_LOGGING) {
                ClearAILogger.info("AI决策原始输出: " + aiDecision);
            }

            if (ENABLE_PROGRESS_DISPLAY) {
                String decisionPreview = aiDecision.length() > 150 ?
                    aiDecision.substring(0, 147) + "..." : aiDecision;
                progressDisplay.completeStep(String.format("获取到决策: %s", decisionPreview));
            }

            // 3. 解析结构化决策 - 严格模式
            if (ENABLE_PROGRESS_DISPLAY) {
                progressDisplay.startStep("PARSE_DECISION", "📋 解析AI决策...");
            }

            ReActDecision decision;
            try {
                decision = parseDecision(aiDecision);
                state.addDecision(decision);

                if (ENABLE_PROGRESS_DISPLAY) {
                    if (decision.getThought() != null) {
                        progressDisplay.showThinking(decision.getThought());
                    }
                    progressDisplay.completeStep(String.format("解析完成: %s",
                        decision.isFinalAnswer() ? "最终答案" :
                        decision.hasAction() ? "工具调用" : "无效决策"));
                }
            } catch (InvalidAIResponseException e) {
                // 处理AI响应格式错误
                ClearAILogger.error("AI响应格式错误: " + e.getReason());
                if (ENABLE_DEBUG_LOGGING) {
                    ClearAILogger.error("原始AI响应: " + e.getAiResponse());
                }

                // 重新构建一个纠正性的Prompt并重试
                String correctivePrompt = buildCorrectivePrompt(aiDecision, e);
                aiDecision = callAIWithTimeout(correctivePrompt);

                // 第二次尝试解析
                decision = parseDecision(aiDecision);
                state.addDecision(decision);

                if (ENABLE_PROGRESS_DISPLAY) {
                    progressDisplay.completeStep("纠正后解析成功");
                }
            }

            // 4. 执行决策
            if (decision.isFinalAnswer()) {
                // 最终答案，结束循环
                if (ENABLE_PROGRESS_DISPLAY) {
                    progressDisplay.showFinalAnswer(decision.getFinalAnswer());
                    progressDisplay.endLoop(decision.getFinalAnswer());
                }

                if (ENABLE_DEBUG_LOGGING) {
                    ClearAILogger.info("LLM给出最终答案，结束ReAct循环");
                }
                return decision.getFinalAnswer();
            } else if (decision.hasAction()) {
                // 执行工具调用
                if (ENABLE_PROGRESS_DISPLAY) {
                    progressDisplay.startStep("EXECUTE_ACTION", "⚡ 执行工具调用...");
                    progressDisplay.showToolCall(decision.getAction().getToolName(),
                        decision.getAction().getParameters());
                }

                try {
                    String toolResult = executeToolAction(decision.getAction());
                    state.addToolResult(decision.getAction().getToolName(), toolResult);

                    if (ENABLE_PROGRESS_DISPLAY) {
                        String resultPreview = toolResult.length() > 200 ?
                            toolResult.substring(0, 197) + "..." : toolResult;
                        progressDisplay.showToolResult(decision.getAction().getToolName(),
                            resultPreview, true);
                        progressDisplay.completeStep(String.format("工具执行成功: %s",
                            decision.getAction().getToolName()));
                    }

                    if (ENABLE_DEBUG_LOGGING) {
                        ClearAILogger.info("工具执行完成，结果长度: " + toolResult.length() + " 字符");
                    }
                } catch (UserConfirmationRequiredException e) {
                    // 用户确认工具被调用，需要暂停并返回当前状态
                    if (ENABLE_PROGRESS_DISPLAY) {
                        progressDisplay.showToolResult(decision.getAction().getToolName(),
                            "需要用户确认", false);
                        progressDisplay.completeStep("暂停等待用户确认");
                    }

                    if (ENABLE_DEBUG_LOGGING) {
                        ClearAILogger.info("用户确认工具调用，暂停ReAct循环等待用户输入");
                    }
                    return "🔄 等待用户确认，请继续对话...";
                } catch (Exception e) {
                    if (ENABLE_PROGRESS_DISPLAY) {
                        progressDisplay.showError("工具执行失败", e);
                    }
                    throw e;
                }
            } else {
                // 无效决策
                String errorMsg = "❌ AI给出了无效的决策格式";

                if (ENABLE_PROGRESS_DISPLAY) {
                    progressDisplay.showError(errorMsg, null);
                }

                ClearAILogger.error(errorMsg);
                return errorMsg;
            }

            if (ENABLE_PROGRESS_DISPLAY) {
                progressDisplay.showSeparator();
            }
        }

        // 达到最大循环次数
        String timeoutMsg = "⏰ 任务执行超时，已达到最大循环次数限制";

        if (ENABLE_PROGRESS_DISPLAY) {
            progressDisplay.showError(timeoutMsg, null);
        }

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
     * 解析AI返回的结构化决策 - 严格模式
     *
     * 只接受标准的ReAct JSON格式：{ "thought": "...", "action": {...} } 或 { "thought": "...", "final_answer": "..." }
     * 任何不符合此格式的响应都会抛出InvalidAIResponseException异常
     */
    private ReActDecision parseDecision(String aiResponse) throws Exception {
        if (ENABLE_DEBUG_LOGGING) {
            ClearAILogger.info("开始解析AI响应，长度: " + aiResponse.length());
        }

        try {
            // 提取JSON部分
            String jsonStr = extractJsonFromResponse(aiResponse);

            if (jsonStr.isEmpty()) {
                throw new InvalidAIResponseException(
                    "AI响应中未找到有效的JSON格式",
                    aiResponse,
                    "响应不包含有效的JSON结构"
                );
            }

            // 解析JSON
            JsonNode json = objectMapper.readTree(jsonStr);

            // 验证基本结构
            if (!json.has("thought")) {
                throw new InvalidAIResponseException(
                    "JSON缺少必需的thought字段",
                    jsonStr,
                    "标准ReAct格式必须包含thought字段"
                );
            }

            ReActDecision decision = new ReActDecision();
            decision.setThought(json.get("thought").asText());

            // 检查是否有最终答案
            if (json.has("final_answer")) {
                decision.setFinalAnswer(json.get("final_answer").asText());
                return decision;
            }

            // 检查是否有工具调用
            if (json.has("action")) {
                JsonNode actionNode = json.get("action");

                if (!actionNode.has("tool_name")) {
                    throw new InvalidAIResponseException(
                        "action缺少必需的tool_name字段",
                        jsonStr,
                        "工具调用必须包含tool_name字段"
                    );
                }

                ReActAction action = new ReActAction();
                action.setToolName(actionNode.get("tool_name").asText());

                if (actionNode.has("parameters")) {
                    action.setParameters(actionNode.get("parameters"));
                }

                decision.setAction(action);
                return decision;
            }

            // 既没有final_answer也没有action，格式不完整
            throw new InvalidAIResponseException(
                "JSON缺少必需的final_answer或action字段",
                jsonStr,
                "必须包含final_answer或action中的一个"
            );

        } catch (InvalidAIResponseException e) {
            // 重新抛出我们自己的异常
            throw e;
        } catch (Exception e) {
            // 其他异常包装为InvalidAIResponseException
            throw new InvalidAIResponseException(
                "JSON解析失败: " + e.getMessage(),
                aiResponse,
                "解析JSON时发生异常: " + e.getClass().getSimpleName()
            );
        }
    }

    /**
     * 构建纠正性Prompt - 当AI返回格式错误时使用
     */
    private String buildCorrectivePrompt(String invalidResponse, InvalidAIResponseException e) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("❌ **AI响应格式错误**\n\n");
        prompt.append("您的响应不符合标准的ReAct JSON格式。\n\n");
        prompt.append("**错误信息：**\n");
        prompt.append(e.getReason()).append("\n\n");
        prompt.append("**您的原始响应：**\n");
        prompt.append("```\n").append(invalidResponse).append("\n```\n\n");
        prompt.append("**正确格式要求：**\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"thought\": \"您的思考过程，解释为什么需要执行这个行动\",\n");
        prompt.append("  \"action\": {\n");
        prompt.append("    \"tool_name\": \"工具名称\",\n");
        prompt.append("    \"parameters\": {\n");
        prompt.append("      \"参数名\": \"参数值\"\n");
        prompt.append("    }\n");
        prompt.append("  }\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        prompt.append("或者，如果任务已完成：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"thought\": \"您的思考过程，说明为什么任务已经完成\",\n");
        prompt.append("  \"final_answer\": \"给用户的最终回答\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        prompt.append("请重新提供符合上述格式的响应。");

        return prompt.toString();
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
     * 执行工具调用 - 纯业务逻辑，不包含UI显示
     */
    private String executeToolAction(ReActAction action) throws Exception {
        String toolName = action.getToolName();

        if (!toolRegistry.hasTool(toolName)) {
            return "❌ 未找到工具: " + toolName;
        }

        try {
            String result = toolRegistry.executeTool(toolName, action.getParameters());

            // 检查是否是需要用户确认的工具
            if (requiresUserConfirmation(toolName, action.getParameters())) {
                // 返回结构化的用户确认信息，由UI层处理显示
                String structuredResult = buildStructuredConfirmationResult(toolName, action.getParameters(), result);
                throw new UserConfirmationRequiredException(structuredResult);
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
     * 检查工具是否需要用户确认
     */
    private boolean requiresUserConfirmation(String toolName, JsonNode parameters) {
        if (!toolName.equals("request_user_confirmation")) {
            return false;
        }

        // request_user_confirmation 工具总是需要暂停等待用户输入
        return true;
    }

    /**
     * 构建结构化的用户确认结果
     */
    private String buildStructuredConfirmationResult(String toolName, JsonNode parameters, String toolResult) {
        // 返回一个包含所有必要信息的结构化字符串
        // 格式: CONFIRMATION:question|options
        if (toolName.equals("request_user_confirmation") && parameters != null && parameters.has("question")) {
            String question = parameters.get("question").asText();
            String options = parameters.has("options") ? parameters.get("options").asText() : "";

            return String.format("CONFIRMATION:%s|%s", question, options);
        }

        return "CONFIRMATION:需要用户确认|";
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

    /**
     * 简化的任务完成检测
     * 只有在用户明确表示完成时才认为任务完成
     */
    private boolean isTaskCompleted(ConversationState state) {
        // 只有在用户明确表示完成时才认为完成
        List<String> userMessages = state.getUserMessages();
        if (!userMessages.isEmpty()) {
            String lastMessage = userMessages.get(userMessages.size() - 1);
            // 检查明确的完成信号
            if (isCompletionSignal(lastMessage)) {
                return true;
            }
        }

        // 其他情况下不认为完成，继续对话
        return false;
    }

    /**
     * 检查是否为明确的完成信号
     */
    private boolean isCompletionSignal(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String lowerMessage = message.trim().toLowerCase();

        // 明确的完成信号词汇
        return lowerMessage.equals("完成") ||
               lowerMessage.equals("结束") ||
               lowerMessage.equals("finish") ||
               lowerMessage.equals("done") ||
               lowerMessage.equals("exit") ||
               lowerMessage.equals("quit") ||
               lowerMessage.equals("结束对话") ||
               lowerMessage.equals("任务完成") ||
               lowerMessage.equals("不需要了") ||
               lowerMessage.equals("就这样吧") ||
               lowerMessage.equals("好的，谢谢") ||
               lowerMessage.equals("ok, thanks") ||
               lowerMessage.equals("谢谢") ||
               lowerMessage.contains("任务已经完成") ||
               lowerMessage.contains("我完成了") ||
               lowerMessage.contains("不需要进一步的帮助");
    }

    
    /**
     * 计算实际需要的最大循环次数
     * 简化为固定值，让AI自然地决定何时结束对话
     */
    private int calculateMaxLoops(String userInput, ConversationState state) {
        // 直接返回最大值，不做过度限制
        // 让AI自然地与用户交互，根据对话状态决定何时完成
        return MAX_REACT_LOOPS;
    }

    
    }