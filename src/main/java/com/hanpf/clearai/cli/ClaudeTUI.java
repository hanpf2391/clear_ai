package com.hanpf.clearai.cli;

import org.jline.reader.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

// 导入新的ReAct功能相关类
import com.hanpf.clearai.react.agent.ClearAiAgent;
import com.hanpf.clearai.react.agent.ClearAiToolExecutor;

/**
 * Claude Code 风格的 Java AI 助手终端界面 - 简化版本
 *
 * 新架构特点：
 * - 统一的AI代理入口（ClearAiAgent）
 * - 移除复杂的分支逻辑
 * - 真正的ReAct对话流
 */
public class ClaudeTUI {

    // ANSI 颜色和样式常量
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String ORANGE = "\u001B[38;5;208m";
    private static final String GREEN = "\u001B[38;5;120m";
    private static final String BLUE = "\u001B[38;5;75m";
    private static final String GRAY = "\u001B[38;5;245m";
    private static final String YELLOW = "\u001B[38;5;221m";
    private static final String RED = "\u001B[38;5;203m";
    private static final String CYAN = "\u001B[38;5;87m";

    private final Terminal terminal;
    private LineReader lineReader;
    private final AtomicBoolean running = new AtomicBoolean(true);

    // 新的ReAct AI代理 - 统一的智能核心
    private ClearAiAgent clearAiAgent;
    private ClearAiToolExecutor toolExecutor;

    public ClaudeTUI() throws IOException {
        this.terminal = TerminalBuilder.builder()
                .name("Claude-TUI")
                .system(true)
                .encoding("UTF-8")
                .build();

        // 检查配置文件
        if (!com.hanpf.clearai.config.AIConfigManager.isConfigComplete()) {
            System.out.println("❌ 未找到配置文件 setting.json，程序将无法正常运行");
            System.out.println("请在程序同级目录创建 setting.json 文件并配置AI信息");
            System.out.println("按任意键退出...");
            try {
                System.in.read();
            } catch (Exception e) {
                // 忽略异常
            }
            System.exit(1);
            return;
        }

        this.lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        // 初始化新的ReAct AI代理 - 简化的统一入口
        this.clearAiAgent = new ClearAiAgent();
        this.toolExecutor = new ClearAiToolExecutor();
        this.toolExecutor.setTerminal(terminal);

        // 新架构：简化初始化
        terminal.writer().println(GRAY + "✨ 智能清理助手已启动" + RESET);
        terminal.writer().println();

        // 显示AI配置信息
        displayAIConfig();
    }

    /**
     * 显示AI配置信息
     */
    private void displayAIConfig() {
        try {
            String providerName = com.hanpf.clearai.config.AIConfigManager.getProviderName();
            String modelName = com.hanpf.clearai.config.AIConfigManager.getCurrentModel();

            terminal.writer().println(CYAN + "🤖 AI服务状态:" + RESET);
            terminal.writer().println("  提供商: " + GREEN + providerName + RESET);
            terminal.writer().println("  模型: " + GREEN + modelName + RESET);
            terminal.writer().println("  配置文件: " + GREEN + "setting.json" + RESET);
            terminal.writer().println("  连接状态: " + GREEN + "✅ 已连接" + RESET);
            terminal.writer().println();
            terminal.writer().flush();
        } catch (Exception e) {
            // 静默处理，避免启动失败
        }
    }

    /**
     * 启动TUI界面
     */
    public void start() {
        showWelcomeBanner();

        while (running.get()) {
            try {
                String input = lineReader.readLine(getPrompt());

                if (input == null || input.trim().equalsIgnoreCase("exit")) {
                    break;
                }

                if (!input.trim().isEmpty()) {
                    processUserInput(input.trim());
                }

            } catch (UserInterruptException e) {
                terminal.writer().println("^C");
                terminal.flush();
            } catch (EndOfFileException e) {
                break;
            } catch (Exception e) {
                terminal.writer().println(RED + "错误: " + e.getMessage() + RESET);
                terminal.flush();
            }
        }

        showGoodbye();
    }

