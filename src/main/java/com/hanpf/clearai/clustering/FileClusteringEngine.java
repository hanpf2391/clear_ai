package com.hanpf.clearai.clustering;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 文件聚类引擎
 * 负责将文件按照特征进行聚类分组
 */
public class FileClusteringEngine {

    private static final long ONE_WEEK_MS = 7 * 24 * 60 * 60 * 1000L;
    private static final long THREE_MONTHS_MS = 90 * 24 * 60 * 60 * 1000L;

    // 父路径缓存，避免重复计算，大幅提升性能
    private final Map<String, String> parentPathCache = new ConcurrentHashMap<>();

    // 序列文件匹配模式
    private static final Pattern PART_PATTERN = Pattern.compile(".*\\.part\\d+\\.(rar|zip|7z)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPLIT_PATTERN = Pattern.compile(".*\\.[zZ]\\d+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RAR_PART_PATTERN = Pattern.compile(".*\\.r\\d+$", Pattern.CASE_INSENSITIVE);

    /**
     * 生成文件特征指纹（I/O优化版）
     * 格式：parentPath|extension|timeBucket
     * 接收预读的lastModified时间，避免重复I/O调用
     */
    public String generateClusterKey(String fileName, String parentPath, long lastModifiedTime) {
        // 缓存优化：同一个父路径只计算一次，避免重复正则匹配
        String normalizedParentPath = parentPathCache.computeIfAbsent(parentPath, this::normalizeParentPath);
        String normalizedExtension = normalizeExtension(fileName);
        String timeBucket = getTimeBucket(lastModifiedTime); // 直接使用传入的时间

        return normalizedParentPath + "|" + normalizedExtension + "|" + timeBucket;
    }

    /**
     * 生成文件特征指纹（缓存优化版）- 保持向后兼容
     */
    public String generateClusterKey(File file, String parentPath) {
        return generateClusterKey(file.getName(), parentPath, file.lastModified());
    }

    /**
     * 标准化父路径（增强版，添加锚点逻辑）
     * 提取有意义的路径特征，避免聚类碎片化
     */
    private String normalizeParentPath(String parentPath) {
        if (parentPath == null) return "UNKNOWN";

        // 转换为小写并统一分隔符
        String normalized = parentPath.toLowerCase().replace("\\", "/");

        // 移除盘符，保留相对路径特征
        if (normalized.contains(":/")) {
            normalized = normalized.substring(normalized.indexOf(":/") + 2);
        }

        // 锚点逻辑：对于"聚类黑洞"目录，直接截断到锚点
        String[] blackHoles = {
            "/node_modules/",
            "/.git/",
            "/target/",
            "/build/",
            "/dist/",
            "/winsxs/",
            "/installer/",
            "/windows/servicing/",
            "/program files/",
            "/program files (x86)/"
        };

        for (String blackHole : blackHoles) {
            int index = normalized.indexOf(blackHole);
            if (index != -1) {
                // 截断到锚点目录
                String anchor = blackHole.replaceAll("/", "_").replaceAll("\\(", "").replaceAll("\\)", "");
                return anchor + "_CONTENT";
            }
        }

        // 智能路径提取：优先保留项目相关目录
        String[] parts = normalized.split("/");
        StringBuilder pathSignature = new StringBuilder();

        // 从后往前找，优先保留有意义的项目目录
        boolean foundProjectDir = false;
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            // 识别项目根目录特征
            if (isProjectDirectory(part) || foundProjectDir) {
                foundProjectDir = true;
                if (pathSignature.length() > 0) {
                    pathSignature.insert(0, "/");
                }
                pathSignature.insert(0, part);

                // 最多保留4层项目相关目录
                if (pathSignature.toString().split("/").length >= 4) {
                    break;
                }
            }
        }

        // 如果没有找到项目目录，使用fallback逻辑
        if (!foundProjectDir) {
            // 保留最后3层目录，避免路径过长
            int start = Math.max(0, parts.length - 3);
            for (int i = start; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    if (pathSignature.length() > 0) {
                        pathSignature.append("/");
                    }
                    pathSignature.append(parts[i]);
                }
            }
        }

