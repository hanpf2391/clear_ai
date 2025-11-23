package com.hanpf.clearai.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 白名单管理器
 * 负责管理用户不想删除的文件和目录白名单
 */
public class WhitelistManager {

    private static final String WHITELIST_FILE = "whitelist.txt";
    private static final String SYSTEM_WHITELIST_FILE = "system_whitelist.txt";

    private final Set<String> whitelistedPaths;
    private final File whitelistFile;
    private final File systemWhitelistFile;

    private static WhitelistManager instance;

    private WhitelistManager() {
        this.whitelistedPaths = new HashSet<>();
        this.whitelistFile = new File(WHITELIST_FILE);
        this.systemWhitelistFile = new File(SYSTEM_WHITELIST_FILE);

        loadWhitelist();
        loadSystemWhitelist();
    }

    /**
     * 获取单例实例
     */
    public static synchronized WhitelistManager getInstance() {
        if (instance == null) {
            instance = new WhitelistManager();
        }
        return instance;
    }

    /**
     * 加载用户白名单
     */
    private void loadWhitelist() {
        if (!whitelistFile.exists()) {
            createDefaultWhitelist();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(whitelistFile.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    whitelistedPaths.add(line);
                }
            }
            ClearAILogger.info("加载用户白名单，包含 " + whitelistedPaths.size() + " 个路径");
        } catch (IOException e) {
            ClearAILogger.error("加载白名单文件失败: " + e.getMessage());
        }
    }

    /**
     * 加载系统白名单
     */
    private void loadSystemWhitelist() {
        if (!systemWhitelistFile.exists()) {
            createDefaultSystemWhitelist();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(systemWhitelistFile.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    // 替换环境变量
                    line = replaceEnvironmentVariables(line);
                    whitelistedPaths.add(line);
                }
            }
            ClearAILogger.info("加载系统白名单");
        } catch (IOException e) {
            ClearAILogger.error("加载系统白名单文件失败: " + e.getMessage());
        }
    }

    /**
     * 创建默认用户白名单
     */
    private void createDefaultWhitelist() {
        try (BufferedWriter writer = Files.newBufferedWriter(whitelistFile.toPath())) {
            writer.write("# ClearAI 用户白名单文件\n");
            writer.write("# 每行一个路径，支持通配符 * 和 ?\n");
            writer.write("# 以 # 开头的行为注释\n");
            writer.write("# 示例:\n");
            writer.write("# C:\\Users\\username\\Important\\*\n");
            writer.write("# D:\\Projects\\backup\\*.zip\n");
            writer.write("# %USERPROFILE%\\Documents\\*.pdf\n");
            writer.write("\n");
            ClearAILogger.info("创建默认用户白名单文件: " + WHITELIST_FILE);
        } catch (IOException e) {
            ClearAILogger.error("创建白名单文件失败: " + e.getMessage());
        }
    }

    /**
     * 创建默认系统白名单
     */
    private void createDefaultSystemWhitelist() {
        try (BufferedWriter writer = Files.newBufferedWriter(systemWhitelistFile.toPath())) {
            writer.write("# ClearAI 系统白名单文件\n");
            writer.write("# 此文件包含系统重要目录，不建议修改\n");
            writer.write("\n");
            writer.write("# Windows系统目录\n");
            writer.write("C:\\Windows\\*\n");
            writer.write("C:\\Program Files\\*\n");
            writer.write("C:\\Program Files (x86)\\*\n");
            writer.write("C:\\ProgramData\\*\n");
            writer.write("%SystemRoot%\\*\n");
            writer.write("%ProgramFiles%\\*\n");
            writer.write("%ProgramFiles(x86)%\\*\n");
            writer.write("%ProgramData%\\*\n");
            writer.write("\n");
            writer.write("# 系统重要文件\n");
            writer.write("pagefile.sys\n");
            writer.write("hiberfil.sys\n");
            writer.write("swapfile.sys\n");
            writer.write("\n");
            writer.write("# 用户重要配置目录\n");
            writer.write("%USERPROFILE%\\AppData\\Local\\Microsoft\\*\n");
            writer.write("%USERPROFILE%\\AppData\\Roaming\\Microsoft\\*\n");
            writer.write("%APPDATA%\\*\n");
            writer.write("%LOCALAPPDATA%\\*\n");
            writer.write("\n");
            ClearAILogger.info("创建默认系统白名单文件: " + SYSTEM_WHITELIST_FILE);
        } catch (IOException e) {
            ClearAILogger.error("创建系统白名单文件失败: " + e.getMessage());
        }
    }

    /**
     * 替换环境变量
     */
    private String replaceEnvironmentVariables(String path) {
        if (path == null) return "";

        // 替换常见环境变量
        path = path.replace("%USERPROFILE%", System.getProperty("user.home"));
        path = path.replace("%HOME%", System.getProperty("user.home"));
        path = path.replace("%TEMP%", System.getProperty("java.io.tmpdir"));

        String userName = System.getProperty("user.name");
        if (userName != null) {
            path = path.replace("%USERNAME%", userName);
        }

        // 处理Windows系统环境变量
        try {
            String systemRoot = System.getenv("SystemRoot");
            if (systemRoot != null) {
                path = path.replace("%SystemRoot%", systemRoot);
                path = path.replace("%WINDIR%", systemRoot);
            }

            String programFiles = System.getenv("ProgramFiles");
            if (programFiles != null) {
                path = path.replace("%ProgramFiles%", programFiles);
            }

            String programFilesX86 = System.getenv("ProgramFiles(x86)");
            if (programFilesX86 != null) {
                path = path.replace("%ProgramFiles(x86)%", programFilesX86);
            }

            String programData = System.getenv("ProgramData");
            if (programData != null) {
                path = path.replace("%ProgramData%", programData);
            }

            String appData = System.getenv("APPDATA");
            if (appData != null) {
                path = path.replace("%APPDATA%", appData);
            }

            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                path = path.replace("%LOCALAPPDATA%", localAppData);
            }

        } catch (Exception e) {
            // 环境变量访问失败，使用原始路径
        }

        return path;
    }

    /**
     * 检查文件或路径是否在白名单中
     */
    public boolean isWhitelisted(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        // 标准化路径
        String normalizedPath = normalizePath(path);

        // 检查是否匹配任何白名单规则
        for (String whitelistRule : whitelistedPaths) {
            if (matchesWhitelistRule(normalizedPath, whitelistRule)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 标准化路径
     */
    private String normalizePath(String path) {
        if (path == null) return "";

        path = path.replace("/", File.separator).replace("\\", File.separator);
        path = path.toLowerCase();

        // 移除末尾的分隔符
        while (path.endsWith(File.separator)) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    /**
     * 检查路径是否匹配白名单规则
     */
    private boolean matchesWhitelistRule(String path, String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            return false;
        }

        rule = normalizePath(rule);

        // 如果规则以通配符结尾，检查路径前缀
        if (rule.endsWith("*")) {
            String prefix = rule.substring(0, rule.length() - 1);
            return path.startsWith(prefix);
        }

        // 精确匹配
        return path.equals(rule);
    }

    /**
     * 添加路径到白名单
     */
    public void addToWhitelist(String path) {
        if (path == null || path.trim().isEmpty()) {
            return;
        }

        String normalizedPath = normalizePath(path);
        if (whitelistedPaths.add(normalizedPath)) {
            saveWhitelist();
            ClearAILogger.info("添加路径到白名单: " + path);
        }
    }

    /**
     * 从白名单中移除路径
     */
    public void removeFromWhitelist(String path) {
        if (path == null || path.trim().isEmpty()) {
            return;
        }

        String normalizedPath = normalizePath(path);
        if (whitelistedPaths.remove(normalizedPath)) {
            saveWhitelist();
            ClearAILogger.info("从白名单移除路径: " + path);
        }
    }

    /**
     * 保存白名单到文件
     */
    private void saveWhitelist() {
        try (BufferedWriter writer = Files.newBufferedWriter(whitelistFile.toPath())) {
            writer.write("# ClearAI 用户白名单文件\n");
            writer.write("# 此文件由程序自动生成，请谨慎修改\n");
            writer.write("# 每行一个路径，支持通配符 * 和 ?\n");
            writer.write("# 以 # 开头的行为注释\n");
            writer.write("\n");

            for (String path : whitelistedPaths) {
                writer.write(path);
                writer.write("\n");
            }

        } catch (IOException e) {
            ClearAILogger.error("保存白名单文件失败: " + e.getMessage());
        }
    }

    /**
     * 重新加载白名单
     */
    public void reloadWhitelist() {
        whitelistedPaths.clear();
        loadWhitelist();
        loadSystemWhitelist();
    }

    /**
     * 获取白名单规则数量
     */
    public int getWhitelistCount() {
        return whitelistedPaths.size();
    }

    /**
     * 获取所有白名单规则
     */
    public List<String> getWhitelistedPaths() {
        return new ArrayList<>(whitelistedPaths);
    }

    /**
     * 显示白名单信息
     */
    public void displayWhitelistInfo() {
        System.out.println("📋 白名单信息:");
        System.out.println("  用户白名单文件: " + whitelistFile.getAbsolutePath());
        System.out.println("  系统白名单文件: " + systemWhitelistFile.getAbsolutePath());
        System.out.println("  白名单规则数量: " + whitelistedPaths.size());

        if (!whitelistedPaths.isEmpty()) {
            System.out.println("  白名单规则:");
            int count = 0;
            for (String rule : whitelistedPaths) {
                if (count < 10) { // 只显示前10个
                    System.out.println("    • " + rule);
                    count++;
                } else {
                    System.out.println("    ... 还有 " + (whitelistedPaths.size() - 10) + " 个规则");
                    break;
                }
            }
        }
    }
}