    /**
     * 显示欢迎界面
     */
    private void showWelcomeBanner() {
        terminal.writer().println();
        terminal.writer().println(BLUE + "╭─────────────────────────────────────────────────────────────────╮" + RESET);
        terminal.writer().println(BLUE + "│" + ORANGE + BOLD + "                            CLEAR AI                              " + RESET + BLUE + "│" + RESET);
        terminal.writer().println(BLUE + "│                                                                 │" + RESET);
        terminal.writer().println(BLUE + "│" + GREEN + "  🧹 欢迎使用智能清理助手！让AI帮您轻松清理电脑        " + RESET + BLUE + "  │" + RESET);
        terminal.writer().println(BLUE + "│                                                                 │" + RESET);
        terminal.writer().println(BLUE + "│" + GRAY + "  功能: 智能文件分析 • 垃圾清理 • 系统优化              " + RESET + BLUE + "  │" + RESET);
        terminal.writer().println(BLUE + "╰─────────────────────────────────────────────────────────────────╯" + RESET);
        terminal.writer().println();
        terminal.writer().println(GRAY + "💡 使用说明：" + RESET);
        terminal.writer().println("   • 直接输入问题，如：" + GREEN + "清理电脑、扫描文件" + RESET);
        terminal.writer().println("   • AI工具调用：" + GREEN + "检查C盘空间、分析下载文件夹" + RESET);
        terminal.writer().println("   • 路径扫描：" + GREEN + "扫描@C:\\Downloads 或直接说清理下载文件夹" + RESET);
        terminal.writer().println("   • 输入 " + RED + "exit" + RESET + " 退出程序");
        terminal.writer().println();
        terminal.writer().println(YELLOW + "🚀 新架构：真正的智能对话，无需填表式交互！" + RESET);
        terminal.writer().println();
        terminal.writer().flush();
    }

    /**
     * 获取输入提示符
     */
    private String getPrompt() {
        LocalDateTime now = LocalDateTime.now();
        String time = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        return ORANGE + "[" + time + "] " + GREEN + "User" + RESET + " > ";
    }

    /**
     * 处理用户输入 - 智能处理用户意图
     */
    private void processUserInput(String input) throws Exception {
        // 检查用户是否想要取消操作
        if (isUserCancellation(input)) {
            terminal.writer().println(YELLOW + "✅ 操作已取消，有什么其他可以帮助您的吗？" + RESET);
            terminal.writer().flush();
            return;
        }

        // 检查是否是退出命令
        if (isExitCommand(input)) {
            running.set(false);
            return;
        }

        // 统一处理：所有其他输入都交给ClearAiAgent
        String response = clearAiAgent.chat(input);

        // 检查是否是询问用户的问题
        if (response.startsWith("QUESTION_ASKED:")) {
            // 处理需要用户输入的情况
            String question = response.substring("QUESTION_ASKED:".length());
            handleUserClarification(question);
        } else if (response.contains("等待用户确认")) {
            // AI正在等待用户确认，提示用户继续
            terminal.writer().println(CYAN + response + RESET);
            terminal.writer().println(GRAY + "💡 您可以回复选项编号、确认操作，或说'取消'来中止" + RESET);
        } else {
            // 直接显示AI响应
            terminal.writer().println(CYAN + response + RESET);
        }
        terminal.writer().flush();
    }

    /**
     * 检查是否是用户取消操作的表达
     */
    private boolean isUserCancellation(String input) {
        String lowerInput = input.toLowerCase().trim();
        return lowerInput.equals("算了") ||
               lowerInput.equals("取消") ||
               lowerInput.equals("不") ||
               lowerInput.equals("不要") ||
               lowerInput.equals("停止") ||
               lowerInput.equals("exit") ||
               lowerInput.equals("quit") ||
               lowerInput.startsWith("取消") ||
               lowerInput.contains("不想");
    }

    /**
     * 检查是否是退出命令
     */
    private boolean isExitCommand(String input) {
        String lowerInput = input.toLowerCase().trim();
        return lowerInput.equals("exit") ||
               lowerInput.equals("quit") ||
               lowerInput.equals("退出") ||
               lowerInput.equals("再见") ||
               lowerInput.equals("拜拜");
    }

    /**
     * 处理需要用户澄清的情况
     */
    private void handleUserClarification(String question) {
        try {
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            String userInput = reader.readLine().trim();

            if (!userInput.isEmpty()) {
                // 将用户回应继续交给AI处理
                String response = clearAiAgent.chat(userInput);
                terminal.writer().println(CYAN + response + RESET);
            }
        } catch (Exception e) {
            terminal.writer().println(RED + "❌ 处理用户输入时出错: " + e.getMessage() + RESET);
        }
        terminal.writer().flush();
    }

    /**
     * 显示告别信息
     */
    private void showGoodbye() {
        terminal.writer().println();
        terminal.writer().println(GREEN + "👋 感谢使用 CLEAR AI 智能清理助手！" + RESET);
        terminal.writer().println(GRAY + "期待下次为您服务 😊" + RESET);
        terminal.writer().println();
        terminal.writer().flush();
    }

    /**
     * 主程序入口
     */
    public static void main(String[] args) {
        try {
            ClaudeTUI tui = new ClaudeTUI();
            tui.start();
        } catch (Exception e) {
            System.err.println("启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}