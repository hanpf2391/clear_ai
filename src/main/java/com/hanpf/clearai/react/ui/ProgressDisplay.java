package com.hanpf.clearai.react.ui;

import com.hanpf.clearai.cli.cleaning.react.ProgressTracker;
import com.hanpf.clearai.cli.cleaning.react.ScanProgress;

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 进度显示界面
 * 提供实时的进度显示和用户交互
 */
public class ProgressDisplay implements ProgressTracker.ProgressListener {

    private final Scanner scanner;
    private final AtomicBoolean displayActive;
    private Thread displayThread;
    private ProgressTracker currentTracker;
    private boolean showDetails = false;

    public ProgressDisplay() {
        this.scanner = new Scanner(System.in);
        this.displayActive = new AtomicBoolean(false);
    }

    /**
     * 启动实时进度显示
     *
     * @param tracker 进度追踪器
     */
    public void startRealTimeProgress(ProgressTracker tracker) {
        if (displayActive.get()) {
            stopRealTimeProgress();
        }

        this.currentTracker = tracker;
        tracker.addListener(this);

        displayActive.set(true);
        displayThread = new Thread(this::displayLoop, "ProgressDisplay");
        displayThread.setDaemon(true);
        displayThread.start();

        System.out.println("🔄 启动进度显示 (按 'd' 切换详情，按 'q' 退出显示)");
    }

