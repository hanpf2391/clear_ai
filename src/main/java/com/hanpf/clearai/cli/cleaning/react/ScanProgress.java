package com.hanpf.clearai.cli.cleaning.react;

/**
 * 扫描进度信息类
 */
public class ScanProgress {

    private String status;
    private long totalFiles = 0;
    private long scannedFiles = 0;
    private long totalSize = 0;
    private long scannedSize = 0;
    private long startTime;
    private long endTime = 0;
    private String error = null;

    public ScanProgress() {
        this.startTime = System.currentTimeMillis();
        this.status = "准备中";
    }

    public ScanProgress(String status) {
        this();
        this.status = status;
    }

    /**
     * 更新进度状态
     *
     * @param status 新状态
     */
    public void update(String status) {
        this.status = status;
    }

    /**
     * 更新扫描进度
     *
     * @param scannedFiles 已扫描文件数
     * @param totalFiles 总文件数
     * @param scannedSize 已扫描大小
     * @param totalSize 总大小
     */
    public void updateProgress(long scannedFiles, long totalFiles,
                              long scannedSize, long totalSize) {
        this.scannedFiles = scannedFiles;
        this.totalFiles = totalFiles;
        this.scannedSize = scannedSize;
        this.totalSize = totalSize;
    }

    /**
     * 标记为完成
     */
    public void complete() {
        this.endTime = System.currentTimeMillis();
        this.status = "扫描完成";
    }

    /**
     * 标记为失败
     *
     * @param error 错误信息
     */
    public void fail(String error) {
        this.endTime = System.currentTimeMillis();
        this.status = "扫描失败";
        this.error = error;
    }

    /**
     * 获取完成百分比
     *
     * @return 完成百分比 (0.0-1.0)
     */
    public double getCompletionPercentage() {
        if (totalFiles == 0) {
            return status.equals("扫描完成") ? 1.0 : 0.0;
        }
        return (double) scannedFiles / totalFiles;
    }

    /**
     * 获取扫描速度（文件/秒）
     *
     * @return 扫描速度
     */
    public double getScanSpeed() {
        long elapsed = getElapsedTime();
        if (elapsed == 0) {
            return 0;
        }
        return (double) scannedFiles / (elapsed / 1000.0);
    }

    /**
     * 获取已用时间（毫秒）
     *
     * @return 已用时间
     */
    public long getElapsedTime() {
        long end = endTime > 0 ? endTime : System.currentTimeMillis();
        return end - startTime;
    }

    /**
     * 格式化文件大小
     *
     * @param bytes 字节数
     * @return 格式化的大小字符串
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
     * 获取格式化的进度信息
     *
     * @return 进度信息字符串
     */
    public String getProgressInfo() {
        StringBuilder info = new StringBuilder();
        info.append(status);

        if (totalFiles > 0) {
            info.append(String.format(" (%d/%d 文件, %s/%s)",
                scannedFiles, totalFiles,
                formatSize(scannedSize), formatSize(totalSize)));
        }

        return info.toString();
    }

    // Getters
    public String getStatus() {
        return status;
    }

    public long getTotalFiles() {
        return totalFiles;
    }

    public long getScannedFiles() {
        return scannedFiles;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public long getScannedSize() {
        return scannedSize;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getError() {
        return error;
    }

    public boolean isCompleted() {
        return "扫描完成".equals(status) || "扫描失败".equals(status);
    }

    public boolean hasError() {
        return error != null;
    }
}