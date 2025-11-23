package com.hanpf.clearai.cli.cleaning.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * 路径解析器
 */
public class PathResolver {

    private static final List<String> COMMON_PATHS = Arrays.asList(
        "Downloads", "Desktop", "Documents", "Pictures", "Videos",
        "Music", "temp", "tmp", "cache"
    );

    /**
     * 解析用户输入的路径
     */
    public static String resolvePath(String input) {
        if (input == null || input.trim().isEmpty()) {
            return getCurrentUserDownloads();
        }

        String cleanedPath = input.trim().replaceAll("[\"']", "");

        // 绝对路径直接返回
        if (isAbsolutePath(cleanedPath)) {
            return expandEnvironmentVariables(cleanedPath);
        }

        // 相对路径
        return resolveRelativePath(cleanedPath);
    }

    /**
     * 检查是否为绝对路径
     */
    private static boolean isAbsolutePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        // Windows驱动器路径 (C:\, D:\等)
        if (path.matches("^[A-Za-z]:\\\\.*")) {
            return true;
        }

        // WindowsUNC路径 (\\server\share)
        if (path.startsWith("\\\\")) {
            return true;
        }

        // Unix/Linux绝对路径 (/home/user)
        if (path.startsWith("/")) {
            return true;
        }

        return false;
    }

    /**
     * 展开环境变量
     */
    private static String expandEnvironmentVariables(String path) {
        // Windows环境变量 %TEMP%, %USERPROFILE%等
        if (path.startsWith("%") && path.contains("%")) {
            int start = path.indexOf("%");
            int end = path.indexOf("%", start + 1);
            if (end > start) {
                String envVar = path.substring(start + 1, end);
                String envValue = System.getenv(envVar);
                if (envValue != null) {
                    return envValue + path.substring(end + 1);
                }
            }
        }

        // Unix环境变量 $HOME, $USER等
        if (path.startsWith("$")) {
            String[] parts = path.split("[/\\\\]", 2);
            String envVar = parts[0].substring(1);
            String envValue = System.getenv(envVar);
            if (envValue != null && parts.length > 1) {
                return envValue + File.separator + parts[1];
            } else if (envValue != null) {
                return envValue;
            }
        }

        return path;
    }

    /**
     * 解析相对路径
     */
    private static String resolveRelativePath(String path) {
        String userHome = System.getProperty("user.home");
        String currentDir = System.getProperty("user.dir");

        // 处理~路径
        if (path.startsWith("~")) {
            return userHome + path.substring(1);
        }

        // 处理常见目录名
        for (String commonPath : COMMON_PATHS) {
            if (path.equalsIgnoreCase(commonPath)) {
                return resolveCommonPath(commonPath);
            }
        }

        // 相对于当前目录
        Path currentPath = Paths.get(currentDir, path);
        return currentPath.toAbsolutePath().toString();
    }

    /**
     * 解析常见目录路径
     */
    private static String resolveCommonPath(String dirName) {
        String userHome = System.getProperty("user.home");

        switch (dirName.toLowerCase()) {
            case "downloads":
                return userHome + File.separator + "Downloads";
            case "desktop":
                return userHome + File.separator + "Desktop";
            case "documents":
                return userHome + File.separator + "Documents";
            case "pictures":
                return userHome + File.separator + "Pictures";
            case "videos":
                return userHome + File.separator + "Videos";
            case "music":
                return userHome + File.separator + "Music";
            case "temp":
            case "tmp":
                return System.getProperty("java.io.tmpdir");
            case "cache":
                return getCacheDirectory();
            default:
                return userHome + File.separator + dirName;
        }
    }

    /**
     * 获取缓存目录
     */
    private static String getCacheDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        if (os.contains("win")) {
            return System.getenv("LOCALAPPDATA") != null
                ? System.getenv("LOCALAPPDATA") + "\\Temp"
                : userHome + "\\AppData\\Local\\Temp";
        } else if (os.contains("mac")) {
            return userHome + "/Library/Caches";
        } else {
            return userHome + "/.cache";
        }
    }

    /**
     * 获取当前用户的Downloads目录
     */
    private static String getCurrentUserDownloads() {
        String userHome = System.getProperty("user.home");
        return userHome + File.separator + "Downloads";
    }

    /**
     * 验证路径是否有效
     */
    public static boolean isValidPath(String path) {
        try {
            Path p = Paths.get(path);
            return Files.exists(p) && Files.isDirectory(p) && Files.isReadable(p);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取友好的路径显示名称
     */
    public static String getFriendlyPathName(String path) {
        String userHome = System.getProperty("user.home");

        if (path.startsWith(userHome)) {
            return "~" + path.substring(userHome.length());
        }

        String currentDir = System.getProperty("user.dir");
        if (path.startsWith(currentDir)) {
            return "." + path.substring(currentDir.length());
        }

        return path;
    }

    /**
     * 规范化路径
     */
    public static String normalizePath(String path) {
        try {
            return Paths.get(path).toAbsolutePath().normalize().toString();
        } catch (InvalidPathException e) {
            return path;
        }
    }

    /**
     * 检查路径是否为系统关键目录
     */
    public static boolean isSystemPath(String path) {
        String lowerPath = path.toLowerCase();

        // Windows系统路径
        String[] windowsSystemPaths = {
            "c:\\windows", "c:\\program files", "c:\\program files (x86)",
            "c:\\programdata", "system32", "syswow64"
        };

        // Linux/Mac系统路径
        String[] unixSystemPaths = {
            "/bin", "/sbin", "/usr/bin", "/usr/sbin", "/etc", "/boot",
            "/lib", "/lib64", "/proc", "/sys", "/dev"
        };

        for (String sysPath : windowsSystemPaths) {
            if (lowerPath.contains(sysPath)) {
                return true;
            }
        }

        for (String sysPath : unixSystemPaths) {
            if (lowerPath.contains(sysPath)) {
                return true;
            }
        }

        return false;
    }
}