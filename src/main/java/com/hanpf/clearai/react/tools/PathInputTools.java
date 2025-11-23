package com.hanpf.clearai.react.tools;

import com.hanpf.clearai.cli.cleaning.react.PathInputParser;
import dev.langchain4j.agent.tool.Tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 路径输入工具集
 * 提供路径输入、验证和建议功能
 */
public class PathInputTools {

    private final PathInputParser pathInputParser;
    private final PathHistory pathHistory;

    public PathInputTools() {
        this.pathInputParser = new PathInputParser();
        this.pathHistory = new PathHistory();
    }

    /**
     * 从用户输入中提取@指定的路径
     */
    @Tool("从用户输入中提取@指定的路径")
    public List<String> extractPathsFromInput(String userInput) {
        List<String> paths = pathInputParser.extractPaths(userInput);

        // 将有效的路径添加到历史记录
        for (String path : paths) {
            if (pathInputParser.isValidPath(path)) {
                pathHistory.addPath(path);
            }
        }

        return paths;
    }

    /**
     * 验证路径是否存在且可访问
     */
    @Tool("验证路径是否存在且可访问")
    public boolean validatePath(String path) {
        return pathInputParser.isValidPath(path);
    }

    /**
     * 验证路径格式是否正确
     */
    @Tool("验证路径格式是否正确")
    public boolean validatePathFormat(String path) {
        return pathInputParser.isValidPathFormat(path);
    }

    /**
     * 提供路径输入建议
     */
    @Tool("提供路径输入建议")
    public List<String> suggestRecentPaths() {
        return pathHistory.getRecentPaths();
    }

    /**
     * 获取系统常用目录建议
     */
    @Tool("获取系统常用目录建议")
    public List<String> getSystemDirectorySuggestions() {
        String userHome = System.getProperty("user.home");
        String userDownloads = userHome + File.separator + "Downloads";
        String userDesktop = userHome + File.separator + "Desktop";
        String userDocuments = userHome + File.separator + "Documents";
        String userTemp = System.getProperty("java.io.tmpdir");

        return Arrays.asList(
            userDownloads,
            userDesktop,
            userDocuments,
            userTemp
        );
    }

    /**
     * 显示路径输入帮助
     */
    @Tool("显示路径输入帮助")
    public String showInputHelp() {
        return """
            路径输入格式帮助：

            基本语法：
            • 帮我清理这个路径：@C:\\Users\\username\\Downloads
            • 扫描这些目录：@D:\\Projects @E:\\Temp
            • 清理下载文件夹：@%USERPROFILE%\\Downloads

            支持环境变量：
            • %USERPROFILE% - 用户主目录
            • %TEMP% - 系统临时目录
            • %APPDATA% - 应用程序数据目录
            • %LOCALAPPDATA% - 本地应用程序数据目录
            • ~ - 用户主目录缩写

            路径格式要求：
            • 使用反斜杠(\\)或正斜杠(/)作为路径分隔符
            • 路径长度不超过260个字符
            • 避免使用非法字符：< > : " | ? *

            示例：
            • @C:\\Downloads
            • @~/Downloads
            • @%USERPROFILE%\\Desktop\\MyFolder
            • @D:/Projects/Java
            """;
    }

    /**
     * 标准化路径格式
     */
    @Tool("标准化路径格式")
    public String normalizePath(String path) {
        if (path == null) {
            return "";
        }

        // 使用PathInputParser的标准化逻辑
        List<String> paths = pathInputParser.extractPaths("temp " + path);
        return paths.isEmpty() ? "" : paths.get(0);
    }

    /**
     * 检查路径是否为特殊目录（如系统目录）
     */
    @Tool("检查路径是否为特殊目录")
    public boolean isSystemDirectory(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        String lowerPath = path.toLowerCase();
        return lowerPath.contains("windows") ||
               lowerPath.contains("system32") ||
               lowerPath.contains("program files") ||
               lowerPath.contains("programdata");
    }

    /**
     * 获取路径的安全提示
     */
    @Tool("获取路径的安全提示")
    public String getPathSafetyWarning(String path) {
        if (isSystemDirectory(path)) {
            return "⚠️ 警告：这是一个系统目录，删除文件可能影响系统稳定性！";
        }

        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null && files.length > 1000) {
                return "📁 提示：该目录包含大量文件(" + files.length + "个)，扫描可能需要较长时间";
            }
        }

        return "✅ 路径安全，可以扫描";
    }
}

/**
 * 路径历史记录管理类
 */
class PathHistory {
    private static final int MAX_HISTORY_SIZE = 10;
    private final List<String> recentPaths = new ArrayList<>();

    /**
     * 添加路径到历史记录
     */
    public void addPath(String path) {
        // 移除重复项
        recentPaths.remove(path);

        // 添加到开头
        recentPaths.add(0, path);

        // 限制历史记录大小
        while (recentPaths.size() > MAX_HISTORY_SIZE) {
            recentPaths.remove(recentPaths.size() - 1);
        }
    }

    /**
     * 获取最近的路径列表
     */
    public List<String> getRecentPaths() {
        return new ArrayList<>(recentPaths);
    }

    /**
     * 清除历史记录
     */
    public void clear() {
        recentPaths.clear();
    }
}