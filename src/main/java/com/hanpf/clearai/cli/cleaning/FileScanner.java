package com.hanpf.clearai.cli.cleaning;

import com.hanpf.clearai.cli.cleaning.models.FileInfo;
import com.hanpf.clearai.cli.cleaning.models.ScanOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件扫描器
 */
public class FileScanner {
    private static final long KB = 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;

    /**
     * 扫描指定目录
     */
    public List<FileInfo> scanDirectory(String path, ScanOptions options) throws IOException {
        List<FileInfo> files = new ArrayList<>();
        Path startPath = Paths.get(path);

        if (!Files.exists(startPath)) {
            throw new IOException("路径不存在: " + path);
        }

        if (!Files.isDirectory(startPath)) {
            throw new IOException("路径不是目录: " + path);
        }

        // 验证读取权限
        if (!Files.isReadable(startPath)) {
            throw new IOException("无权限读取目录: " + path);
        }

        try {
            scanRecursive(startPath, options, files, 0);
        } catch (SecurityException e) {
            throw new IOException("访问被拒绝: " + e.getMessage());
        }

        // 按大小排序，大文件在前
        files.sort((a, b) -> Long.compare(b.getSize(), a.getSize()));

        // 限制文件数量
        if (files.size() > options.getMaxFiles()) {
            files = files.subList(0, options.getMaxFiles());
        }

        return files;
    }

    /**
     * 递归扫描目录
     */
    private void scanRecursive(Path path, ScanOptions options, List<FileInfo> files, int currentDepth) {
        if (currentDepth > options.getMaxDepth()) {
            return;
        }

        File[] directoryFiles = path.toFile().listFiles();
        if (directoryFiles == null) {
            return;
        }

        for (File file : directoryFiles) {
            try {
                // 检查是否应该排除
                if (shouldExclude(file, options)) {
                    continue;
                }

                // 获取文件属性
                BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

                // 处理目录
                if (file.isDirectory()) {
                    if (options.isIncludeDirectories()) {
                        FileInfo dirInfo = new FileInfo(
                            file.getName(),
                            file.getAbsolutePath(),
                            attrs.size(),
                            attrs.lastModifiedTime().toMillis(),
                            true
                        );
                        files.add(dirInfo);
                    }
                    // 递归扫描子目录
                    scanRecursive(file.toPath(), options, files, currentDepth + 1);
                } else {
                    // 处理文件
                    if (shouldIncludeFile(file, options, attrs)) {
                        FileInfo fileInfo = new FileInfo(
                            file.getName(),
                            file.getAbsolutePath(),
                            attrs.size(),
                            attrs.lastModifiedTime().toMillis(),
                            false
                        );
                        files.add(fileInfo);
                    }
                }
            } catch (IOException | SecurityException e) {
                // 跳过无法访问的文件
                continue;
            }
        }
    }

    /**
     * 检查是否应该排除此文件/目录
     */
    private boolean shouldExclude(File file, ScanOptions options) {
        String path = file.getAbsolutePath();

        // 检查隐藏文件
        if (!options.isIncludeHidden() && file.isHidden()) {
            return true;
        }

        // 检查排除模式
        if (options.shouldExcludePath(path)) {
            return true;
        }

        return false;
    }

    /**
     * 检查是否应该包含此文件
     */
    private boolean shouldIncludeFile(File file, ScanOptions options, BasicFileAttributes attrs) {
        // 检查大小限制
        long fileSize = attrs.size();
        if (fileSize < options.getMinSize() || fileSize > options.getMaxSize()) {
            return false;
        }

        // 检查扩展名过滤
        String extension = getFileExtension(file.getName());
        if (!options.shouldScanExtension(extension)) {
            return false;
        }

        return true;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    /**
     * 获取目录信息统计
     */
    public DirectoryStats getDirectoryStats(String path, ScanOptions options) throws IOException {
        List<FileInfo> files = scanDirectory(path, options);
        return new DirectoryStats(files);
    }

    /**
     * 目录统计信息
     */
    public static class DirectoryStats {
        private final int totalFiles;
        private final long totalSize;
        private final long avgFileSize;
        private final String formattedTotalSize;
        private final long largeFileCount; // >10MB的文件数量
        private final long hiddenFileCount;

        public DirectoryStats(List<FileInfo> files) {
            this.totalFiles = files.size();
            this.totalSize = files.stream().mapToLong(FileInfo::getSize).sum();
            this.avgFileSize = totalFiles > 0 ? totalSize / totalFiles : 0;
            this.formattedTotalSize = formatFileSize(totalSize);
            this.largeFileCount = files.stream().filter(f -> f.getSize() > 10 * MB).count();
            this.hiddenFileCount = files.stream().filter(f -> f.getName().startsWith(".")).count();
        }

        private String formatFileSize(long size) {
            if (size < KB) {
                return size + "B";
            } else if (size < MB) {
                return String.format("%.1fKB", size / (double) KB);
            } else if (size < GB) {
                return String.format("%.1fMB", size / (double) MB);
            } else {
                return String.format("%.1fGB", size / (double) GB);
            }
        }

        // Getters
        public int getTotalFiles() { return totalFiles; }
        public long getTotalSize() { return totalSize; }
        public long getAvgFileSize() { return avgFileSize; }
        public String getFormattedTotalSize() { return formattedTotalSize; }
        public long getLargeFileCount() { return largeFileCount; }
        public long getHiddenFileCount() { return hiddenFileCount; }
    }

    /**
     * 扫描快速模式 - 只获取大文件
     */
    public List<FileInfo> scanLargeFiles(String path, long minSize) throws IOException {
        ScanOptions options = ScanOptions.create()
                .setMinSize(minSize)
                .setMaxFiles(100)
                .setIncludeHidden(false)
                .setMaxDepth(5);

        return scanDirectory(path, options);
    }

    /**
     * 验证路径
     */
    public boolean isValidPath(String path) {
        try {
            Path p = Paths.get(path);
            return Files.exists(p) && Files.isDirectory(p) && Files.isReadable(p);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 展开用户路径 (支持~等)
     */
    public String expandUserPath(String path) {
        if (path.startsWith("~")) {
            String userHome = System.getProperty("user.home");
            return userHome + path.substring(1);
        }

        // 展开环境变量
        if (path.startsWith("%") && path.endsWith("%")) {
            String envVar = path.substring(1, path.length() - 1);
            String value = System.getenv(envVar);
            return value != null ? value : path;
        }

        return path;
    }
}