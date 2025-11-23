package com.hanpf.clearai.cli.cleaning.react;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 进度追踪系统
 * 用于追踪多个目录的扫描进度，支持监听器模式
 */
public class ProgressTracker {

    private final Map<String, ScanProgress> progressMap = new ConcurrentHashMap<>();
    private final List<ProgressListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean isRunning = false;
    private LocalDateTime startTime;

    /**
     * 初始化进度追踪器
     *
     * @param paths 要追踪的路径列表
     */
    public void initialize(List<String> paths) {
        progressMap.clear();
        startTime = LocalDateTime.now();
        isRunning = true;

        for (String path : paths) {
            progressMap.put(path, new ScanProgress("等待扫描"));
        }

        notifyProgressUpdate();
    }

    /**
     * 更新单个路径的进度
     *
     * @param path 路径
     * @param status 状态描述
     */
    public void updateProgress(String path, String status) {
        ScanProgress progress = progressMap.get(path);
        if (progress != null) {
            progress.update(status);
            notifyProgressUpdate(path, progress);
        }
    }

    /**
     * 更新单个路径的进度和文件统计
     *
     * @param path 路径
     * @param scannedFiles 已扫描文件数
     * @param totalFiles 总文件数
     * @param scannedSize 已扫描大小
     * @param totalSize 总大小
     */
    public void updateProgress(String path, long scannedFiles, long totalFiles,
                              long scannedSize, long totalSize) {
        ScanProgress progress = progressMap.get(path);
        if (progress != null) {
            progress.updateProgress(scannedFiles, totalFiles, scannedSize, totalSize);
            notifyProgressUpdate(path, progress);
        }
    }

    /**
     * 标记路径扫描完成
     *
     * @param path 路径
     */
    public void completePath(String path) {
        ScanProgress progress = progressMap.get(path);
        if (progress != null) {
            progress.complete();
            notifyProgressUpdate(path, progress);
        }
    }

    /**
     * 标记路径扫描失败
     *
     * @param path 路径
     * @param error 错误信息
     */
    public void failPath(String path, String error) {
        ScanProgress progress = progressMap.get(path);
        if (progress != null) {
            progress.fail(error);
            notifyProgressUpdate(path, progress);
        }
    }

    /**
     * 标记所有扫描完成
     */
    public void completeAll() {
        isRunning = false;
        for (ScanProgress progress : progressMap.values()) {
            if (!progress.isCompleted()) {
                progress.complete();
            }
        }
        notifyProgressUpdate();
    }

    /**
     * 获取总体进度百分比
     *
     * @return 进度百分比 (0.0-1.0)
     */
    public double getOverallProgress() {
        if (progressMap.isEmpty()) {
            return isRunning ? 0.0 : 1.0;
        }

        return progressMap.values().stream()
            .mapToDouble(ScanProgress::getCompletionPercentage)
            .average()
            .orElse(0.0);
    }

    /**
     * 获取已完成的路径数量
     *
     * @return 已完成数量
     */
    public int getCompletedCount() {
        return (int) progressMap.values().stream()
            .filter(ScanProgress::isCompleted)
            .count();
    }

    /**
     * 获取有错误的路径数量
     *
     * @return 错误数量
     */
    public int getErrorCount() {
        return (int) progressMap.values().stream()
            .filter(ScanProgress::hasError)
            .count();
    }

    /**
     * 获取总文件数
     *
     * @return 总文件数
     */
    public long getTotalFiles() {
        return progressMap.values().stream()
            .mapToLong(ScanProgress::getTotalFiles)
            .sum();
    }

    /**
     * 获取已扫描文件数
     *
     * @return 已扫描文件数
     */
    public long getScannedFiles() {
        return progressMap.values().stream()
            .mapToLong(ScanProgress::getScannedFiles)
            .sum();
    }

