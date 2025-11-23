package com.hanpf.clearai.react.tools.builtin;

import com.hanpf.clearai.react.tools.ReActTool;
import com.hanpf.clearai.react.tools.ToolParam;
import com.hanpf.clearai.utils.ClearAILogger;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;

/**
 * 系统工具集 - 提供系统信息和分析相关的工具
 */
public class SystemTools {

    /**
     * 获取系统信息
     */
    @ReActTool(
        name = "get_system_info",
        description = "获取系统基本信息，包括操作系统、Java版本、内存使用情况等。用于系统诊断和优化建议。",
        category = "system"
    )
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
            info.append(String.format("系统负载: %.2f\n", osBean.getSystemLoadAverage()));

            // Java信息
            info.append(String.format("Java版本: %s\n", System.getProperty("java.version")));
            info.append(String.format("Java厂商: %s\n", System.getProperty("java.vendor")));

            // 内存信息
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            double heapUsagePercent = (double) heapUsed / heapMax * 100;

            info.append(String.format("堆内存使用: %.2f MB / %.2f MB (%.1f%%)\n",
                heapUsed / (1024.0 * 1024.0), heapMax / (1024.0 * 1024.0), heapUsagePercent));

            // 磁盘信息
            File[] roots = File.listRoots();
            for (File root : roots) {
                long free = root.getFreeSpace();
                long total = root.getTotalSpace();
                double freePercent = (double) free / total * 100;

                info.append(String.format("磁盘 %s: %.2f GB 可用 (%.1f%%)\n",
                    root.getAbsolutePath(),
                    free / (1024.0 * 1024.0 * 1024.0),
                    freePercent));
            }

