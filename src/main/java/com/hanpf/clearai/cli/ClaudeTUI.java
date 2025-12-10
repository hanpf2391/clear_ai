package com.hanpf.clearai.cli;

import org.jline.reader.*;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Claude Code 风格的 Java AI 助手终端界面
 *
 * 功能特性：
 * - 美观的欢迎界面和提示符
 * - 动态思考状态显示
 * - 流式输出效果
 * - 命令审查模式（安全特性）
 * - Markdown 简单渲染
 */
public class ClaudeTUI {

    // ANSI 颜色和样式常量
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String ORANGE = "\u001B[38;5;208m";
    private static final String GREEN = "\u001B[38;5;120m";
    private static final String BLUE = "\u001B[38;5;75m";
    private static final String GRAY = "\u001B[38;5;245m";
    private static final String YELLOW = "\u001B[38;5;221m";
    private static final String RED = "\u001B[38;5;203m";

    // Spinner 字符
    private static final String[] SPINNER_FRAMES = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};

    private final Terminal terminal;
    private final LineReader lineReader;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public ClaudeTUI() throws IOException {
        this.terminal = TerminalBuilder.builder()
                .name("Claude-TUI")
                .system(true)
                .encoding("UTF-8")
                .build();

        this.lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
    }

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

    private void showWelcomeBanner() {
        terminal.writer().println();
        terminal.writer().println(BLUE + "╭─────────────────────────────────────────────────────────────────╮" + RESET);
        terminal.writer().println(BLUE + "│" + ORANGE + BOLD + "                        Claude Code TUI Demo" + RESET + BLUE + "                        │" + RESET);
        terminal.writer().println(BLUE + "│                                                                 │" + RESET);
        terminal.writer().println(BLUE + "│" + GREEN + "  🤖 欢迎使用 AI 助手！输入自然语言，我会帮您执行命令  " + RESET + BLUE + "  │" + RESET);
        terminal.writer().println(BLUE + "│                                                                 │" + RESET);
        terminal.writer().println(BLUE + "│" + DIM + "  特性: 命令审查 • 流式输出 • 安全确认 • 跨平台支持      " + RESET + BLUE + "  │" + RESET);
        terminal.writer().println(BLUE + "╰─────────────────────────────────────────────────────────────────╯" + RESET);
        terminal.writer().println();
        terminal.writer().println(GRAY + "💡 试试这些输入：" + RESET);
        terminal.writer().println("   • " + GREEN + "\"列出当前目录的文件\"" + RESET);
        terminal.writer().println("   • " + GREEN + "\"查看当前时间\"" + RESET);
        terminal.writer().println("   • " + GREEN + "\"你好\"" + RESET);
        terminal.writer().println("   • " + RED + "\"exit\" 退出程序" + RESET);
        terminal.writer().println();
        terminal.writer().flush();
    }

    private String getPrompt() {
        LocalDateTime now = LocalDateTime.now();
        String time = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        return ORANGE + "[" + time + "] " + GREEN + "User" + RESET + " > ";
    }

    private void processUserInput(String input) throws Exception {
        // 模拟 AI 思考过程
        showThinkingState();

        // 模拟 AI 决定要执行的命令
        String suggestedCommand = generateSuggestedCommand(input);

        if (suggestedCommand != null) {
            // 进入命令审查模式
            String confirmedCommand = reviewCommand(suggestedCommand);

            if (confirmedCommand != null && !confirmedCommand.trim().isEmpty()) {
                executeCommand(confirmedCommand);
            } else {
                showStreamingResponse("好的，已取消命令执行。有什么其他需要帮助的吗？");
            }
        } else {
            // 普通对话响应
            String response = generateChatResponse(input);
            showStreamingResponse(response);
        }
    }

    private void showThinkingState() throws InterruptedException {
        // 隐藏光标
        terminal.writer().print("\u001B[?25l");
        terminal.flush();

        long startTime = System.currentTimeMillis();
        int frameIndex = 0;

        while (System.currentTimeMillis() - startTime < 2000) { // 2秒思考时间
            terminal.writer().print("\r" + GRAY + SPINNER_FRAMES[frameIndex % SPINNER_FRAMES.length] +
                                   " Claude is thinking..." + RESET);
            terminal.flush();

            Thread.sleep(100);
            frameIndex++;
        }

        // 清除思考行并显示光标
        terminal.writer().print("\r" + " ".repeat(50) + "\r");
        terminal.writer().print("\u001B[?25h");
        terminal.flush();
    }

    private String generateSuggestedCommand(String input) {
        // 简单的命令映射逻辑
        String lowerInput = input.toLowerCase();

        if (lowerInput.contains("列出") || lowerInput.contains("文件") || lowerInput.contains("目录") || lowerInput.contains("ls") || lowerInput.contains("dir")) {
            return System.getProperty("os.name").toLowerCase().contains("win") ? "dir" : "ls -la";
        } else if (lowerInput.contains("清理") || lowerInput.contains("temp") || lowerInput.contains("垃圾")) {
            return System.getProperty("os.name").toLowerCase().contains("win")
                    ? "dir %TEMP%" : "ls -la /tmp";
        } else if (lowerInput.contains("当前") || lowerInput.contains("路径") || lowerInput.contains("pwd") || lowerInput.contains("cd")) {
            return System.getProperty("os.name").toLowerCase().contains("win") ? "echo %CD%" : "pwd";
        } else if (lowerInput.contains("网络") || lowerInput.contains("ping")) {
            return System.getProperty("os.name").toLowerCase().contains("win")
                    ? "ping -n 4 google.com" : "ping -c 4 google.com";
        } else if (lowerInput.contains("日期") || lowerInput.contains("时间") || lowerInput.contains("date")) {
            return System.getProperty("os.name").toLowerCase().contains("win") ? "date /t && time /t" : "date";
        }

        return null;
    }

    private String reviewCommand(String suggestedCommand) throws IOException {
        terminal.writer().println();
        terminal.writer().println(YELLOW + "🤖 AI 建议执行以下命令:" + RESET);
        terminal.writer().println(BLUE + "┌─────────────────────────────────────────────────────────────────┐" + RESET);
        terminal.writer().println(BLUE + "│" + GREEN + " " + String.format("%-63s", suggestedCommand) + BLUE + "│" + RESET);
        terminal.writer().println(BLUE + "└─────────────────────────────────────────────────────────────────┘" + RESET);
        terminal.writer().println();
        terminal.writer().println(GRAY + "💡 您可以:" + RESET);
        terminal.writer().println("  • " + GREEN + "按 Enter" + RESET + " 确认执行");
        terminal.writer().println("  • " + YELLOW + "编辑命令" + RESET + " 然后按 Enter");
        terminal.writer().println("  • " + RED + "按 Ctrl+C" + RESET + " 取消执行");
        terminal.writer().println();

        // 使用 JLine 的预填充功能
        LineReader confirmReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        String confirmedCommand = null;
        try {
            // 设置预填充的缓冲区内容
            confirmReader.getBuffer().clear();
            confirmReader.getBuffer().write(suggestedCommand);
            confirmedCommand = confirmReader.readLine(ORANGE + "[确认执行] > " + RESET);
        } catch (UserInterruptException e) {
            terminal.writer().println(RED + "\n❌ 已取消命令执行" + RESET);
            terminal.flush();
            return null;
        }

        return confirmedCommand;
    }

    private void executeCommand(String command) {
        try {
            terminal.writer().println();
            terminal.writer().println(BLUE + "🔄 执行命令: " + GREEN + command + RESET);
            terminal.writer().println(BLUE + "─".repeat(60) + RESET);
            terminal.writer().flush();

            ProcessBuilder pb = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb.command("cmd", "/c", command);
            } else {
                pb.command("bash", "-c", command);
            }

            pb.redirectErrorStream(true); // 合并错误流和输出流
            Process process = pb.start();

            // 读取输出
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    terminal.writer().println(GRAY + line + RESET);
                }
            }

            int exitCode = process.waitFor();
            terminal.writer().println(BLUE + "─".repeat(60) + RESET);
            if (exitCode == 0) {
                terminal.writer().println(GREEN + "✅ 命令执行成功 (退出码: 0)" + RESET);
            } else {
                terminal.writer().println(YELLOW + "⚠️ 命令执行完成 (退出码: " + exitCode + ")" + RESET);
            }

        } catch (Exception e) {
            terminal.writer().println(RED + "❌ 执行命令时出错: " + e.getMessage() + RESET);
        }

        terminal.writer().println();
        terminal.writer().flush();
    }

    private String generateChatResponse(String input) {
        // 简单的响应生成逻辑
        if (input.toLowerCase().contains("你好") || input.toLowerCase().contains("hello") || input.toLowerCase().contains("hi")) {
            return "**你好！** 我是您的 AI 助手。我可以帮您:\n• 列出文件和目录\n• 查看系统信息\n• 执行各种系统命令\n\n请告诉我您需要什么帮助！";
        } else if (input.toLowerCase().contains("帮助") || input.toLowerCase().contains("help")) {
            return "**使用指南：**\n\n• 输入自然语言描述您的需求\n• 例如: \"列出当前目录的文件\"\n• 例如: \"查看当前时间\"\n• 例如: \"清理临时文件\"\n\n我会分析您的意图并建议相应的命令，所有命令都需要您的确认才能执行。";
        } else if (input.toLowerCase().contains("功能") || input.toLowerCase().contains("特性")) {
            return "**我的主要功能：**\n\n🔍 **智能命令识别** - 理解自然语言并转换为系统命令\n🛡️ **安全审查** - 所有命令都需要您的确认\n🎨 **美观界面** - Claude Code 风格的终端界面\n⚡ **流式输出** - 打字机效果的响应显示\n\n**支持的命令类型：**\n• 文件操作 (列出、清理等)\n• 系统信息 (时间、路径等)\n• 网络诊断 (ping等)";
        } else {
            return "**我理解您想要：** " + input + "\n\n让我为您分析最佳的执行方案...\n\n您可以尝试更具体的描述，比如:\n• \"列出当前目录文件\"\n• \"查看系统时间\"\n• \"显示当前路径\"\n• \"网络测试\"";
        }
    }

    private void showStreamingResponse(String response) throws InterruptedException {
        terminal.writer().println();

        // 简单的 Markdown 解析
        String processedResponse = parseMarkdown(response);

        // 打字机效果
        for (char c : processedResponse.toCharArray()) {
            terminal.writer().print(c);
            terminal.flush();
            Thread.sleep(15); // 15ms 延迟创造打字机效果
        }

        terminal.writer().println();
        terminal.writer().println();
        terminal.flush();
    }

    private String parseMarkdown(String text) {
        // 解析 **bold** 标记
        Pattern boldPattern = Pattern.compile("\\*\\*(.*?)\\*\\*");
        Matcher matcher = boldPattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(sb, BOLD + matcher.group(1) + RESET);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private void showGoodbye() {
        terminal.writer().println();
        terminal.writer().println(GREEN + "👋 感谢使用 Claude Code TUI Demo！" + RESET);
        terminal.writer().println(GRAY + "再见！期待下次为您服务 🚀" + RESET);
        terminal.writer().flush();
    }

    public static void main(String[] args) {
        try {
            ClaudeTUI claudeTUI = new ClaudeTUI();
            claudeTUI.start();
        } catch (Exception e) {
            System.err.println("启动失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}