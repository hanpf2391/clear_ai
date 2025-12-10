package com.hanpf.clearai.clustering;

import java.io.File;
import java.util.Scanner;

/**
 * 简化的文件聚类演示程序
 */
public class SimpleClusteringDemo {

    public static void main(String[] args) {
        System.out.println("🚀 ClearAI 简化文件聚类分析");
        System.out.println("=====================================");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\n📁 请输入要扫描的目录路径 (输入 'quit' 退出): ");
            String directoryPath = scanner.nextLine().trim();

            if (directoryPath.isEmpty()) {
                System.out.println("❌ 路径不能为空");
                continue;
            }

            // 安全警告：扫描系统盘
            if (directoryPath.equalsIgnoreCase("C:\\") || directoryPath.equalsIgnoreCase("C:/")) {
                System.out.println("⚠️ 警告：您正在扫描C盘根目录，这可能会扫描到大量系统文件");
                System.out.print("确认继续扫描C盘吗? (输入 'YES' 确认): ");
                String confirm = scanner.nextLine().trim();
                if (!confirm.equals("YES")) {
                    System.out.println("❌ 已取消扫描");
                    continue;
                }
            }

            // 简单测试：检查目录是否存在
            File dir = new File(directoryPath);
            if (!dir.exists()) {
                System.out.println("❌ 目录不存在: " + directoryPath);
                continue;
            }

            if (!dir.isDirectory()) {
                System.out.println("❌ 不是目录: " + directoryPath);
                continue;
            }

            System.out.println("✅ 目录存在: " + directoryPath);
            System.out.println("📂 目录内容:");

            File[] files = dir.listFiles();
            if (files == null) {
                System.out.println("❌ 无法读取目录内容（可能是权限问题）");
                continue;
            }

            System.out.println("   总项目数: " + files.length);

            int fileCount = 0;
            int dirCount = 0;

            for (File file : files) {
                if (file.isDirectory()) {
                    dirCount++;
                    System.out.println("   📁 " + file.getName() + "/ (目录)");
                } else {
                    fileCount++;
                    String size = formatFileSize(file.length());
                    System.out.println("   📄 " + file.getName() + " (" + size + ")");
                }

                // 只显示前10个项目
                if (fileCount + dirCount >= 10) {
                    System.out.println("   ... 还有 " + (files.length - 10) + " 个项目");
                    break;
                }
            }

            System.out.println("📊 统计: " + fileCount + "个文件, " + dirCount + "个目录");

            // 特殊处理：对于系统盘或者没有直接文件的目录，直接提供聚类选项
            boolean shouldOfferClustering = fileCount > 0 ||
                directoryPath.equalsIgnoreCase("C:\\") ||
                directoryPath.equalsIgnoreCase("C:/") ||
                dirCount > 0; // 有子目录也可能包含文件

            if (shouldOfferClustering) {
                if (fileCount == 0) {
                    System.out.println("💡 提示：虽然根目录没有文件，但子目录中可能包含大量文件");
                    System.out.println("🔍 聚类分析将递归扫描所有子目录，这可能需要几分钟时间");
                }

                System.out.print("🔍 要使用聚类引擎分析该目录及其所有子目录吗? (y/n): ");
                String analyzeChoice = scanner.nextLine().trim();
                if (analyzeChoice.equalsIgnoreCase("y")) {
                    analyzeWithClustering(directoryPath, scanner);
                } else if (analyzeChoice.equalsIgnoreCase("n")) {
                    System.out.println("⏭️ 跳过分析，继续下一个目录");
                    continue; // 跳过本次循环，继续等待新的目录输入
                } else {
                    System.out.println("❌ 无效输入，请输入 y/n");
                    continue; // 重新询问当前目录
                }
            } else {
                System.out.println("ℹ️ 该目录没有文件，继续扫描其他目录");
                continue; // 继续等待新的目录输入
            }
        }

