package com.hanpf.clearai.cli.cleaning.react;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 路径输入解析器
 * 负责从用户输入中提取@指定的路径，并进行标准化处理
 */
public class PathInputParser {

    private static final Pattern PATH_PATTERN = Pattern.compile("@([^\\s]+)");
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("%([^%]+)%");

    /**
     * 从用户输入中提取@指定的路径
     *
     * @param userInput 用户输入
     * @return 提取的路径列表
     */
    public List<String> extractPaths(String userInput) {
        List<String> paths = new ArrayList<>();

        if (userInput == null || userInput.trim().isEmpty()) {
            return paths;
        }

        Matcher matcher = PATH_PATTERN.matcher(userInput);

        while (matcher.find()) {
            String path = matcher.group(1);
            // 路径标准化处理
            path = normalizePath(path);

            // 移除引号（如果存在）
            path = path.replaceAll("^\"|\"$", "");

            if (isValidPath(path)) {
                paths.add(path);
            }
        }

        return paths;
    }

    /**
     * 路径标准化处理
     *
     * @param path 原始路径
     * @return 标准化后的路径
     */
    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }

        // 替换环境变量
        path = replaceEnvironmentVariables(path);

        // 替换用户主目录缩写
        path = path.replace("~", System.getProperty("user.home"));

        // 统一路径分隔符，但保留通配符和特殊字符
        if (!path.contains("*")) {
            path = path.replace("/", File.separator).replace("\\", File.separator);
        } else {
            // 对于包含通配符的路径，只替换正斜杠
            path = path.replace("/", File.separator);
        }

        // 移除重复的分隔符
        String separator = File.separator;
        String doubleSeparator = separator + separator;
        while (path.contains(doubleSeparator)) {
            path = path.replace(doubleSeparator, separator);
        }

        return path.trim();
    }

    /**
     * 替换环境变量
     *
     * @param path 包含环境变量的路径
     * @return 替换后的路径
     */
    private String replaceEnvironmentVariables(String path) {
        Matcher matcher = ENV_VAR_PATTERN.matcher(path);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String varValue = System.getenv(varName);
            if (varValue != null) {
                matcher.appendReplacement(result, varValue);
            } else {
                // 如果环境变量不存在，保持原样
                matcher.appendReplacement(result, matcher.group());
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 验证路径是否存在且可访问
     *
     * @param path 路径
     * @return 是否有效
     */
    public boolean isValidPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        try {
            File file = new File(path);
            return file.exists() && file.isDirectory() && file.canRead();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证路径格式是否正确（不检查是否存在）
     *
     * @param path 路径
     * @return 格式是否正确
     */
    public boolean isValidPathFormat(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        // 检查路径长度
        if (path.length() > 260) { // Windows路径长度限制
            return false;
        }

        // 检查非法字符
        String illegalChars = "<>:\"|?*";
        for (char c : illegalChars.toCharArray()) {
            if (path.indexOf(c) >= 0 && path.indexOf(c) != 1) { // 允许驱动器符号如C:
                return false;
            }
        }

        return true;
    }

    /**
     * 获取路径的显示名称（用于进度显示）
     *
     * @param path 完整路径
     * @param maxLength 最大长度
     * @return 显示名称
     */
    public String getDisplayName(String path, int maxLength) {
        if (path == null) {
            return "";
        }

        if (path.length() <= maxLength) {
            return path;
        }

        // 简化长路径显示：显示开头和结尾，中间用...省略
        return path.substring(0, maxLength / 2 - 2) + "..." +
               path.substring(path.length() - maxLength / 2 + 2);
    }
}