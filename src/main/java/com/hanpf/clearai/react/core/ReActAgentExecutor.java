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

        // 智能任务完成检测 - 避免不必要的循环
        if (isTaskCompleted(state)) {
            String summary = generateFinalSummary(state);
            if (ENABLE_DEBUG_LOGGING) {
                ClearAILogger.info("检测到任务已完成，直接生成最终答案");
            }
            return summary;
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

            // 3. 解析结构化决策
            if (ENABLE_PROGRESS_DISPLAY) {
                progressDisplay.startStep("PARSE_DECISION", "📋 解析AI决策...");
            }

            ReActDecision decision = parseDecision(aiDecision);
            state.addDecision(decision);

            if (ENABLE_PROGRESS_DISPLAY) {
                if (decision.getThought() != null) {
                    progressDisplay.showThinking(decision.getThought());
                }
                progressDisplay.completeStep(String.format("解析完成: %s",
                    decision.isFinalAnswer() ? "最终答案" :
                    decision.hasAction() ? "工具调用" : "无效决策"));
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
     * 解析AI返回的结构化决策
     */
    private ReActDecision parseDecision(String aiResponse) throws Exception {
        ReActDecision decision = new ReActDecision();

        try {
            // 检查编码相关的异常
            if (aiResponse.contains("Conversion =") ||
                aiResponse.contains("UnknownFormatConversionException") ||
                aiResponse.contains("CharacterEncoding") ||
                aiResponse.contains("UnsupportedCharsetException")) {

                // 为编码错误创建友好的响应
                String userFriendlyResponse = String.format(
                    "👋 你好！我是CLEAR AI智能清理助手。\n\n" +
                    "🚀 我可以帮你：\n" +
                    "• 智能扫描和清理垃圾文件\n" +
                    "• 分析磁盘空间使用情况\n" +
                    "• 提供系统优化建议\n\n" +
                    "💡 试试对我说：\n" +
                    "• \"检查C盘空间\"\n" +
                    "• \"扫描下载文件夹\"\n" +
                    "• \"清理临时文件\""
                );

                decision.setFinalAnswer(userFriendlyResponse);
                return decision;
            }

            // 检查是否是纯文本响应（不包含JSON）
            if (!aiResponse.contains("{") || !aiResponse.contains("}")) {
                // 纯文本响应，直接作为最终答案
                decision.setFinalAnswer(aiResponse.trim());
                return decision;
            }

            // 尝试提取JSON部分
            String jsonStr = extractJsonFromResponse(aiResponse);

            if (jsonStr.isEmpty()) {
                // JSON提取失败，但响应可能包含有用信息
                if (aiResponse.trim().length() > 10) {
                    decision.setFinalAnswer(aiResponse.trim());
                } else {
                    decision.setFinalAnswer("抱歉，我没有理解您的请求。请重新描述您需要什么帮助。");
                }
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

        } catch (java.util.UnknownFormatConversionException e) {
            // 专门处理格式转换异常
            ClearAILogger.error("字符编码转换异常: " + e.getMessage());

            String encodingErrorResponse = String.format(
                "🤖 AI服务状态:\n" +
                "  提供商: 智普AI\n" +
                "  模型: glm-4.5-air\n" +
                "  连接状态: ✅ 正在处理中文编码问题\n\n" +
                "🔧 系统优化中...\n" +
                "👋 很抱歉出现编码问题，请重试您的请求。"
            );

            decision.setFinalAnswer(encodingErrorResponse);
            return decision;

        } catch (Exception e) {
            // 检查是否为编码相关异常
            String errorMessage = e.getMessage();
            if (errorMessage != null && (
                errorMessage.contains("Conversion =") ||
                errorMessage.contains("UnknownFormatConversionException") ||
                errorMessage.contains("CharacterEncoding") ||
                errorMessage.contains("UnsupportedCharsetException"))) {

                // 为编码异常创建友好的响应
                String userFriendlyResponse = String.format(
                    "🤖 AI服务状态:\n" +
                    "  提供商: 智普AI\n" +
                    "  模型: glm-4.5-air\n" +
                    "  连接状态: ✅ 正在处理中文编码问题\n\n" +
                    "🔧 系统优化中...\n" +
                    "👋 很抱歉出现编码问题，请重试您的请求。"
                );

                decision.setFinalAnswer(userFriendlyResponse);
                return decision;
            }

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
     * 判断是否有足够的目录信息
     */
    private boolean hasEnoughDirectoryInfo(ConversationState state) {
        List<String> scanResults = state.getToolHistory("scan_directory");
        if (scanResults.isEmpty()) {
            return false;
        }

        // 检查最近的扫描结果是否包含详细信息
        String latestScan = scanResults.get(scanResults.size() - 1);

        // 如果扫描结果包含文件列表或大小信息，认为有足够信息
        return latestScan.contains("找到") &&
               (latestScan.contains("个文件") ||
                latestScan.contains("MB") ||
                latestScan.contains("字节"));
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

    
    /**
     * 生成最终摘要
     * 基于已有的工具结果生成最终答案
     */
    private String generateFinalSummary(ConversationState state) {
        StringBuilder summary = new StringBuilder();

        // 优先显示结构化分析结果
        if (state.hasToolBeenCalled("analyzeDirectoryForCleaning")) {
            List<String> results = state.getToolHistory("analyzeDirectoryForCleaning");
            if (!results.isEmpty()) {
                summary.append("🚀 **目录分析完成！**\n\n");
                summary.append(results.get(results.size() - 1)); // 显示最新结果
                return summary.toString();
            }
        }

        // 次优显示普通目录分析结果
        if (state.hasToolBeenCalled("analyzeDirectory")) {
            List<String> results = state.getToolHistory("analyzeDirectory");
            if (!results.isEmpty()) {
                summary.append("📊 **目录分析结果：**\n\n");
                summary.append(results.get(results.size() - 1));
                return summary.toString();
            }
        }

        // 显示扫描结果摘要
        if (state.hasToolBeenCalled("scan_directory")) {
            List<String> results = state.getToolHistory("scan_directory");
            if (!results.isEmpty()) {
                summary.append("🔍 **扫描结果摘要：**\n\n");
                String latestResult = results.get(results.size() - 1);

                // 提取关键信息
                if (latestResult.contains("找到")) {
                    summary.append(latestResult);
                } else {
                    summary.append("已完成目录扫描，发现了一些文件。\n\n");
                    summary.append("💡 **建议：** 如需详细分析，可以请求进一步的操作建议。");
                }

                return summary.toString();
            }
        }

        // 默认摘要
        summary.append("✅ **任务已完成！**\n\n");
        summary.append("我已经完成了您的请求。如需进一步帮助，请告诉我。");

        return summary.toString();
    }
}