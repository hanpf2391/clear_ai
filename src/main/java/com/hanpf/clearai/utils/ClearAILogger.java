package com.hanpf.clearai.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ClearAI专用日志系统
 * 用户界面简洁美观，详细调试信息记录到日志文件
 */
public class ClearAILogger {

    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = "clearai-debug.log";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static boolean initialized = false;
    private static Path logPath;

    /**
     * 初始化日志系统
     */
    private static void init() {
        if (initialized) return;

        try {
            // 创建logs目录
            Path logsDir = Paths.get(LOG_DIR);
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
            }

            logPath = logsDir.resolve(LOG_FILE);
            initialized = true;

            // 写入日志文件头部
            writeLog("=== ClearAI Debug Log Started ===", LogLevel.INFO);
            writeLog("Log file: " + logPath.toAbsolutePath(), LogLevel.INFO);

        } catch (IOException e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
        }
    }

    /**
     * 日志级别
     */
    public enum LogLevel {
        INFO("INFO"),
        DEBUG("DEBUG"),
        AI("AI"),
        ERROR("ERROR"),
        JSON("JSON");

        private final String label;
        LogLevel(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * 记录AI响应
     */
    public static void logAIResponse(String request, String response) {
        init();
        writeLog("=== AI Request ===", LogLevel.AI);
        writeLog(request, LogLevel.AI);
        writeLog("=== AI Response ===", LogLevel.AI);
        writeLog(response, LogLevel.AI);
        writeLog("", LogLevel.AI);
    }

    /**
     * 记录JSON解析相关
     */
    public static void logJSON(String operation, String jsonContent, Object result) {
        init();
        writeLog("=== JSON Operation: " + operation + " ===", LogLevel.JSON);
        writeLog("Input: " + jsonContent, LogLevel.JSON);
        writeLog("Result: " + (result != null ? result.toString() : "null"), LogLevel.JSON);
        writeLog("", LogLevel.JSON);
    }

    /**
     * 记录JSON解析错误
     */
    public static void logJSONError(String operation, String jsonContent, Exception error) {
        init();
        writeLog("=== JSON Parse Error: " + operation + " ===", LogLevel.ERROR);
        writeLog("Input JSON: " + jsonContent, LogLevel.ERROR);
        writeLog("Error: " + error.getClass().getSimpleName() + ": " + error.getMessage(), LogLevel.ERROR);
        writeLog("", LogLevel.ERROR);
    }

    /**
     * 记录文件扫描操作
     */
    public static void logFileScan(String path, int fileCount, String totalSize) {
        init();
        writeLog("=== File Scan Operation ===", LogLevel.INFO);
        writeLog("Scan Path: " + path, LogLevel.INFO);
        writeLog("Files Found: " + fileCount, LogLevel.INFO);
        writeLog("Total Size: " + totalSize, LogLevel.INFO);
        writeLog("", LogLevel.INFO);
    }

    /**
     * 记录工具调用
     */
    public static void logToolCall(String toolName, String parameters, String result) {
        init();
        writeLog("=== Tool Call: " + toolName + " ===", LogLevel.DEBUG);
        writeLog("Parameters: " + parameters, LogLevel.DEBUG);
        writeLog("Result: " + result, LogLevel.DEBUG);
        writeLog("", LogLevel.DEBUG);
    }

    /**
     * 记录普通信息
     */
    public static void info(String message) {
        init();
        writeLog(message, LogLevel.INFO);
    }

    /**
     * 记录调试信息
     */
    public static void debug(String message) {
        init();
        writeLog(message, LogLevel.DEBUG);
    }

    /**
     * 记录错误
     */
    public static void error(String message, Exception e) {
        init();
        writeLog("ERROR: " + message, LogLevel.ERROR);
        if (e != null) {
            writeLog("Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage(), LogLevel.ERROR);
            // 记录堆栈跟踪的前几行
            for (int i = 0; i < Math.min(5, e.getStackTrace().length); i++) {
                writeLog("  at " + e.getStackTrace()[i].toString(), LogLevel.ERROR);
            }
        }
    }

    /**
     * 记录配置信息
     */
    public static void logConfiguration(String configType, String details) {
        init();
        writeLog("=== Configuration: " + configType + " ===", LogLevel.INFO);
        writeLog(details, LogLevel.INFO);
        writeLog("", LogLevel.INFO);
    }

    /**
     * 写入日志
     */
    private static void writeLog(String message, LogLevel level) {
        try (FileWriter fw = new FileWriter(logPath.toFile(), true);
             PrintWriter pw = new PrintWriter(fw)) {

            String timestamp = LocalDateTime.now().format(TIME_FORMAT);
            String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);
            pw.println(logEntry);
            pw.flush();

        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    /**
     * 获取日志文件路径
     */
    public static String getLogFilePath() {
        return initialized ? logPath.toAbsolutePath().toString() : "Not initialized";
    }

    /**
     * 清理旧日志（保留最近7天）
     */
    public static void cleanupOldLogs() {
        // TODO: 实现日志轮转功能
    }

    /**
     * 只接受消息的error方法（兼容性）
     */
    public static void error(String message) {
        error(message, (Exception) null);
    }

    /**
     * 记录警告信息
     */
    public static void warn(String message) {
        init();
        writeLog("WARNING: " + message, LogLevel.ERROR);
    }
}