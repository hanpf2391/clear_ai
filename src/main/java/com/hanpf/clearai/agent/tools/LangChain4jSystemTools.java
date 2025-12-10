package com.hanpf.clearai.agent.tools;

import dev.langchain4j.agent.tool.Tool;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;

/**
 * 基于LangChain4j的系统工具集
 */
public class LangChain4jSystemTools {

    /**
     * 获取系统信息
     */
    @Tool("获取系统基本信息，包括操作系统、Java版本、内存使用情况等。用于系统诊断和优化建议。")
    public String getSystemInfo() {
        try {
            StringBuilder info = new StringBuilder();
            info.append("💻 系统信息\n");

            // 操作系统信息
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            info.append(String.format("操作系统: %s %s\n",
                System.getProperty("os.name"), System.getProperty("os.version")));
            info.append(String.format("架构: %s\n", System.getProperty("os.arch")));
            info.append(String.format("CPU核心数: %d\n", osBean.getAvailableProcessors()));

            // Java信息
            info.append(String.format("Java版本: %s\n", System.getProperty("java.version")));
            info.append(String.format("Java厂商: %s\n", System.getProperty("java.vendor")));

            // 内存信息
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            double heapUsagePercent = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;

            info.append(String.format("堆内存使用: %.2f MB / %.2f MB (%.1f%%)\n",
                heapUsed / (1024.0 * 1024.0), heapMax / (1024.0 * 1024.0), heapUsagePercent));

            return info.toString();

        } catch (Exception e) {
            return "❌ 获取系统信息失败: " + e.getMessage();
        }
    }

    /**
     * 检查系统健康状况
     */
    @Tool("检查系统健康状况，包括内存、磁盘、CPU使用情况")
    public String checkSystemHealth() {
        try {
            StringBuilder health = new StringBuilder();
            health.append("🏥 系统健康检查\n");

            // 内存健康检查
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            double memoryUsage = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;

            health.append(String.format("内存使用率: %.1f%% - %s\n",
                memoryUsage,
                memoryUsage < 80 ? "🟢 正常" : memoryUsage < 90 ? "🟡 注意" : "🔴 需要关注"));

            // 磁盘空间检查
            File[] roots = File.listRoots();
            if (roots.length > 0) {
                File mainDrive = roots[0];
                long freeSpace = mainDrive.getFreeSpace();
                long totalSpace = mainDrive.getTotalSpace();
                double diskUsage = (double) (totalSpace - freeSpace) / totalSpace * 100;

                health.append(String.format("主磁盘使用率: %.1f%% - %s\n",
                    diskUsage,
                    diskUsage < 80 ? "🟢 正常" : diskUsage < 90 ? "🟡 注意" : "🔴 需要关注"));
            }

            return health.toString();

        } catch (Exception e) {
            return "❌ 系统健康检查失败: " + e.getMessage();
        }
    }
}