    /**
     * 获取总体文件大小
     *
     * @return 总文件大小
     */
    public long getTotalSize() {
        return progressMap.values().stream()
            .mapToLong(ScanProgress::getTotalSize)
            .sum();
    }

    /**
     * 获取已扫描文件大小
     *
     * @return 已扫描文件大小
     */
    public long getScannedSize() {
        return progressMap.values().stream()
            .mapToLong(ScanProgress::getScannedSize)
            .sum();
    }

    /**
     * 获取所有路径的进度
     *
     * @return 进度映射
     */
    public Map<String, ScanProgress> getAllProgress() {
        return new HashMap<>(progressMap);
    }

    /**
     * 获取指定路径的进度
     *
     * @param path 路径
     * @return 进度信息
     */
    public ScanProgress getProgress(String path) {
        return progressMap.get(path);
    }

    /**
     * 获取进度摘要
     *
     * @return 摘要字符串
     */
    public String getProgressSummary() {
        int total = progressMap.size();
        int completed = getCompletedCount();
        int errors = getErrorCount();
        int inProgress = total - completed - errors;

        return String.format("总体进度: %.1f%% (%d/%d 完成, %d 进行中, %d 错误)",
            getOverallProgress() * 100, completed, total, inProgress, errors);
    }

    /**
     * 获取详细信息摘要
     *
     * @return 详细摘要
     */
    public String getDetailedSummary() {
        StringBuilder summary = new StringBuilder();

        if (startTime != null) {
            summary.append("开始时间: ").append(startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("\n");
        }

        summary.append(getProgressSummary()).append("\n");

        long scannedFiles = getScannedFiles();
        long totalFiles = getTotalFiles();
        long scannedSize = getScannedSize();
        long totalSize = getTotalSize();

        if (totalFiles > 0) {
            summary.append(String.format("文件进度: %d/%d (%.1f%%)\n",
                scannedFiles, totalFiles, (double) scannedFiles / totalFiles * 100));
        }

        if (totalSize > 0) {
            summary.append(String.format("大小进度: %s/%s (%.1f%%)\n",
                formatSize(scannedSize), formatSize(totalSize),
                (double) scannedSize / totalSize * 100));
        }

        return summary.toString();
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

    /**
     * 检查是否所有扫描都已完成
     *
     * @return 是否完成
     */
    public boolean isComplete() {
        if (progressMap.isEmpty()) {
            return true;
        }

        return progressMap.values().stream().allMatch(ScanProgress::isCompleted);
    }

    /**
     * 检查是否正在运行
     *
     * @return 是否运行中
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * 添加进度监听器
     *
     * @param listener 监听器
     */
    public void addListener(ProgressListener listener) {
        listeners.add(listener);
    }

    /**
     * 移除进度监听器
     *
     * @param listener 监听器
     */
    public void removeListener(ProgressListener listener) {
        listeners.remove(listener);
    }

    /**
     * 通知所有监听器
     */
    private void notifyProgressUpdate() {
        for (ProgressListener listener : listeners) {
            try {
                listener.onProgressUpdate(this);
            } catch (Exception e) {
                // 忽略监听器异常
            }
        }
    }

    /**
     * 通知特定路径的进度更新
     */
    private void notifyProgressUpdate(String path, ScanProgress progress) {
        for (ProgressListener listener : listeners) {
            try {
                listener.onPathProgressUpdate(path, progress);
            } catch (Exception e) {
                // 忽略监听器异常
            }
        }
    }

    /**
     * 重置追踪器
     */
    public void reset() {
        progressMap.clear();
        listeners.clear();
        isRunning = false;
        startTime = null;
    }

    /**
     * 进度监听器接口
     */
    public interface ProgressListener {
        /**
         * 总体进度更新
         */
        default void onProgressUpdate(ProgressTracker tracker) {}

        /**
         * 特定路径进度更新
         */
        default void onPathProgressUpdate(String path, ScanProgress progress) {}
    }
}