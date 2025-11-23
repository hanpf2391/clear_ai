package com.hanpf.clearai.react.tools;

import com.hanpf.clearai.cli.cleaning.models.FileInfo;
import com.hanpf.clearai.utils.WhitelistManager;
import dev.langchain4j.agent.tool.Tool;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ReAct清理工具集
 * 集成白名单功能，提供安全的文件操作
 */
public class ReActCleanupTools {

    private final WhitelistManager whitelistManager;

    public ReActCleanupTools() {
        this.whitelistManager = WhitelistManager.getInstance();
    }

    /**
     * 检查文件是否在白名单中（受保护）
     */
    @Tool("检查文件是否在白名单中，无法删除")
    public boolean isFileProtected(String filePath) {
        return whitelistManager.isWhitelisted(filePath);
    }

    /**
     * 检查路径是否为系统路径
     */
    @Tool("检查路径是否为系统路径，需要特别小心")
    public boolean isSystemPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        String lowerPath = path.toLowerCase();

        // 检查常见的系统目录
        return lowerPath.contains("windows") ||
               lowerPath.contains("system32") ||
               lowerPath.contains("program files") ||
               lowerPath.contains("programdata") ||
               lowerPath.contains("$recycle.bin") ||
               lowerPath.contains("system volume information");
    }

    /**
     * 过滤受保护的文件
     */
    @Tool("从文件列表中过滤掉受保护的文件")
    public List<FileInfo> filterProtectedFiles(List<FileInfo> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        return files.stream()
            .filter(file -> !whitelistManager.isWhitelisted(file.getPath()))
            .collect(Collectors.toList());
    }

    /**
     * 分析文件的安全性
     */
    @Tool("分析文件是否可以安全删除")
    public String analyzeFileSafety(String filePath, long fileSize) {
        File file = new File(filePath);

        // 检查白名单
        if (whitelistManager.isWhitelisted(filePath)) {
            return "❌ 受保护文件：此文件在白名单中，无法删除";
        }

        // 检查系统路径
        if (isSystemPath(filePath)) {
            return "⚠️ 系统文件：位于系统目录，删除可能影响系统稳定性";
        }

        // 检查文件大小
        if (fileSize == 0) {
            return "✅ 空文件：可以安全删除";
        } else if (fileSize < 1024) {
            return "✅ 小文件：可以安全删除";
        } else if (fileSize > 100 * 1024 * 1024) { // 100MB
            return "⚠️ 大文件：建议谨慎删除，请确认内容";
        }

        return "✅ 普通文件：可以安全删除";
    }

    /**
     * 获取路径的安全建议
     */
    @Tool("获取路径的安全操作建议")
    public String getPathSafetyAdvice(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "❌ 无效路径";
        }

        StringBuilder advice = new StringBuilder();
        advice.append("📍 路径分析: ").append(path).append("\n");

        // 检查是否在白名单中
        if (whitelistManager.isWhitelisted(path)) {
            advice.append("❌ 此路径在白名单中，受保护无法操作\n");
            return advice.toString();
        }

        // 检查是否为系统路径
        if (isSystemPath(path)) {
            advice.append("⚠️ 这是系统相关路径，请谨慎操作\n");
        } else {
            advice.append("✅ 这是用户路径，可以安全操作\n");
        }

        // 检查路径是否存在
        File file = new File(path);
        if (!file.exists()) {
            advice.append("❌ 路径不存在\n");
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            int fileCount = files != null ? files.length : 0;
            advice.append("📁 目录包含 ").append(fileCount).append(" 个项目\n");
        } else {
            advice.append("📄 这是文件，大小: ").append(formatSize(file.length())).append("\n");
        }

        return advice.toString();
    }

    /**
     * 添加路径到白名单
     */
    @Tool("添加路径到白名单保护")
    public String addPathToWhitelist(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "❌ 路径不能为空";
        }

        whitelistManager.addToWhitelist(path);
        return "✅ 已添加到白名单: " + path;
    }

    /**
     * 从白名单移除路径
     */
    @Tool("从白名单移除路径保护")
    public String removePathFromWhitelist(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "❌ 路径不能为空";
        }

        whitelistManager.removeFromWhitelist(path);
        return "✅ 已从白名单移除: " + path;
    }

    /**
     * 显示白名单信息
     */
    @Tool("显示白名单配置信息")
    public String showWhitelistInfo() {
        StringBuilder info = new StringBuilder();
        info.append("📋 白名单保护信息:\n");
        info.append("  • 白名单规则数量: ").append(whitelistManager.getWhitelistCount()).append("\n");
        info.append("  • 用户可以在 whitelist.txt 中添加保护路径\n");
        info.append("  • 系统会在 system_whitelist.txt 中自动创建保护规则\n");
        info.append("  • 支持环境变量: %USERPROFILE%, %TEMP%, %APPDATA% 等\n");
        info.append("  • 支持通配符: *, ?\n");
        return info.toString();
    }

    /**
     * 格式化文件大小
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}