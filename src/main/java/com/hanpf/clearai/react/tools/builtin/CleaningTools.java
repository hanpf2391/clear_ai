package com.hanpf.clearai.react.tools.builtin;

import com.hanpf.clearai.react.tools.ReActTool;
import com.hanpf.clearai.react.tools.ToolParam;
import com.hanpf.clearai.utils.ClearAILogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 清理工具集 - 提供文件系统清理相关的工具
 */
public class CleaningTools {

    /**
     * 扫描指定目录的文件信息
     */
    @ReActTool(
        name = "scan_directory",
        description = "扫描指定目录，分析文件分布、大小统计、垃圾文件识别等。用于了解目录的清理潜力。",
        category = "cleaning"
    )
    public String scanDirectory(
        @ToolParam(name = "path", description = "要扫描的目录路径", required = true) String path,
        @ToolParam(name = "include_subdirs", description = "是否包含子目录", required = false, defaultValue = "true") boolean includeSubdirs,
        @ToolParam(name = "max_depth", description = "最大扫描深度，0表示无限制", required = false, defaultValue = "0") int maxDepth
    ) {
        try {
            File directory = new File(path);
            if (!directory.exists()) {
                return "❌ 目录不存在: " + path;
            }

            if (!directory.isDirectory()) {
                return "❌ 指定路径不是目录: " + path;
            }

            DirectoryScanResult result = performDirectoryScan(directory, includeSubdirs, maxDepth);
            return formatScanResult(result);

        } catch (Exception e) {
            ClearAILogger.error("目录扫描失败: " + e.getMessage(), e);
            return "❌ 扫描目录时出错: " + e.getMessage();
        }
    }

    /**
     * 清理临时文件
     */
    @ReActTool(
        name = "clean_temp_files",
        description = "清理系统临时文件和用户临时文件。释放磁盘空间，提升系统性能。",
        category = "cleaning"
    )
    public String cleanTempFiles(
        @ToolParam(name = "include_user_temp", description = "是否包含用户临时目录", required = false, defaultValue = "true") boolean includeUserTemp,
        @ToolParam(name = "include_system_temp", description = "是否包含系统临时目录", required = false, defaultValue = "false") boolean includeSystemTemp
    ) {
        StringBuilder result = new StringBuilder();
        int totalCleaned = 0;
        long totalSize = 0;

        try {
            // 清理用户临时目录
            if (includeUserTemp) {
                String userTemp = System.getProperty("java.io.tmpdir");
                CleaningResult cleanResult = cleanTempDirectory(userTemp, "用户临时目录");
                result.append(cleanResult.getDescription()).append("\n");
                totalCleaned += cleanResult.getFileCount();
                totalSize += cleanResult.getSizeFreed();
            }

            // 清理系统临时目录（需要管理员权限）
            if (includeSystemTemp) {
                String systemTemp = "C:\\Windows\\Temp";
                File tempDir = new File(systemTemp);
                if (tempDir.exists() && tempDir.canWrite()) {
                    CleaningResult cleanResult = cleanTempDirectory(systemTemp, "系统临时目录");
                    result.append(cleanResult.getDescription()).append("\n");
                    totalCleaned += cleanResult.getFileCount();
                    totalSize += cleanResult.getSizeFreed();
                } else {
                    result.append("⚠️ 系统临时目录需要管理员权限\n");
                }
            }

            result.append(String.format("✅ 临时文件清理完成，共清理 %d 个文件，释放 %.2f MB 空间",
                totalCleaned, totalSize / (1024.0 * 1024.0)));

            return result.toString();

        } catch (Exception e) {
            ClearAILogger.error("清理临时文件失败: " + e.getMessage(), e);
            return "❌ 清理临时文件时出错: " + e.getMessage();
        }
    }