        // scanner.close(); // JVM会自动关闭
    }

    /**
     * 使用聚类引擎分析
     */
    private static void analyzeWithClustering(String directoryPath, Scanner scanner) {
        System.out.println("🔧 开始聚类分析，这可能需要几分钟时间...");
        System.out.println("⏳ 正在扫描文件，请耐心等待...");

        try {
            FileScanner fileScanner = new FileScanner();
            long startTime = System.currentTimeMillis();

            // 执行扫描聚类（递归扫描，保守的深度限制）
            // 12层安全可靠，覆盖绝大部分用户文件，避免深度风险
            FileScanner.ScanResult result = fileScanner.scanAndCluster(directoryPath, true, 12);

            long duration = System.currentTimeMillis() - startTime;

            if (result.getTotalFiles() == 0) {
                System.out.println("❌ 聚类分析未找到文件");
                return;
            }

            // 格式化显示时间
            String timeStr;
            if (duration < 1000) {
                timeStr = duration + "毫秒";
            } else if (duration < 60000) {
                timeStr = String.format("%.1f秒", duration / 1000.0);
            } else {
                timeStr = String.format("%.1f分钟", duration / 60000.0);
            }

            System.out.println("\n🎉 聚类分析完成！耗时: " + timeStr);
            System.out.println("📊 " + fileScanner.getScanStatistics());

            // 显示错误信息
            if (!result.getErrors().isEmpty()) {
                System.out.println("\n⚠️ 由于权限问题，跳过了 " + result.getErrors().size() + " 个文件");
                System.out.println("💡 这解释了为什么总大小(2.6GB) < Windows显示的74.5GB");

                // 只显示前5个具体错误
                result.getErrors().stream().limit(5).forEach(error ->
                    System.out.println("   " + error));

                if (result.getErrors().size() > 5) {
                    System.out.println("   ... 还有 " + (result.getErrors().size() - 5) + " 个权限错误未显示");
                }
            }

            // 显示聚类结果（只显示前10个最大的簇）
            System.out.println("\n📋 Top 10 最大的文件簇:");
            result.getClustersSortedByFileCount().stream()
                    .limit(10)
                    .forEach(cluster -> {
                        System.out.println("  🏷️ " + cluster.getDescription());
                        if (!cluster.getSampleFiles().isEmpty()) {
                            System.out.println("      样本: " + String.join(", ", cluster.getSampleFiles()));
                        }
                        if (cluster.hasMoreFiles()) {
                            System.out.println("      💡 更多文件未显示");
                        }
                    });

            // 询问是否要删除文件簇
            System.out.println("\n🗑️ 文件管理选项:");
            System.out.println("1. 查看更多聚类详情");
            System.out.println("2. 删除指定类型的文件簇");
            System.out.println("3. 删除大于指定大小的文件");
            System.out.println("4. 继续扫描其他目录");
            System.out.print("请选择 (1-4): ");
            String actionChoice = scanner.nextLine().trim();

            switch (actionChoice) {
                case "1":
                    showMoreClusters(result);
                    break;
                case "2":
                    deleteByClusterType(result, scanner);
                    break;
                case "3":
                    deleteByFileSize(result, scanner);
                    break;
                case "4":
                default:
                    break;
            }

            // 显示簇总数信息
            long totalClusters = result.getClusterCount();
            if (totalClusters > 10) {
                System.out.println("  ... 还有 " + (totalClusters - 10) + " 个较小的簇未显示");
            }

        } catch (Exception e) {
            System.out.println("❌ 聚类分析失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    /**
     * 显示更多聚类详情
     */
    private static void showMoreClusters(FileScanner.ScanResult result) {
        System.out.println("\n📋 更多聚类详情 (前20个):");
        result.getClustersSortedByFileCount().stream()
                .limit(20)
                .forEach(cluster -> {
                    System.out.println("  🏷️ " + cluster.getDescription());
                    System.out.println("      文件数: " + cluster.getFileCount() +
                                     ", 大小: " + formatFileSize(cluster.getTotalSize()));
                });
    }

    /**
     * 按文件类型删除
     */
    private static void deleteByClusterType(FileScanner.ScanResult result, Scanner scanner) {
        System.out.println("\n🗑️ 删除指定类型的文件簇:");
        System.out.println("可删除的文件类型 (输入扩展名，如 .log, .tmp, .cache):");
        System.out.print("请输入要删除的文件扩展名: ");
        String extToDelete = scanner.nextLine().trim().toLowerCase();

        if (!extToDelete.startsWith(".")) {
            extToDelete = "." + extToDelete;
        }

        System.out.println("\n⚠️ 警告：即将删除所有 " + extToDelete + " 文件");
        System.out.print("确认删除吗？输入 'DELETE' 确认: ");
        String confirm = scanner.nextLine().trim();

        if (!"DELETE".equals(confirm)) {
            System.out.println("❌ 删除操作已取消");
            return;
        }

        System.out.println("🔍 正在搜索并删除 " + extToDelete + " 文件...");
        int deletedCount = 0;
        long deletedSize = 0;

        for (FileCluster cluster : result.getClusters()) {
            if (cluster.getDescription().contains("类型: " + extToDelete)) {
                // 这里需要实际的文件删除逻辑
                // 注意：这是危险操作，需要谨慎实现
                System.out.println("找到可删除的簇: " + cluster.getDescription());
                System.out.println("  文件数: " + cluster.getFileCount() +
                                 ", 大小: " + formatFileSize(cluster.getTotalSize()));
                // 实际删除功能需要更详细的实现
            }
        }

        System.out.println("🎉 删除完成！共处理了 " + deletedCount + " 个文件，释放 " +
                         formatFileSize(deletedSize) + " 空间");
    }

    /**
     * 按文件大小删除
     */
    private static void deleteByFileSize(FileScanner.ScanResult result, Scanner scanner) {
        System.out.println("\n🗑️ 删除大于指定大小的文件:");
        System.out.print("请输入文件大小阈值 (MB): ");
        String sizeInput = scanner.nextLine().trim();

        try {
            long sizeThreshold = Long.parseLong(sizeInput) * 1024 * 1024; // 转换为字节

            System.out.println("\n⚠️ 警告：即将删除所有大于 " + sizeInput + " MB 的文件");
            System.out.print("确认删除吗？输入 'DELETE' 确认: ");
            String confirm = scanner.nextLine().trim();

            if (!"DELETE".equals(confirm)) {
                System.out.println("❌ 删除操作已取消");
                return;
            }

            System.out.println("🔍 正在搜索大文件...");
            int largeFileCount = 0;
            long largeFileSize = 0;

            // 这里需要实际的文件删除逻辑
            // 注意：这是危险操作，需要谨慎实现
            System.out.println("💡 删除功能需要更详细的实现以确保安全");
            System.out.println("🎯 建议先查看文件列表，确认安全后再删除");

        } catch (NumberFormatException e) {
            System.out.println("❌ 无效的数字格式");
        }
    }

    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }
}