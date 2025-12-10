package com.hanpf.clearai.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

/**
 * 文件簇数据结构
 * 用于存储具有相同特征的文件集合
 */
public class FileCluster {

    private String clusterId;
    private String pathSignature;     // 路径特征：C:/Users/*/Downloads
    private String extension;         // 扩展名：.exe, .tmp, __NO_EXT__
    private String timeBucket;        // 时间分桶：HOT, WARM, COLD

    // 使用LongAdder实现无锁累加，大幅提升并发性能
    private final LongAdder fileCount = new LongAdder();
    private final LongAdder totalSize = new LongAdder();
    private final LongAdder largeFileCount = new LongAdder();
    private final LongAdder largeFilesSize = new LongAdder();

    private List<String> sampleFiles;     // 样本文件名
    private List<String> topPaths;        // 内存中只保留前50个文件路径用于展示
    private volatile boolean hasMoreFiles; // 使用volatile保证可见性
    private String aiDecision;        // AI决策：DELETE/REVIEW/KEEP
    private String aiReasoning;       // AI解释

    public FileCluster(String clusterId, String pathSignature, String extension, String timeBucket) {
        this.clusterId = clusterId;
        this.pathSignature = pathSignature;
        this.extension = extension;
        this.timeBucket = timeBucket;
        this.sampleFiles = new ArrayList<>();
        this.topPaths = new ArrayList<>();
        this.hasMoreFiles = false;
    }

    /**
     * 添加文件到簇中（无锁优化版）
     */
    public void addFile(String filePath, String fileName, long fileSize) {
        // 无锁原子操作，性能极高
        this.fileCount.increment();
        this.totalSize.add(fileSize);

        // 识别大文件 (>100MB)
        if (fileSize > 100 * 1024 * 1024) {
            this.largeFileCount.increment();
            this.largeFilesSize.add(fileSize);
        }

        // 只在涉及List操作时才同步，且概率很低
        if (!this.hasMoreFiles || this.sampleFiles.size() < 5) {
            synchronized (this) {
                // 双重检查，避免重复添加
                if (this.topPaths.size() < 50) {
                    this.topPaths.add(filePath);
                } else {
                    this.hasMoreFiles = true;
                }

                if (this.sampleFiles.size() < 5) {
                    this.sampleFiles.add(fileName);
                }
            }
        }
    }

    /**
     * 获取平均文件大小
     */
    public double getAverageFileSize() {
        long count = fileCount.sum();
        return count > 0 ? (double) totalSize.sum() / count : 0;
    }

    /**
     * 获取格式化的大小信息
     */
    public String getFormattedTotalSize() {
        return formatFileSize(totalSize.sum());
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * 生成增强的簇描述信息
     */
    public String getDescription() {
        long count = fileCount.sum();
        long largeCount = largeFileCount.sum();
        long largeSize = largeFilesSize.sum();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("路径: %s | 类型: %s | 时间: %s | 文件数: %d | 大小: %s",
                pathSignature, extension, timeBucket, count, getFormattedTotalSize()));

        // 添加大文件信息
        if (largeCount > 0) {
            sb.append(String.format(" | 大文件: %d个 (%s)",
                    largeCount, formatFileSize(largeSize)));
        }

        // 添加更多文件标记
        if (hasMoreFiles) {
            sb.append(" | +更多文件");
        }

        return sb.toString();
    }

    // Getters and Setters
    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getPathSignature() { return pathSignature; }
    public void setPathSignature(String pathSignature) { this.pathSignature = pathSignature; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public String getTimeBucket() { return timeBucket; }
    public void setTimeBucket(String timeBucket) { this.timeBucket = timeBucket; }

    public int getFileCount() { return (int) fileCount.sum(); }
    public void setFileCount(int fileCount) { this.fileCount.reset(); this.fileCount.add(fileCount); }

    public long getTotalSize() { return totalSize.sum(); }
    public void setTotalSize(long totalSize) { this.totalSize.reset(); this.totalSize.add(totalSize); }

    public List<String> getSampleFiles() { return sampleFiles; }
    public void setSampleFiles(List<String> sampleFiles) { this.sampleFiles = sampleFiles; }

    public List<String> getTopPaths() { return topPaths; }
    public void setTopPaths(List<String> topPaths) { this.topPaths = topPaths; }

    public boolean hasMoreFiles() { return hasMoreFiles; }
    public void setHasMoreFiles(boolean hasMoreFiles) { this.hasMoreFiles = hasMoreFiles; }

    public long getLargeFileCount() { return largeFileCount.sum(); }
    public void setLargeFileCount(long largeFileCount) { this.largeFileCount.reset(); this.largeFileCount.add(largeFileCount); }

    public long getLargeFilesSize() { return largeFilesSize.sum(); }
    public void setLargeFilesSize(long largeFilesSize) { this.largeFilesSize.reset(); this.largeFilesSize.add(largeFilesSize); }

    public String getAiDecision() { return aiDecision; }
    public void setAiDecision(String aiDecision) { this.aiDecision = aiDecision; }

    public String getAiReasoning() { return aiReasoning; }
    public void setAiReasoning(String aiReasoning) { this.aiReasoning = aiReasoning; }

    @Override
    public String toString() {
        return "FileCluster{" +
                "clusterId='" + clusterId + '\'' +
                ", pathSignature='" + pathSignature + '\'' +
                ", extension='" + extension + '\'' +
                ", timeBucket='" + timeBucket + '\'' +
                ", fileCount=" + fileCount +
                ", totalSize=" + getFormattedTotalSize() +
                '}';
    }
}