    /**
     * 分析磁盘空间使用情况
     */
    @ReActTool(
        name = "analyze_disk_space",
        description = "分析磁盘空间使用情况，包括总容量、已用空间、剩余空间和各目录占用分析。",
        category = "analysis"
    )
    public String analyzeDiskSpace(
        @ToolParam(name = "drive_path", description = "要分析的磁盘路径，如 C:\\", required = false, defaultValue = "C:\\") String drivePath
    ) {
        try {
            File drive = new File(drivePath);
            if (!drive.exists()) {
                return "❌ 磁盘路径不存在: " + drivePath;
            }

            long totalSpace = drive.getTotalSpace();
            long freeSpace = drive.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;

            double usedPercent = (double) usedSpace / totalSpace * 100;
            double freePercent = (double) freeSpace / totalSpace * 100;

            StringBuilder result = new StringBuilder();
            result.append("📊 磁盘空间分析: ").append(drivePath).append("\n");
            result.append(String.format("总容量: %.2f GB\n", totalSpace / (1024.0 * 1024.0 * 1024.0)));
            result.append(String.format("已使用: %.2f GB (%.1f%%)\n", usedSpace / (1024.0 * 1024.0 * 1024.0), usedPercent));
            result.append(String.format("剩余空间: %.2f GB (%.1f%%)\n", freeSpace / (1024.0 * 1024.0 * 1024.0), freePercent));

            // 空间使用建议
            if (usedPercent > 90) {
                result.append("⚠️ 磁盘空间严重不足，建议立即清理\n");
            } else if (usedPercent > 80) {
                result.append("⚠️ 磁盘空间不足，建议清理文件\n");
            } else if (usedPercent > 70) {
                result.append("💡 磁盘使用率较高，可考虑清理\n");
            } else {
                result.append("✅ 磁盘空间充足\n");
            }

            return result.toString();

        } catch (Exception e) {
            ClearAILogger.error("分析磁盘空间失败: " + e.getMessage(), e);
            return "❌ 分析磁盘空间时出错: " + e.getMessage();
        }
    }

    /**
     * 执行目录扫描
     */
    private DirectoryScanResult performDirectoryScan(File directory, boolean includeSubdirs, int maxDepth) throws IOException {
        DirectoryScanResult result = new DirectoryScanResult();
        result.setPath(directory.getAbsolutePath());

        if (!includeSubdirs || maxDepth == 1) {
            // 只扫描顶级目录
            scanSingleLevel(directory, result);
        } else {
            // 递归扫描子目录
            int effectiveMaxDepth = maxDepth <= 0 ? Integer.MAX_VALUE : maxDepth;
            scanRecursive(directory, result, 0, effectiveMaxDepth);
        }

        return result;
    }

