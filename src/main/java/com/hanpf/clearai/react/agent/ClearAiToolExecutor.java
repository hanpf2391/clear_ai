package com.hanpf.clearai.react.agent;

import com.fasterxml.jackson.databind.JsonNode;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import java.io.File;
import java.util.*;

/**
 * CLEAR AI工具执行器
 * 负责执行AI代理决策的各种工具调用
 */
public class ClearAiToolExecutor {

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String RED = "\u001B[31m";
    private static final String GRAY = "\u001B[90m";

    private Terminal terminal;

    public ClearAiToolExecutor() {
        // Terminal将在第一次调用时通过TUI传入
    }

    public void setTerminal(Terminal terminal) {
        this.terminal = terminal;
    }

    /**
     * 执行指定的工具
     */
    public String executeTool(String toolName, JsonNode parameters, ClearAiAgent agent) {
        try {
            switch (toolName) {
                case "scan_directory":
                    return executeScanDirectory(parameters);
                case "delete_files":
                    return executeDeleteFiles(parameters);
                case "ask_user_for_clarification":
                    return executeAskUserForClarification(parameters);
                case "provide_suggestions":
                    return executeProvideSuggestions(parameters);
                case "cancel_operation":
                    return executeCancelOperation();
                default:
                    return "❌ 未知工具: " + toolName;
            }
        } catch (Exception e) {
            return "❌ 执行工具时出错: " + e.getMessage();
        }
    }

    /**
     * 扫描目录工具
     */
    private String executeScanDirectory(JsonNode parameters) {
        String path = parameters.get("path").asText();

        printWithColor(CYAN, "🔍 正在扫描目录: " + path);

        File directory = new File(path);
        if (!directory.exists()) {
            return "❌ 目录不存在: " + path;
        }

        if (!directory.isDirectory()) {
            return "❌ 不是有效目录: " + path;
        }

        // 简化的目录扫描逻辑
        File[] files = directory.listFiles();
        if (files == null) {
            return "❌ 无法读取目录内容: " + path;
        }

        printWithColor(GREEN, "✅ 扫描完成，找到 " + files.length + " 个项目");

        // 生成扫描报告
        StringBuilder report = new StringBuilder();
        report.append("📊 **扫描报告**\n\n");
        report.append("**路径**: ").append(path).append("\n");
        report.append("**项目总数**: ").append(files.length).append("\n\n");

        // 按类型分类统计
        long totalSize = 0;
        int fileCount = 0;
        int dirCount = 0;

        Map<String, Integer> extensionCount = new HashMap<>();

        for (File file : files) {
            if (file.isFile()) {
                fileCount++;
                totalSize += file.length();

                // 统计文件扩展名
                String name = file.getName().toLowerCase();
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex > 0) {
                    String ext = name.substring(dotIndex);
                    extensionCount.put(ext, extensionCount.getOrDefault(ext, 0) + 1);
                }
            } else if (file.isDirectory()) {
                dirCount++;
            }
        }

        report.append("**文件**: ").append(fileCount).append("\n");
        report.append("**文件夹**: ").append(dirCount).append("\n");
        report.append("**总大小**: ").append(formatFileSize(totalSize)).append("\n\n");

        // 显示常见文件类型
        if (!extensionCount.isEmpty()) {
            report.append("**文件类型分布**:\n");
            extensionCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> report.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" 个\n"));
        }

        printWithColor(GRAY, report.toString());

        return "📋 扫描完成！发现 " + fileCount + " 个文件和 " + dirCount + " 个文件夹，总计占用 " + formatFileSize(totalSize) +
               "。如果您想清理其中一些文件，请告诉我具体要求。";
    }

    /**
     * 删除文件工具（简化版本）
     */
    private String executeDeleteFiles(JsonNode parameters) {
        // 这里暂时只返回确认信息，实际删除逻辑需要更复杂的实现
        printWithColor(YELLOW, "⚠️ 文件删除功能正在开发中");
        return "🔒 文件删除功能已安全暂停。在正式版本中，这里会有详细的确认步骤。";
    }

    /**
     * 询问用户澄清工具
     */
    private String executeAskUserForClarification(JsonNode parameters) {
        String question = parameters.get("question").asText();

        printWithColor(YELLOW, "🤖 " + question);

        if (terminal != null) {
            terminal.writer().print(GREEN + "[您] " + RESET);
            terminal.writer().flush();
        }

        // 返回提示信息，实际的用户输入将在TUI层处理
        return "QUESTION_ASKED:" + question;
    }

    /**
     * 提供建议工具
     */
    private String executeProvideSuggestions(JsonNode parameters) {
        JsonNode suggestions = parameters.get("suggestions");

        printWithColor(YELLOW, "💡 我可以帮您：");

        StringBuilder response = new StringBuilder();
        response.append("💡 我可以帮您：\n\n");

        for (int i = 0; i < suggestions.size(); i++) {
            String suggestion = suggestions.get(i).asText();
            response.append("  ").append(i + 1).append(". ").append(suggestion).append("\n");
            printWithColor(GRAY, "  " + (i + 1) + ". " + suggestion);
        }

        response.append("\n请告诉我您想要哪个选项，或者直接描述您的需求。");

        return response.toString();
    }

    /**
     * 取消操作工具
     */
    private String executeCancelOperation() {
        printWithColor(GREEN, "✅ 操作已取消");
        return "好的，操作已取消。如果您有其他需要，随时可以告诉我。";
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 带颜色的打印
     */
    private void printWithColor(String color, String message) {
        if (terminal != null) {
            terminal.writer().println(color + message + RESET);
            terminal.writer().flush();
        } else {
            // 如果没有terminal，直接输出到控制台
            System.out.println(message);
        }
    }
}