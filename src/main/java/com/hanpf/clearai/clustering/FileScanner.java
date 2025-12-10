package com.hanpf.clearai.clustering;

import com.hanpf.clearai.utils.ClearAILogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

/**
 * 高速文件扫描器
 * 使用NIO和并发扫描提高性能
 */
public class FileScanner {

    private final FileClusteringEngine clusteringEngine;
    private final Map<String, FileCluster> clusters;
    private final AtomicInteger clusterCounter;
    private final LongAdder scannedFiles;
    private final LongAdder totalSize;
    private final List<String> errors;

    // 系统关键目录白名单（只保护最核心的系统文件）
    private static final Set<String> SYSTEM_CRITICAL_PATHS = Set.of(
            "windows/system32",
            "windows/syswow64",
            "windows/servicing",
            "windows/winsxs"
    );

    public FileScanner() {
        this.clusteringEngine = new FileClusteringEngine();
        this.clusters = new ConcurrentHashMap<>();
        this.clusterCounter = new AtomicInteger(1);
        this.scannedFiles = new LongAdder();
        this.totalSize = new LongAdder();
        this.errors = new ArrayList<>();
    }

    /**
     * 扫描指定目录并进行文件聚类
     * @param directoryPath 要扫描的目录路径
     * @param includeSubdirs 是否包含子目录
     * @param maxDepth 最大扫描深度
     * @return 聚类结果
     */
    public ScanResult scanAndCluster(String directoryPath, boolean includeSubdirs, int maxDepth) {
        ClearAILogger.info("🔧 开始扫描目录: " + directoryPath);
        ClearAILogger.info("   参数: 包含子目录=" + includeSubdirs + ", 最大深度=" + maxDepth);

        long startTime = System.currentTimeMillis();

        try {
            Path rootPath = Paths.get(directoryPath);
            if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
                errors.add("目录不存在或不是目录: " + directoryPath);
                return new ScanResult(new ArrayList<>(), scannedFiles.sum(), totalSize.sum(), errors);
            }

            // 执行扫描
            if (includeSubdirs && maxDepth > 1) {
                scanWithDepth(rootPath, maxDepth);
            } else {
                scanSingleLevel(rootPath);
            }

            long duration = System.currentTimeMillis() - startTime;
            ClearAILogger.info("✅ 扫描完成，耗时: " + duration + "ms");
            ClearAILogger.info("   📊 扫描结果: " + scannedFiles.sum() + "个文件, " + clusters.size() + "个簇");
            ClearAILogger.info("   💾 总大小: " + formatFileSize(totalSize.sum()));

            return new ScanResult(new ArrayList<>(clusters.values()), scannedFiles.sum(), totalSize.sum(), errors);

        } catch (Exception e) {
            ClearAILogger.error("扫描过程中出错: " + e.getMessage(), e);
            errors.add("扫描失败: " + e.getMessage());
            return new ScanResult(new ArrayList<>(clusters.values()), scannedFiles.sum(), totalSize.sum(), errors);
        }
    }

    /**
     * 带深度的并行递归扫描（I/O优化版）
     */
    private void scanWithDepth(Path rootPath, int maxDepth) throws IOException {
        try (Stream<Path> pathStream = Files.walk(rootPath, maxDepth)) {
            pathStream
                    .parallel() // 启用并行流，利用多核CPU
                    .filter(path -> !path.equals(rootPath)) // 排除根目录自己
                    .forEach(this::processFileFast); // 使用极速处理方法
        }
    }

    /**
     * 单层扫描（I/O优化版）
     */
    private void scanSingleLevel(Path directoryPath) throws IOException {
        try (Stream<Path> pathStream = Files.list(directoryPath)) {
            pathStream
                    .parallel() // 即使单层扫描也启用并行
                    .forEach(this::processFileFast);
        }
    }

    /**
     * 判断是否应该扫描该文件
     */
    private boolean shouldScanFile(Path filePath) {
        try {
            // 删除调试信息：控制台打印是性能杀手，已移除

            // 检查文件大小（大文件不再跳过，而是正常处理）
            long fileSize = Files.size(filePath);
            // 不跳过大文件，让FileCluster在addFile时统计它们
            // 大文件会被特殊标记但仍然包含在聚类结果中

            // 检查文件是否可读
            if (!Files.isReadable(filePath)) {
                return false;
            }

            // 检查路径是否在系统关键目录中（使用startsWith避免误杀）
            String pathStr = filePath.toString().toLowerCase();
            String normalizedPath = pathStr.replace("\\", "/");

            for (String criticalPath : SYSTEM_CRITICAL_PATHS) {
                String normalizedCritical = criticalPath.replace("\\", "/");
                // 只在系统盘根目录下才匹配，避免误杀普通项目
                if (normalizedPath.startsWith("c:/") &&
                    normalizedPath.contains("/" + normalizedCritical + "/")) {
                    return false;
                }
                if (normalizedPath.startsWith("c:\\") &&
                    normalizedPath.contains("\\" + normalizedCritical + "\\")) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            errors.add("检查文件时出错 " + filePath + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * ⚡️ 极速处理方法：一次I/O获取所有属性
     */
    private void processFileFast(Path filePath) {
        try {
            // 【关键】一次系统调用，获取所有元数据（大小, 时间, 类型）
            // 如果文件不可读或不存在，这里会直接抛异常，相当于自动过滤了
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);

            // 快速过滤（在内存中判断，不走I/O）
            if (!attrs.isRegularFile()) return; // 只处理普通文件

            long fileSize = attrs.size();
            // 纯字符串检查，极快
            if (isSystemFile(filePath.toString())) return;

            // 准备数据传给Engine，避免创建File对象
            String fileName = filePath.getFileName().toString();
            String parentPath = filePath.getParent().toString();
            long lastModified = attrs.lastModifiedTime().toMillis();

            // 生成Key（传入预读的时间，不再发生I/O）
            String clusterKey = clusteringEngine.generateClusterKey(fileName, parentPath, lastModified);

            // 聚类逻辑（保持不变）
            FileCluster cluster = clusters.computeIfAbsent(clusterKey, k -> {
                String[] parts = k.split("\\|");
                String pathSignature = parts.length > 0 ? parts[0] : "unknown";
                String extension = parts.length > 1 ? parts[1] : "__NO_EXT__";
                String timeBucket = parts.length > 2 ? parts[2] : "UNKNOWN";

                String clusterId = clusteringEngine.createClusterId(
                        pathSignature, extension, timeBucket, clusterCounter.getAndIncrement());

                return new FileCluster(clusterId, pathSignature, extension, timeBucket);
            });

            // 添加文件（传入Path字符串，不再转File）
            cluster.addFile(filePath.toString(), fileName, fileSize);

            // 更新统计
            scannedFiles.increment();
            totalSize.add(fileSize);

        } catch (IOException | SecurityException e) {
            // 这里的异常通常意味着文件不可读或没有权限，直接忽略即可，不用打印错误日志刷屏
        } catch (Exception e) {
            errors.add("Error processing " + filePath + ": " + e.getMessage());
        }
    }

    /**
     * 纯字符串检查，不走I/O
     */
    private boolean isSystemFile(String pathStr) {
        String lower = pathStr.toLowerCase().replace("\\", "/");
        for (String critical : SYSTEM_CRITICAL_PATHS) {
            // 简单的字符串匹配
            if (lower.contains("/" + critical + "/")) return true;
        }
        return false;
    }

    /**
     * 处理单个文件，进行聚类（保留兼容性）
     */
    private void processFile(Path filePath) {
        try {
            File file = filePath.toFile();
            String absolutePath = file.getAbsolutePath();
            String parentPath = file.getParent();

            // 生成聚类键
            String clusterKey = clusteringEngine.generateClusterKey(file, parentPath);

            // 获取或创建簇
            FileCluster cluster = clusters.computeIfAbsent(clusterKey, k -> {
                String[] parts = k.split("\\|");
                String pathSignature = parts.length > 0 ? parts[0] : "unknown";
                String extension = parts.length > 1 ? parts[1] : "__NO_EXT__";
                String timeBucket = parts.length > 2 ? parts[2] : "UNKNOWN";

                String clusterId = clusteringEngine.createClusterId(
                        pathSignature, extension, timeBucket, clusterCounter.getAndIncrement());

                return new FileCluster(clusterId, pathSignature, extension, timeBucket);
            });

            // 添加文件到簇
            long fileSize = file.length();
            cluster.addFile(absolutePath, file.getName(), fileSize);

            // 更新统计信息（使用原子操作保证并发安全）
            scannedFiles.increment();
            totalSize.add(fileSize);

            // 定期报告进度
            if (scannedFiles.sum() % 1000 == 0) {
                ClearAILogger.debug("已扫描 " + scannedFiles.sum() + " 个文件，生成 " + clusters.size() + " 个簇");
            }

        } catch (Exception e) {
            errors.add("处理文件时出错 " + filePath + ": " + e.getMessage());
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * 获取扫描统计信息
     */
    public String getScanStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("📊 扫描统计:\n");
        stats.append("  扫描文件数: ").append(scannedFiles.sum()).append("\n");
        stats.append("  生成簇数: ").append(clusters.size()).append("\n");
        stats.append("  总大小: ").append(formatFileSize(totalSize.sum())).append("\n");
        stats.append("  平均每簇文件数: ").append(clusters.isEmpty() ? 0 : scannedFiles.sum() / clusters.size()).append("\n");
        stats.append("  错误数: ").append(errors.size()).append("\n");
        return stats.toString();
    }

    /**
     * 扫描结果数据结构
     */
    public static class ScanResult {
        private final List<FileCluster> clusters;
        private final long totalFiles;
        private final long totalSize;
        private final List<String> errors;

        public ScanResult(List<FileCluster> clusters, long totalFiles, long totalSize, List<String> errors) {
            this.clusters = clusters;
            this.totalFiles = totalFiles;
            this.totalSize = totalSize;
            this.errors = errors;
        }

        public List<FileCluster> getClusters() { return clusters; }
        public long getTotalFiles() { return totalFiles; }
        public long getTotalSize() { return totalSize; }
        public List<String> getErrors() { return errors; }

        public int getClusterCount() { return clusters.size(); }

        /**
         * 按文件数量排序簇
         */
        public List<FileCluster> getClustersSortedByFileCount() {
            List<FileCluster> sorted = new ArrayList<>(clusters);
            sorted.sort((c1, c2) -> Integer.compare(c2.getFileCount(), c1.getFileCount()));
            return sorted;
        }

        /**
         * 按大小排序簇
         */
        public List<FileCluster> getClustersSortedBySize() {
            List<FileCluster> sorted = new ArrayList<>(clusters);
            sorted.sort((c1, c2) -> Long.compare(c2.getTotalSize(), c1.getTotalSize()));
            return sorted;
        }
    }
}