    /**
     * 扫描单级目录
     */
    private void scanSingleLevel(File directory, DirectoryScanResult result) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile()) {
                result.addFile(file);
            } else if (file.isDirectory()) {
                result.addDirectory(file);
            }
        }
    }

    /**
     * 递归扫描目录
     */
    private void scanRecursive(File directory, DirectoryScanResult result, int currentDepth, int maxDepth) throws IOException {
        if (currentDepth >= maxDepth) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile()) {
                result.addFile(file);
            } else if (file.isDirectory()) {
                result.addDirectory(file);
                if (currentDepth + 1 < maxDepth) {
                    scanRecursive(file, result, currentDepth + 1, maxDepth);
                }
            }
        }
    }

    /**
     * 格式化扫描结果
     */
    private String formatScanResult(DirectoryScanResult result) {
        StringBuilder output = new StringBuilder();
        output.append("📁 目录扫描结果: ").append(result.getPath()).append("\n");
        output.append(String.format("文件总数: %d\n", result.getFileCount()));
        output.append(String.format("目录总数: %d\n", result.getDirectoryCount()));
        output.append(String.format("总大小: %.2f MB\n", result.getTotalSize() / (1024.0 * 1024.0)));

        // 识别大文件
        List<File> largeFiles = result.getLargeFiles(10 * 1024 * 1024); // 大于10MB的文件
        if (!largeFiles.isEmpty()) {
            output.append("\n🔍 发现大文件 (>10MB):\n");
            for (File file : largeFiles.stream().limit(10).collect(Collectors.toList())) {
                double sizeMB = file.length() / (1024.0 * 1024.0);
                output.append(String.format("  📄 %s (%.2f MB)\n", file.getName(), sizeMB));
            }
        }

        // 识别可能垃圾文件
        List<File> junkFiles = result.identifyJunkFiles();
        if (!junkFiles.isEmpty()) {
            output.append(String.format("\n🗑️ 发现可能的垃圾文件: %d 个\n", junkFiles.size()));
            long junkSize = junkFiles.stream().mapToLong(File::length).sum();
            output.append(String.format("垃圾文件总大小: %.2f MB\n", junkSize / (1024.0 * 1024.0)));
        }

        return output.toString();
    }

    /**
     * 清理临时目录
     */
    private CleaningResult cleanTempDirectory(String tempPath, String description) {
        int cleanedCount = 0;
        long sizeFreed = 0;
        List<String> errors = new ArrayList<>();

        try {
            File tempDir = new File(tempPath);
            if (!tempDir.exists() || !tempDir.isDirectory()) {
                return new CleaningResult(0, 0, description + " 目录不存在");
            }

            File[] files = tempDir.listFiles();
            if (files == null) {
                return new CleaningResult(0, 0, description + " 无法访问");
            }

            for (File file : files) {
                try {
                    if (deleteRecursively(file)) {
                        cleanedCount++;
                        sizeFreed += getFileSizeRecursively(file);
                    }
                } catch (Exception e) {
                    errors.add(file.getName() + ": " + e.getMessage());
                }
            }

            String resultDesc = String.format("%s: 清理完成，删除 %d 个文件，释放 %.2f MB",
                description, cleanedCount, sizeFreed / (1024.0 * 1024.0));

            if (!errors.isEmpty()) {
                resultDesc += String.format("，%d 个文件清理失败", errors.size());
            }

            return new CleaningResult(cleanedCount, sizeFreed, resultDesc);

        } catch (Exception e) {
            return new CleaningResult(0, 0, description + " 清理失败: " + e.getMessage());
        }
    }

    /**
     * 递归删除文件/目录
     */
    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    /**
     * 递归获取文件大小
     */
    private long getFileSizeRecursively(File file) {
        if (file.isFile()) {
            return file.length();
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                long size = 0;
                for (File child : children) {
                    size += getFileSizeRecursively(child);
                }
                return size;
            }
        }

        return 0;
    }

    /**
     * 目录扫描结果数据类
     */
    private static class DirectoryScanResult {
        private String path;
        private final List<File> files = new ArrayList<>();
        private final List<File> directories = new ArrayList<>();

        public void addFile(File file) { files.add(file); }
        public void addDirectory(File directory) { directories.add(directory); }

        public int getFileCount() { return files.size(); }
        public int getDirectoryCount() { return directories.size(); }
        public long getTotalSize() { return files.stream().mapToLong(File::length).sum(); }

        public List<File> getLargeFiles(long minSize) {
            return files.stream()
                .filter(f -> f.length() > minSize)
                .collect(Collectors.toList());
        }

        public List<File> identifyJunkFiles() {
            return files.stream()
                .filter(this::isJunkFile)
                .collect(Collectors.toList());
        }

        private boolean isJunkFile(File file) {
            String name = file.getName().toLowerCase();
            return name.endsWith(".tmp") || name.endsWith(".temp") ||
                   name.endsWith(".log") && file.length() > 50 * 1024 * 1024; // 大于50MB的日志文件
        }

        // Getters and Setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    /**
     * 清理结果数据类
     */
    private static class CleaningResult {
        private final int fileCount;
        private final long sizeFreed;
        private final String description;

        public CleaningResult(int fileCount, long sizeFreed, String description) {
            this.fileCount = fileCount;
            this.sizeFreed = sizeFreed;
            this.description = description;
        }

        public int getFileCount() { return fileCount; }
        public long getSizeFreed() { return sizeFreed; }
        public String getDescription() { return description; }
    }
}