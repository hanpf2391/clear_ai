package com.hanpf.clearai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * 清理工具类
 * AI可调用的系统清理功能
 * 手动配置版本，适配ClearAI项目
 */
public class CleaningTools {

    /**
     * 扫描指定目录中的大文件
     * @param directoryPath 要扫描的目录路径
     * @param sizeThresholdMB 文件大小阈值（MB）
     * @return 大文件列表
     */
    @Tool("扫描指定目录中的大文件")
    public String scanLargeFiles(
            @P("目录路径") String directoryPath,
            @P("文件大小阈值(MB)") int sizeThresholdMB) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                return "错误：指定的目录不存在 - " + directoryPath;
            }

            var largeFiles = Files.walk(path)
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(file -> file.length() > sizeThresholdMB * 1024L * 1024L)
                    .sorted((a, b) -> Long.compare(b.length(), a.length()))
                    .limit(20)
                    .collect(Collectors.toList());

            if (largeFiles.isEmpty()) {
                return String.format("在目录 %s 中没有找到大于 %dMB 的文件。", directoryPath, sizeThresholdMB);
            }

            StringBuilder result = new StringBuilder();
            result.append(String.format("在目录 %s 中找到 %d 个大于 %dMB 的文件：\n\n",
                    directoryPath, largeFiles.size(), sizeThresholdMB));

            for (int i = 0; i < largeFiles.size(); i++) {
                File file = largeFiles.get(i);
                result.append(String.format("%d. %s\n", i + 1, file.getName()));
                result.append(String.format("   路径: %s\n", file.getAbsolutePath()));
                result.append(String.format("   大小: %.2f MB\n", file.length() / (1024.0 * 1024.0)));
                result.append(String.format("   修改时间: %s\n",
                        new java.util.Date(file.lastModified()).toString()));
                result.append("\n");
            }

            return result.toString();

        } catch (Exception e) {
            return "扫描大文件时出错: " + e.getMessage();
        }
    }

    /**
     * 获取磁盘使用情况
     * @param diskPath 磁盘路径（如 "C:/" 或 "/home"）
     * @return 磁盘使用信息
     */
    @Tool("获取磁盘使用情况")
    public String getDiskUsage(@P("磁盘路径") String diskPath) {
        try {
            File disk = new File(diskPath);
            if (!disk.exists()) {
                return "指定的磁盘路径不存在: " + diskPath;
            }

            long totalSpace = disk.getTotalSpace();
            long freeSpace = disk.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;

            double usedPercent = (double) usedSpace / totalSpace * 100;
            double freePercent = (double) freeSpace / totalSpace * 100;

            return String.format("磁盘使用情况 - %s\n" +
                            "总容量: %.2f GB\n" +
                            "已使用: %.2f GB (%.1f%%)\n" +
                            "可用空间: %.2f GB (%.1f%%)",
                    diskPath,
                    totalSpace / (1024.0 * 1024.0 * 1024.0),
                    usedSpace / (1024.0 * 1024.0 * 1024.0),
                    usedPercent,
                    freeSpace / (1024.0 * 1024.0 * 1024.0),
                    freePercent);

        } catch (Exception e) {
            return "获取磁盘使用情况时出错: " + e.getMessage();
        }
    }

    /**
     * 检查系统内存使用情况
     * @return 内存使用信息
     */
    @Tool("检查系统内存使用情况")
    public String checkMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();

            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            double usedPercent = (double) usedMemory / maxMemory * 100;

            return String.format("JVM内存使用情况\n" +
                            "最大可用内存: %.2f MB\n" +
                            "当前占用内存: %.2f MB\n" +
                            "已使用内存: %.2f MB (%.1f%%)\n" +
                            "空闲内存: %.2f MB",
                    maxMemory / (1024.0 * 1024.0),
                    totalMemory / (1024.0 * 1024.0),
                    usedMemory / (1024.0 * 1024.0),
                    usedPercent,
                    freeMemory / (1024.0 * 1024.0));

        } catch (Exception e) {
            return "检查内存使用情况时出错: " + e.getMessage();
        }
    }

    /**
     * 分析目录结构
     * @param directoryPath 要分析的目录路径
     * @return 目录结构分析结果
     */
    @Tool("分析目录结构")
    public String analyzeDirectory(@P("目录路径") String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return "指定的目录不存在或不是目录: " + directoryPath;
            }

            final long[] fileCount = {0};
            final long[] totalSize = {0};
            final long[] subDirCount = {0};

            Files.walk(path)
                    .forEach(p -> {
                        if (Files.isRegularFile(p)) {
                            fileCount[0]++;
                            try {
                                totalSize[0] += Files.size(p);
                            } catch (Exception ignored) {}
                        } else if (Files.isDirectory(p) && !p.equals(path)) {
                            subDirCount[0]++;
                        }
                    });

            return String.format("目录分析结果 - %s\n" +
                            "文件总数: %d\n" +
                            "子目录数: %d\n" +
                            "总大小: %.2f MB\n" +
                            "平均文件大小: %.2f KB",
                    directoryPath,
                    fileCount[0],
                    subDirCount[0],
                    totalSize[0] / (1024.0 * 1024.0),
                    fileCount[0] > 0 ? (double) totalSize[0] / fileCount[0] / 1024.0 : 0);

        } catch (Exception e) {
            return "分析目录时出错: " + e.getMessage();
        }
    }

    /**
     * 清空临时文件
     * @return 清理结果
     */
    @Tool("清空系统临时文件")
    public String cleanTempFiles() {
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            Path tempPath = Paths.get(tempDir);

            if (!Files.exists(tempPath)) {
                return "临时目录不存在: " + tempDir;
            }

            // 统计临时文件
            long[] stats = Files.walk(tempPath)
                    .filter(Files::isRegularFile)
                    .mapToLong(file -> {
                        try {
                            long size = Files.size(file);
                            Files.delete(file);
                            return size;
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .toArray();

            long totalSize = java.util.Arrays.stream(stats).sum();
            int deletedCount = (int) java.util.Arrays.stream(stats).filter(size -> size > 0).count();

            return String.format("临时文件清理完成！\n删除文件数量: %d\n释放空间: %.2f MB",
                    deletedCount, totalSize / (1024.0 * 1024.0));

        } catch (Exception e) {
            return "清理临时文件时出错: " + e.getMessage();
        }
    }
}