            return info.toString();

        } catch (Exception e) {
            ClearAILogger.error("获取系统信息失败: " + e.getMessage(), e);
            return "❌ 获取系统信息时出错: " + e.getMessage();
        }
    }

    /**
     * 检查系统健康状况
     */
    @ReActTool(
        name = "check_system_health",
        description = "检查系统健康状况，包括内存、磁盘、CPU使用情况，并给出优化建议。",
        category = "system"
    )
    public String checkSystemHealth() {
        try {
            StringBuilder health = new StringBuilder();
            health.append("🏥 系统健康检查\n");

            int issues = 0;

            // 检查内存使用
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            double heapUsagePercent = (double) heapUsed / heapMax * 100;

            if (heapUsagePercent > 80) {
                health.append(String.format("⚠️ 内存使用率过高: %.1f%% (>80%%)\n", heapUsagePercent));
                issues++;
            } else {
                health.append(String.format("✅ 内存使用正常: %.1f%%\n", heapUsagePercent));
            }

            // 检查磁盘空间
            File systemDrive = new File("C:\\");
            long freeSpace = systemDrive.getFreeSpace();
            long totalSpace = systemDrive.getTotalSpace();
            double diskUsagePercent = (1.0 - (double) freeSpace / totalSpace) * 100;

            if (diskUsagePercent > 90) {
                health.append(String.format("🔴 C盘空间严重不足: %.1f%% (>90%%)\n", diskUsagePercent));
                issues++;
            } else if (diskUsagePercent > 80) {
                health.append(String.format("⚠️ C盘空间不足: %.1f%% (>80%%)\n", diskUsagePercent));
                issues++;
            } else {
                health.append(String.format("✅ C盘空间充足: %.1f%%\n", diskUsagePercent));
            }

            // 检查系统负载
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double loadAvg = osBean.getSystemLoadAverage();
            int processors = osBean.getAvailableProcessors();
            double loadPercent = (loadAvg / processors) * 100;

            if (loadAvg > processors * 0.8) {
                health.append(String.format("⚠️ 系统负载较高: %.2f\n", loadAvg));
                issues++;
            } else {
                health.append(String.format("✅ 系统负载正常: %.2f\n", loadAvg));
            }

            // 总结和建议
            health.append("\n📊 健康评估: ");
            if (issues == 0) {
                health.append("系统状态良好 ✅\n");
                health.append("建议：定期清理临时文件，监控系统性能");
            } else if (issues == 1) {
                health.append("存在1个问题 ⚠️\n");
                health.append("建议：关注上述警告并采取相应措施");
            } else {
                health.append(String.format("存在 %d 个问题 🔴\n", issues));
                health.append("建议：优先处理磁盘空间和内存问题");
            }

            return health.toString();

        } catch (Exception e) {
            ClearAILogger.error("系统健康检查失败: " + e.getMessage(), e);
            return "❌ 系统健康检查时出错: " + e.getMessage();
        }
    }

    /**
     * 获取进程信息
     */
    @ReActTool(
        name = "get_process_info",
        description = "获取当前Java进程的基本信息，包括PID、启动时间、运行时长等。",
        category = "system"
    )
    public String getProcessInfo() {
        try {
            StringBuilder info = new StringBuilder();
            info.append("🔄 进程信息\n");

            // 获取进程ID
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            String pid = jvmName.split("@")[0];
            info.append(String.format("进程ID: %s\n", pid));

            // 运行时间信息
            long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
            long uptimeSeconds = uptime / 1000;
            long hours = uptimeSeconds / 3600;
            long minutes = (uptimeSeconds % 3600) / 60;
            long seconds = uptimeSeconds % 60;

            info.append(String.format("运行时间: %02d:%02d:%02d\n", hours, minutes, seconds));

            // 启动时间
            long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
            info.append(String.format("启动时间: %s\n", new java.util.Date(startTime).toString()));

            // JVM参数
            java.util.List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            if (!inputArguments.isEmpty()) {
                info.append("JVM参数:\n");
                for (String arg : inputArguments) {
                    info.append("  ").append(arg).append("\n");
                }
            }

            // 类加载信息
            info.append(String.format("已加载类数: %d\n",
                ManagementFactory.getClassLoadingMXBean().getLoadedClassCount()));

            return info.toString();

        } catch (Exception e) {
            ClearAILogger.error("获取进程信息失败: " + e.getMessage(), e);
            return "❌ 获取进程信息时出错: " + e.getMessage();
        }
    }

    /**
     * 分析常见垃圾文件位置
     */
    @ReActTool(
        name = "analyze_junk_locations",
        description = "分析常见的垃圾文件位置，包括临时文件、回收站、浏览器缓存等，并估算可清理的空间。",
        category = "analysis"
    )
    public String analyzeJunkLocations() {
        try {
            StringBuilder analysis = new StringBuilder();
            analysis.append("🗑️ 垃圾文件位置分析\n");

            long totalJunkSize = 0;
            int checkedLocations = 0;

            // 用户临时目录
            String userTemp = System.getProperty("java.io.tmpdir");
            long tempSize = calculateDirectorySize(userTemp);
            if (tempSize > 0) {
                analysis.append(String.format("📁 用户临时目录: %.2f MB (%s)\n",
                    tempSize / (1024.0 * 1024.0), userTemp));
                totalJunkSize += tempSize;
                checkedLocations++;
            }

            // Windows临时目录
            String windowsTemp = "C:\\Windows\\Temp";
            if (new File(windowsTemp).exists()) {
                long winTempSize = calculateDirectorySize(windowsTemp);
                if (winTempSize > 0) {
                    analysis.append(String.format("📁 系统临时目录: %.2f MB (%s)\n",
                        winTempSize / (1024.0 * 1024.0), windowsTemp));
                    totalJunkSize += winTempSize;
                    checkedLocations++;
                }
            }

            // 用户下载目录
            String userHome = System.getProperty("user.home");
            String downloadsPath = userHome + "\\Downloads";
            if (new File(downloadsPath).exists()) {
                long downloadsSize = calculateDirectorySize(downloadsPath);
                analysis.append(String.format("📁 下载目录: %.2f MB (%s)\n",
                    downloadsSize / (1024.0 * 1024.0), downloadsPath));
                checkedLocations++;
            }

            // 预取文件目录
            String prefetchPath = "C:\\Windows\\Prefetch";
            if (new File(prefetchPath).exists()) {
                long prefetchSize = calculateDirectorySize(prefetchPath);
                if (prefetchSize > 0) {
                    analysis.append(String.format("📁 预取文件: %.2f MB (%s)\n",
                        prefetchSize / (1024.0 * 1024.0), prefetchPath));
                    totalJunkSize += prefetchSize;
                    checkedLocations++;
                }
            }

            // 总结
            analysis.append(String.format("\n📊 分析完成，共检查 %d 个位置\n", checkedLocations));
            analysis.append(String.format("🗑️ 可清理空间总计: %.2f MB\n", totalJunkSize / (1024.0 * 1024.0)));

            if (totalJunkSize > 1024 * 1024 * 1024) { // 大于1GB
                analysis.append("💡 建议：垃圾文件较多，建议执行清理操作释放空间\n");
            } else if (totalJunkSize > 100 * 1024 * 1024) { // 大于100MB
                analysis.append("💡 建议：可以考虑清理以释放一些空间\n");
            } else {
                analysis.append("✅ 垃圾文件较少，系统相对整洁\n");
            }

            return analysis.toString();

        } catch (Exception e) {
            ClearAILogger.error("分析垃圾文件位置失败: " + e.getMessage(), e);
            return "❌ 分析垃圾文件位置时出错: " + e.getMessage();
        }
    }

    /**
     * 计算目录大小
     */
    private long calculateDirectorySize(String path) {
        try {
            File directory = new File(path);
            if (!directory.exists() || !directory.isDirectory()) {
                return 0;
            }

            return calculateDirectorySizeRecursive(directory);
        } catch (Exception e) {
            ClearAILogger.debug("计算目录大小失败: " + path + ", " + e.getMessage());
            return 0;
        }
    }

    /**
     * 递归计算目录大小
     */
    private long calculateDirectorySizeRecursive(File directory) {
        long size = 0;

        try {
            File[] files = directory.listFiles();
            if (files == null) return 0;

            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += calculateDirectorySizeRecursive(file);
                }
            }
        } catch (Exception e) {
            // 忽略权限错误等
        }

        return size;
    }
}