    /**
     * 停止实时进度显示
     */
    public void stopRealTimeProgress() {
        displayActive.set(false);

        if (displayThread != null) {
            displayThread.interrupt();
            try {
                displayThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (currentTracker != null) {
            currentTracker.removeListener(this);
        }

        displayThread = null;
        currentTracker = null;
    }

    /**
     * 显示循环
     */
    private void displayLoop() {
        while (displayActive.get() && !Thread.currentThread().isInterrupted()) {
            try {
                clearScreen();
                displayProgress(currentTracker);

                // 检查用户输入
                checkUserInput();

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                // 显示异常但继续运行
                System.err.println("进度显示异常: " + e.getMessage());
            }
        }
    }

    /**
     * 检查用户输入
     */
    private void checkUserInput() {
        try {
            if (System.in.available() > 0) {
                String input = scanner.nextLine().trim().toLowerCase();

                switch (input) {
                    case "d":
                        showDetails = !showDetails;
                        System.out.println(showDetails ? "🔍 显示详细进度" : "📊 显示简单进度");
                        break;
                    case "q":
                        stopRealTimeProgress();
                        System.out.println("📊 已停止进度显示");
                        break;
                    case "h":
                    case "help":
                        showHelp();
                        break;
                }
            }
        } catch (Exception e) {
            // 忽略输入异常
        }
    }

    /**
     * 显示进度信息
     */
    public void displayProgress(ProgressTracker tracker) {
        if (tracker == null) {
            System.out.println("📊 无进度信息");
            return;
        }

        System.out.println("📊 多目录扫描进度");
        System.out.println("=".repeat(60));

        // 显示总体摘要
        System.out.println(tracker.getDetailedSummary());
        System.out.println();

        // 显示进度条
        displayProgressBar(tracker.getOverallProgress());
        System.out.println();

        // 显示各个路径的进度
        displayPathProgresses(tracker.getAllProgress());

        // 显示控制提示
        displayControls();

        System.out.println();
    }

    /**
     * 显示进度条
     */
    private void displayProgressBar(double progress) {
        int totalBars = 50;
        int filledBars = (int) (progress * totalBars);

        System.out.print("进度条: [");
        for (int i = 0; i < totalBars; i++) {
            if (i < filledBars) {
                System.out.print("█");
            } else {
                System.out.print(" ");
            }
        }
        System.out.printf("] %.1f%%\n", progress * 100);
    }

    /**
     * 显示各个路径的进度
     */
    private void displayPathProgresses(Map<String, ScanProgress> progressMap) {
        if (progressMap.isEmpty()) {
            System.out.println("📁 暂无扫描任务");
            return;
        }

        System.out.println("📁 各目录扫描进度:");
        System.out.println("-".repeat(60));

        int index = 1;
        for (Map.Entry<String, ScanProgress> entry : progressMap.entrySet()) {
            String path = entry.getKey();
            ScanProgress progress = entry.getValue();

            // 路径显示名称
            String displayName = getShortPath(path, 40);

            // 状态图标
            String statusIcon = getStatusIcon(progress);

            System.out.printf("%2d. %s %s\n", index++, statusIcon, displayName);

            if (showDetails || progress.hasError()) {
                // 显示详细进度
                System.out.printf("    %s\n", progress.getProgressInfo());

                // 显示小进度条
                double pathProgress = progress.getCompletionPercentage();
                System.out.printf("    [%-20s] %.1f%%\n",
                    getProgressBar(pathProgress, 20), pathProgress * 100);

                if (progress.hasError()) {
                    System.out.printf("    ❌ 错误: %s\n", progress.getError());
                }
            } else {
                // 显示简单进度
                System.out.printf("    %s\n", progress.getStatus());
            }

            System.out.println();
        }
    }

    /**
     * 获取状态图标
     */
    private String getStatusIcon(ScanProgress progress) {
        if (progress.hasError()) {
            return "❌";
        } else if (progress.isCompleted()) {
            return "✅";
        } else if (progress.getScannedFiles() > 0) {
            return "🔄";
        } else {
            return "⏳";
        }
    }

    /**
     * 获取简短的路径名称
     */
    private String getShortPath(String path, int maxLength) {
        if (path == null || path.length() <= maxLength) {
            return path;
        }

        // 尝试保留文件名部分
        String fileName = new java.io.File(path).getName();
        if (fileName.length() <= maxLength - 3) {
            return ".../" + fileName;
        }

        // 如果文件名也太长，截断
        return fileName.length() > maxLength ?
            fileName.substring(0, maxLength - 3) + "..." : fileName;
    }

    /**
     * 获取进度条字符串
     */
    private String getProgressBar(double progress, int length) {
        int filled = (int) (progress * length);
        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < length; i++) {
            bar.append(i < filled ? "=" : " ");
        }

        return bar.toString();
    }

    /**
     * 显示控制提示
     */
    private void displayControls() {
        System.out.println("控制: [d] 切换详情 | [q] 退出显示 | [h] 帮助");
    }

    /**
     * 显示帮助信息
     */
    private void showHelp() {
        System.out.println();
        System.out.println("🎮 进度显示控制帮助:");
        System.out.println("=".repeat(40));
        System.out.println("d - 切换详细/简单显示模式");
        System.out.println("q - 退出进度显示");
        System.out.println("h - 显示此帮助信息");
        System.out.println();
        System.out.println("状态图标说明:");
        System.out.println("✅ - 扫描完成");
        System.out.println("❌ - 扫描失败");
        System.out.println("🔄 - 正在扫描");
        System.out.println("⏳ - 等待扫描");
        System.out.println();
        System.out.println("按回车键继续...");
        try {
            scanner.nextLine();
        } catch (Exception e) {
            // 忽略
        }
    }

    /**
     * 清屏
     */
    private void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // 如果清屏失败，输出换行符
            System.out.println("\n".repeat(50));
        }
    }

    /**
     * 显示最终结果摘要
     */
    public void displayFinalSummary(ProgressTracker tracker, String scanSummary) {
        System.out.println();
        System.out.println("🎉 扫描完成");
        System.out.println("=".repeat(60));

        // 显示进度摘要
        System.out.println(tracker.getDetailedSummary());
        System.out.println();

        // 显示扫描摘要
        System.out.println("📋 扫描结果摘要:");
        System.out.println(scanSummary);

        System.out.println();
    }

    /**
     * 显示错误信息
     */
    public void displayError(String error) {
        System.out.println();
        System.out.println("❌ 错误");
        System.out.println("=".repeat(60));
        System.out.println(error);
        System.out.println();
    }

    /**
     * 获取当前是否显示详情
     */
    public boolean isShowDetails() {
        return showDetails;
    }

    /**
     * 设置是否显示详情
     */
    public void setShowDetails(boolean showDetails) {
        this.showDetails = showDetails;
    }

    /**
     * 检查是否正在显示
     */
    public boolean isDisplaying() {
        return displayActive.get();
    }

    // 进度监听器接口实现
    @Override
    public void onProgressUpdate(ProgressTracker tracker) {
        // 进度更新时会自动在显示循环中处理
    }

    @Override
    public void onPathProgressUpdate(String path, ScanProgress progress) {
        // 路径进度更新时会自动在显示循环中处理
    }
}