        return pathSignature.length() > 0 ? pathSignature.toString() : "root";
    }

    /**
     * 判断是否为项目目录
     */
    private boolean isProjectDirectory(String dirName) {
        // 常见的项目目录名
        String[] projectDirs = {
            "src", "lib", "components", "utils", "assets", "public",
            "views", "pages", "controllers", "models", "services", "tests",
            "test", "spec", "__tests__", "app", "main", "core", "config",
            "webapp", "static", "resources", "content", "scripts", "styles"
        };

        for (String projectDir : projectDirs) {
            if (projectDir.equals(dirName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 扩展名归一化
     * 处理特殊情况：序列文件、无后缀文件等
     */
    private String normalizeExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "__NO_EXT__";
        }

        // 处理序列压缩文件
        if (PART_PATTERN.matcher(fileName).matches()) {
            int dotIndex = fileName.lastIndexOf('.');
            return fileName.substring(dotIndex).toLowerCase(); // .rar, .zip, .7z
        }

        if (SPLIT_PATTERN.matcher(fileName).matches()) {
            return ".zip"; // .z01, .z02 -> .zip
        }

        if (RAR_PART_PATTERN.matcher(fileName).matches()) {
            return ".rar"; // .r01, .r02 -> .rar
        }

        // 处理无扩展名文件
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == fileName.length() - 1) {
            return "__NO_EXT__";
        }

        String extension = fileName.substring(lastDot).toLowerCase();
        return extension;
    }

    /**
     * 时间分桶
     * 根据文件修改时间分为HOT、WARM、COLD
     */
    private String getTimeBucket(long lastModified) {
        long now = System.currentTimeMillis();
        long age = now - lastModified;

        if (age < 0) {
            return "FUTURE"; // 未来时间，可能是系统时间错误
        } else if (age < ONE_WEEK_MS) {
            return "HOT";    // < 1周
        } else if (age < THREE_MONTHS_MS) {
            return "WARM";   // 1周 - 3个月
        } else {
            return "COLD";   // > 3个月
        }
    }

    /**
     * 获取文件扩展名（备用方法）
     */
    public String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDot + 1).toLowerCase();
    }

    /**
     * 判断是否为常见的垃圾文件扩展名
     */
    public boolean isLikelyJunkExtension(String extension) {
        if (extension == null || extension.isEmpty()) return false;

        String[] junkExtensions = {
                "tmp", "temp", "cache", "log", "bak", "old", "part",
                "dmp", "chk", "swp", "swo", "~", "ds_store"
        };

        for (String junk : junkExtensions) {
            if (extension.equalsIgnoreCase(junk)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断路径是否包含垃圾文件关键词
     */
    public boolean isLikelyJunkPath(String path) {
        if (path == null || path.isEmpty()) return false;

        String pathLower = path.toLowerCase();
        String[] junkKeywords = {
                "temp", "tmp", "cache", "recycle", "$recycle.bin", "thumb",
                "download", "backup", "old", "log"
        };

        for (String keyword : junkKeywords) {
            if (pathLower.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 创建描述性的簇ID
     */
    public String createClusterId(String pathSignature, String extension, String timeBucket, int clusterNumber) {
        // 提取关键路径信息
        String keyPath = extractKeyPath(pathSignature);

        // 格式：cluster_001_+downloads+_exe+_cold
        return String.format("cluster_%03d_%s_%s_%s",
                clusterNumber,
                keyPath.replace("/", "_").replace("\\", "_"),
                extension.replace(".", ""),
                timeBucket.toLowerCase());
    }

    /**
     * 提取关键路径信息
     */
    private String extractKeyPath(String pathSignature) {
        if (pathSignature == null || pathSignature.isEmpty()) {
            return "unknown";
        }

        // 提取最后一级目录名
        String[] parts = pathSignature.split("/");
        if (parts.length == 0) {
            return "root";
        }

        String lastPart = parts[parts.length - 1];

        // 如果最后一级是通用名称，往前找一级
        String[] genericNames = {"cache", "temp", "data", "files", "content"};
        for (String generic : genericNames) {
            if (lastPart.equalsIgnoreCase(generic) && parts.length > 1) {
                return parts[parts.length - 2] + "_" + lastPart;
            }
        }

        return lastPart;
    }
}