package com.hanpf.clearai.cli.cleaning.react;

import com.hanpf.clearai.cli.cleaning.models.FileInfo;
import com.hanpf.clearai.cli.cleaning.models.ScanOptions;
import com.hanpf.clearai.utils.WhitelistManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 多目录扫描器
 * 支持并发扫描多个目录，实时更新进度
 */
public class MultiDirectoryScanner {

    private final ExecutorService executorService;
    private final ProgressTracker progressTracker;
    private final WhitelistManager whitelistManager;

    public MultiDirectoryScanner() {
        this.executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "MultiDirectoryScanner-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            }
        );
        this.progressTracker = new ProgressTracker();
        this.whitelistManager = WhitelistManager.getInstance();
    }

    /**
     * 扫描多个目录
     *
     * @param paths 要扫描的路径列表
     * @param options 扫描选项
     * @return 扫描结果
     */
    public ScanResult scanMultipleDirectories(List<String> paths, ScanOptions options) {
        if (paths == null || paths.isEmpty()) {
            return new ScanResult(new ArrayList<>(), "没有指定要扫描的路径", 0);
        }

        // 初始化进度追踪器
        progressTracker.initialize(paths);

        List<CompletableFuture<DirectoryScanResult>> futures = new ArrayList<>();

        for (String path : paths) {
            CompletableFuture<DirectoryScanResult> future = CompletableFuture.supplyAsync(
                () -> scanDirectory(path, options), executorService
            );
            futures.add(future);
        }

        // 等待所有扫描完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );

        try {
            allFutures.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return new ScanResult(new ArrayList<>(), "扫描过程中断: " + e.getMessage(), 0);
        }

        // 聚合结果
        return aggregateResults(futures, paths);
    }

    /**
     * 扫描单个目录
     *
     * @param path 目录路径
     * @param options 扫描选项
     * @return 目录扫描结果
     */
    private DirectoryScanResult scanDirectory(String path, ScanOptions options) {
        progressTracker.updateProgress(path, "开始扫描...");

        ScanProgress progress = progressTracker.getProgress(path);
        long startTime = System.currentTimeMillis();

        try {
            List<FileInfo> files = scanFiles(path, options, progress);

            long elapsedTime = System.currentTimeMillis() - startTime;
            progress.complete();

            String summary = String.format("扫描完成，找到 %d 个文件，耗时 %d ms",
                files.size(), elapsedTime);
            progressTracker.updateProgress(path, summary);

            return new DirectoryScanResult(path, files, progress);

        } catch (Exception e) {
            String errorMsg = "扫描失败: " + e.getMessage();
            progress.fail(errorMsg);
            progressTracker.updateProgress(path, errorMsg);

            return new DirectoryScanResult(path, new ArrayList<>(), progress);
        }
    }

    /**
     * 递归扫描文件
     *
     * @param dirPath 目录路径
     * @param options 扫描选项
     * @param progress 进度追踪
     * @return 文件信息列表
     */
    private List<FileInfo> scanFiles(String dirPath, ScanOptions options, ScanProgress progress) {
        List<FileInfo> files = new ArrayList<>();
        File dir = new File(dirPath);

        if (!dir.exists() || !dir.isDirectory()) {
            return files;
        }

        try {
            File[] fileList = dir.listFiles();
            if (fileList == null) {
                return files;
            }

            // 预估总文件数（用于进度显示）
            long estimatedTotal = countFilesRecursively(dir);
            progress.updateProgress(0, estimatedTotal, 0, 0);

            processFiles(fileList, options, progress, files);

        } catch (Exception e) {
            throw new RuntimeException("扫描目录失败: " + e.getMessage(), e);
        }

        return files;
    }

    /**
     * 处理文件列表
     */
    private void processFiles(File[] files, ScanOptions options, ScanProgress progress, List<FileInfo> result) {
        long totalSize = 0;
        long processedFiles = 0;

        Queue<File> fileQueue = new LinkedList<>(Arrays.asList(files));

        while (!fileQueue.isEmpty() && !Thread.currentThread().isInterrupted()) {
            File file = fileQueue.poll();

            if (file == null) {
                continue;
            }

            if (file.isDirectory()) {
                // 处理子目录
                File[] subFiles = file.listFiles();
                if (subFiles != null) {
                    for (File subFile : subFiles) {
                        fileQueue.offer(subFile);
                    }
                }
            } else {
                // 处理文件
                if (shouldIncludeFile(file, options)) {
                    FileInfo fileInfo = createFileInfo(file);
                    result.add(fileInfo);
                    totalSize += fileInfo.getSize();
                }
                processedFiles++;

                // 定期更新进度
                if (processedFiles % 100 == 0) {
                    progress.updateProgress(processedFiles, progress.getTotalFiles(),
                        totalSize, progress.getTotalSize());
                }
            }
        }

        // 最终更新进度
        progress.updateProgress(processedFiles, processedFiles, totalSize, totalSize);
    }

    /**
     * 递归计算文件数量
     */
    private long countFilesRecursively(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return 0;
        }

        long count = 0;
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += countFilesRecursively(file);
                } else {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * 判断文件是否应该包含在扫描结果中
     */
    private boolean shouldIncludeFile(File file, ScanOptions options) {
        if (options == null) {
            return true;
        }

        // 首先检查白名单（安全第一）
        if (whitelistManager.isWhitelisted(file.getAbsolutePath())) {
            return false; // 白名单中的文件不包含在扫描结果中
        }

        // 检查文件大小
        if (options.getMinFileSize() > 0 && file.length() < options.getMinFileSize()) {
            return false;
        }
        if (options.getMaxFileSize() > 0 && file.length() > options.getMaxFileSize()) {
            return false;
        }

        // 检查文件扩展名
        if (options.getIncludeExtensions() != null && !options.getIncludeExtensions().isEmpty()) {
            String fileName = file.getName().toLowerCase();
            boolean matches = options.getIncludeExtensions().stream()
                .anyMatch(ext -> fileName.endsWith("." + ext.toLowerCase()));
            if (!matches) {
                return false;
            }
        }

        // 检查排除的扩展名
        if (options.getExcludeExtensions() != null && !options.getExcludeExtensions().isEmpty()) {
            String fileName = file.getName().toLowerCase();
            boolean excluded = options.getExcludeExtensions().stream()
                .anyMatch(ext -> fileName.endsWith("." + ext.toLowerCase()));
            if (excluded) {
                return false;
            }
        }

        return true;
    }

    /**
     * 创建文件信息对象
     */
    private FileInfo createFileInfo(File file) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(file.getName());
        fileInfo.setPath(file.getAbsolutePath());
        fileInfo.setSize(file.length());
        fileInfo.setLastModified(file.lastModified());
        fileInfo.setDirectory(file.isDirectory());

        try {
            Path filePath = Paths.get(file.getAbsolutePath());
            fileTypeDetection(fileInfo, Files.probeContentType(filePath));
        } catch (IOException e) {
            fileTypeDetection(fileInfo, null);
        }

        return fileInfo;
    }

    /**
     * 文件类型检测
     */
    private void fileTypeDetection(FileInfo fileInfo, String mimeType) {
        if (mimeType != null) {
            fileInfo.setType(mimeType);
        } else {
            // 基于文件扩展名的简单类型检测
            String fileName = fileInfo.getName().toLowerCase();
            if (fileName.endsWith(".txt") || fileName.endsWith(".log")) {
                fileInfo.setType("text/plain");
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".png")) {
                fileInfo.setType("image");
            } else if (fileName.endsWith(".mp4") || fileName.endsWith(".avi")) {
                fileInfo.setType("video");
            } else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav")) {
                fileInfo.setType("audio");
            } else if (fileName.endsWith(".pdf")) {
                fileInfo.setType("application/pdf");
            } else if (fileName.endsWith(".zip") || fileName.endsWith(".rar")) {
                fileInfo.setType("archive");
            } else {
                fileInfo.setType("unknown");
            }
        }
    }

    /**
     * 聚合扫描结果
     */
    private ScanResult aggregateResults(List<CompletableFuture<DirectoryScanResult>> futures,
                                      List<String> originalPaths) {
        List<FileInfo> allFiles = new ArrayList<>();
        StringBuilder summary = new StringBuilder();
        long totalSize = 0;
        int successCount = 0;
        int failureCount = 0;

        for (CompletableFuture<DirectoryScanResult> future : futures) {
            try {
                DirectoryScanResult result = future.get();
                allFiles.addAll(result.getFiles());
                totalSize += result.getFiles().stream().mapToLong(FileInfo::getSize).sum();

                if (result.getProgress().hasError()) {
                    failureCount++;
                    summary.append(String.format("❌ %s: %s\n",
                        result.getDirectory(), result.getProgress().getError()));
                } else {
                    successCount++;
                }
            } catch (Exception e) {
                failureCount++;
                summary.append(String.format("❌ 扫描异常: %s\n", e.getMessage()));
            }
        }

        // 按大小排序
        allFiles.sort((a, b) -> Long.compare(b.getSize(), a.getSize()));

        String finalSummary = String.format(
            "扫描完成: %d个目录成功, %d个目录失败, 总计 %d 个文件, 占用空间 %s\n%s",
            successCount, failureCount, allFiles.size(), formatSize(totalSize), summary.toString()
        );

        return new ScanResult(allFiles, finalSummary, totalSize);
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
     * 获取进度追踪器
     */
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    /**
     * 关闭扫描器
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 扫描结果类
     */
    public static class ScanResult {
        private final List<FileInfo> files;
        private final String summary;
        private final long totalSize;

        public ScanResult(List<FileInfo> files, String summary, long totalSize) {
            this.files = files;
            this.summary = summary;
            this.totalSize = totalSize;
        }

        public List<FileInfo> getFiles() { return files; }
        public String getSummary() { return summary; }
        public long getTotalSize() { return totalSize; }
    }

    /**
     * 目录扫描结果类
     */
    public static class DirectoryScanResult {
        private final String directory;
        private final List<FileInfo> files;
        private final ScanProgress progress;

        public DirectoryScanResult(String directory, List<FileInfo> files, ScanProgress progress) {
            this.directory = directory;
            this.files = files;
            this.progress = progress;
        }

        public String getDirectory() { return directory; }
        public List<FileInfo> getFiles() { return files; }
        public ScanProgress getProgress() { return progress; }
    }
}