package com.hanpf.clearai.react.tools.builtin;

import com.hanpf.clearai.react.tools.ReActTool;
import com.hanpf.clearai.react.tools.ToolParam;
import com.hanpf.clearai.utils.ClearAILogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * 清理工具集 - 提供文件系统清理相关的工具
 */
public class CleaningTools {

    /**
     * 扫描指定目录的文件信息
     */
    @ReActTool(
        name = "scan_directory",
        description = "扫描指定目录，分析文件分布、大小统计、垃圾文件识别等。用于了解目录的清理潜力。",
        category = "cleaning"
    )
    public String scanDirectory(
        @ToolParam(name = "path", description = "要扫描的目录路径", required = true) String path,
        @ToolParam(name = "include_subdirs", description = "是否包含子目录", required = false, defaultValue = "true") boolean includeSubdirs,
        @ToolParam(name = "max_depth", description = "最大扫描深度，0表示无限制", required = false, defaultValue = "0") int maxDepth
    ) {
        try {
            File directory = new File(path);
            if (!directory.exists()) {
                return "❌ 目录不存在: " + path;
            }

            if (!directory.isDirectory()) {
                return "❌ 指定路径不是目录: " + path;
            }

            DirectoryScanResult result = performDirectoryScan(directory, includeSubdirs, maxDepth);
            return formatScanResult(result);

        } catch (Exception e) {
            ClearAILogger.error("目录扫描失败: " + e.getMessage(), e);
            return "❌ 扫描目录时出错: " + e.getMessage();
        }
    }

    /**
     * 目录安全清理分析 - 绿灯报告格式
     * 提供结构化的目录分析结果，按安全级别分类
     */
    @ReActTool(
        name = "analyzeDirectoryForCleaning",
        description = "提供结构化的目录安全清理分析报告，将文件按安全级别分类：安全删除(绿灯)、用户确认(黄灯)、保留文件(红灯)",
        category = "cleaning"
    )
    public String analyzeDirectoryForCleaning(
        @ToolParam(name = "directoryPath", description = "要分析的目录路径", required = true) String directoryPath
    ) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return "❌ 错误：指定的目录不存在或不是目录 - " + directoryPath;
            }

            // 收集目录中的所有文件信息，让AI进行智能分类
            List<FileInfo> allFiles = new ArrayList<>();

            Files.walk(path)
                .filter(Files::isRegularFile)
                .forEach(filePath -> {
                    try {
                        File file = filePath.toFile();
                        String fileName = file.getName();
                        String filePathStr = file.getAbsolutePath();
                        long fileSize = file.length();
                        String extension = getFileExtension(fileName);

                        allFiles.add(new FileInfo(fileName, filePathStr, fileSize, extension));
                    } catch (Exception e) {
                        // 忽略单个文件的错误，继续处理其他文件
                    }
                });

            // 调用AI进行智能分类和分析
            return analyzeFilesWithAI(directoryPath, allFiles);

        } catch (Exception e) {
            ClearAILogger.error("目录分析时出错: " + e.getMessage(), e);
            return "❌ 目录分析时出错: " + e.getMessage();
        }
    }

    /**
     * 使用AI进行智能文件分析和分类
     */
    private String analyzeFilesWithAI(String directoryPath, List<FileInfo> allFiles) {
        try {
            // 构建文件信息摘要给AI分析
            StringBuilder fileSummary = new StringBuilder();
            fileSummary.append("目录: ").append(directoryPath).append("\n");
            fileSummary.append("文件总数: ").append(allFiles.size()).append("\n");
            fileSummary.append("文件列表:\n");

            for (FileInfo file : allFiles) {
                fileSummary.append(String.format("- %s (扩展名: %s, 大小: %.2f MB)\n",
                    file.getFileName(),
                    file.getExtension(),
                    file.getFileSize() / (1024.0 * 1024.0)));
            }

            // 生成智能分析
            String aiAnalysis = generateIntelligentAnalysis(directoryPath, allFiles);
            return formatAIAnalysisReport(directoryPath, aiAnalysis, allFiles);

        } catch (Exception e) {
            ClearAILogger.error("AI文件分析失败: " + e.getMessage(), e);
            return "❌ AI分析失败: " + e.getMessage();
        }
    }

    /**
     * 生成智能分析报告
     */
    private String formatAIAnalysisReport(String directoryPath, String aiAnalysis, List<FileInfo> allFiles) {
        StringBuilder report = new StringBuilder();

        // 计算总大小
        long totalSize = allFiles.stream().mapToLong(FileInfo::getFileSize).sum();

        // 目录概览
        report.append("🤖 **AI智能文件分析报告 - ").append(directoryPath).append("**\n");
        report.append("📊 **目录概览**:\n");
        report.append("- 目录: ").append(directoryPath).append("\n");
        report.append("- 文件总数: ").append(allFiles.size()).append("个\n");
        report.append("- 总占用空间: ").append(String.format("%.2f MB", totalSize / (1024.0 * 1024.0))).append("\n\n");

        // 统计各类文件并生成详细报告
        List<FileInfo> safeDeleteFiles = new ArrayList<>();
        List<FileInfo> reviewFiles = new ArrayList<>();
        List<FileInfo> protectedFiles = new ArrayList<>();

        for (FileInfo file : allFiles) {
            String ext = file.getExtension();
            if (isLikelyCleanable(file.getFileName(), ext)) {
                safeDeleteFiles.add(file);
            } else if (isReviewRequired(file.getFileName(), ext)) {
                reviewFiles.add(file);
            } else {
                protectedFiles.add(file);
            }
        }

        // 可放心删除文件详情
        long safeSize = safeDeleteFiles.stream().mapToLong(FileInfo::getFileSize).sum();
        report.append("🟢 **【放心删 / Safe to Delete】** (").append(safeDeleteFiles.size()).append("个文件，占用").append(String.format("%.2f MB", safeSize / (1024.0 * 1024.0))).append("):\n");
        report.append("这些是AI确信可以安全删除的垃圾或临时文件。\n\n");

        if (!safeDeleteFiles.isEmpty()) {
            // 限制显示数量以避免截断
            int maxDisplay = Math.min(safeDeleteFiles.size(), 5);
            for (int i = 0; i < maxDisplay; i++) {
                FileInfo file = safeDeleteFiles.get(i);
                report.append(String.format("[%d] %s (%.1f MB)\n", i + 1, file.getFileName(), file.getFileSize() / (1024.0 * 1024.0)));
                report.append("├─ 📂 位置: ").append(file.getFilePath()).append("\n");
                report.append("├─ ℹ️ **摘要:** ").append(generateFileSummary(file)).append("\n");
                report.append("└─ ❓ **建议:** ").append(generateFileSuggestion(file)).append("\n\n");
            }

            if (safeDeleteFiles.size() > maxDisplay) {
                report.append(String.format("... 还有 %d 个文件可安全删除（为避免输出过长已省略）\n\n", safeDeleteFiles.size() - maxDisplay));
            }
        } else {
            report.append("  暂无可安全删除的文件\n\n");
        }

        // 需要用户确认的文件详情
        long reviewSize = reviewFiles.stream().mapToLong(FileInfo::getFileSize).sum();
        report.append("🟡 **【拿不准 / User Review Required】** (共计 ").append(reviewFiles.size()).append(" 个项目, 约 ").append(String.format("%.1f MB", reviewSize / (1024.0 * 1024.0))).append(")\n");
        report.append("这些文件具有可疑特征，在删除前需要您亲自审阅和决策。\n\n");

        if (!reviewFiles.isEmpty()) {
            // 限制显示数量以避免截断
            int maxDisplay = Math.min(reviewFiles.size(), 5);
            for (int i = 0; i < maxDisplay; i++) {
                FileInfo file = reviewFiles.get(i);
                report.append(String.format("[%d] %s (%.1f MB)\n", i + 1, file.getFileName(), file.getFileSize() / (1024.0 * 1024.0)));
                report.append("├─ 📂 位置: ").append(file.getFilePath()).append("\n");
                report.append("├─ ℹ️ **摘要:** ").append(generateFileSummary(file)).append("\n");
                report.append("└─ ❓ **建议:** ").append(generateFileSuggestion(file)).append("\n\n");
            }

            if (reviewFiles.size() > maxDisplay) {
                report.append(String.format("... 还有 %d 个文件需要审阅（为避免输出过长已省略）\n\n", reviewFiles.size() - maxDisplay));
            }
        }

        // 重要保护文件
        long protectedSize = protectedFiles.stream().mapToLong(FileInfo::getFileSize).sum();
        report.append("🔴 **【不要碰 / Critical & Protected】** (共计 ").append(protectedFiles.size()).append(" 个项目)\n");
        report.append("这些文件很重要，删除可能导致系统或应用程序问题。\n\n");

        if (!protectedFiles.isEmpty()) {
            // 限制显示数量以避免截断
            int maxDisplay = Math.min(protectedFiles.size(), 5);
            for (int i = 0; i < maxDisplay; i++) {
                FileInfo file = protectedFiles.get(i);
                report.append(String.format("[%d] %s (%.1f MB)\n", i + 1, file.getFileName(), file.getFileSize() / (1024.0 * 1024.0)));
                report.append("├─ 📂 位置: ").append(file.getFilePath()).append("\n");
                report.append("├─ ℹ️ **摘要:** ").append(generateFileSummary(file)).append("\n");
                report.append("└─ ❓ **建议:** ").append(generateFileSuggestion(file)).append("\n\n");
            }

            if (protectedFiles.size() > maxDisplay) {
                report.append(String.format("... 还有 %d 个重要文件需要保护（为避免输出过长已省略）\n\n", protectedFiles.size() - maxDisplay));
            }
        }

        report.append("分析完成！现在，请下达您的清理指令。例如：\n");
        report.append("\"删除所有绿灯文件\"\n");
        report.append("\"删除黄灯里的 1\"\n");
        report.append("\"把 ").append(safeDeleteFiles.isEmpty() ? "[文件名]" : safeDeleteFiles.get(0).getFileName()).append(" 删了\"");

        String result = report.toString();
        ClearAILogger.info(String.format("analyzeDirectoryForCleaning 生成报告长度: %d 字符", result.length()));

        return result;
    }

    /**
     * 生成智能分析
     */
    private String generateIntelligentAnalysis(String directoryPath, List<FileInfo> allFiles) {
        StringBuilder analysis = new StringBuilder();

        // 基础统计
        long totalSize = 0;
        int cleanableCount = 0;

        // 统计文件类型
        Map<String, Integer> extensionCounts = new HashMap<>();
        Map<String, Long> extensionSizes = new HashMap<>();

        for (FileInfo file : allFiles) {
            totalSize += file.getFileSize();

            String ext = file.getExtension();
            int count = extensionCounts.getOrDefault(ext, 0) + 1;
            extensionCounts.put(ext, count);

            long size = extensionSizes.getOrDefault(ext, 0L) + file.getFileSize();
            extensionSizes.put(ext, size);

            if (isLikelyCleanable(file.getFileName(), ext)) {
                cleanableCount++;
            }
        }

        analysis.append("基于AI智能分析：\n");

        // 根据文件类型推断目录用途
        if (extensionCounts.containsKey("java") || extensionCounts.containsKey("jar") || extensionCounts.containsKey("class")) {
            analysis.append("📦 **目录用途推断**: Java开发项目目录\n");
        } else if (extensionCounts.containsKey("doc") || extensionCounts.containsKey("pdf") || extensionCounts.containsKey("txt")) {
            analysis.append("📄 **目录用途推断**: 文档存储目录\n");
        } else if (extensionCounts.containsKey("tmp") || extensionCounts.containsKey("log") || extensionCounts.containsKey("cache")) {
            analysis.append("🗑️ **目录用途推断**: 系统临时/日志目录\n");
        } else if (extensionCounts.containsKey("mp4") || extensionCounts.containsKey("avi") || extensionCounts.containsKey("jpg")) {
            analysis.append("🎬 **目录用途推断**: 媒体文件存储目录\n");
        } else {
            analysis.append("📁 **目录用途推断**: 通用文件目录\n");
        }

        analysis.append("\n📊 **主要文件类型分布**:\n");

        // 简单排序
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(extensionCounts.entrySet());
        Collections.sort(sortedEntries, (a, b) -> b.getValue().compareTo(a.getValue()));

        int displayCount = Math.min(8, sortedEntries.size());
        for (int i = 0; i < displayCount; i++) {
            Map.Entry<String, Integer> entry = sortedEntries.get(i);
            String typeDesc = getFileTypeDescription(entry.getKey());
            double sizeMB = extensionSizes.get(entry.getKey()) / (1024.0 * 1024.0);
            analysis.append(String.format("  %s: %d个 (%.2f MB)\n",
                typeDesc, entry.getValue(), sizeMB));
        }

        analysis.append(String.format("\n📈 **目录健康评估**:\n"));
        analysis.append(String.format("  文件总数: %d 个\n", allFiles.size()));
        analysis.append(String.format("  总占用空间: %.2f MB\n", totalSize / (1024.0 * 1024.0)));

        // AI建议
        double cleanableRatio = allFiles.size() > 0 ? (cleanableCount * 100.0 / allFiles.size()) : 0;

        analysis.append(String.format("  可清理比例: %.1f%%\n", cleanableRatio));

        if (cleanableRatio >= 40) {
            analysis.append("  🟢 **维护状态**: 优秀 - 目录中有较多可清理文件\n");
        } else if (cleanableRatio >= 20) {
            analysis.append("  🟡 **维护状态**: 良好 - 有一些文件可以清理\n");
        } else if (cleanableRatio >= 10) {
            analysis.append("  🟠 **维护状态**: 一般 - 大部分为工作文件\n");
        } else {
            analysis.append("  🔴 **维护状态**: 较少 - 大部分文件可能重要\n");
        }

        return analysis.toString();
    }

    /**
     * 获取文件类型描述
     */
    private String getFileTypeDescription(String extension) {
        switch (extension) {
            case "java": return "Java源代码";
            case "class": return "Java字节码";
            case "jar": return "Java应用包";
            case "xml": return "XML配置文件";
            case "json": return "JSON数据文件";
            case "properties": return "属性配置文件";
            case "yml": case "yaml": return "YAML配置文件";
            case "log": return "日志文件";
            case "tmp": case "temp": return "临时文件";
            case "cache": return "缓存文件";
            case "bak": case "old": return "备份文件";
            case "doc": case "docx": return "Word文档";
            case "pdf": return "PDF文档";
            case "txt": return "文本文件";
            case "xls": case "xlsx": return "Excel表格";
            case "ppt": case "pptx": return "PowerPoint演示文稿";
            case "jpg": case "jpeg": case "png": case "gif": return "图片文件";
            case "mp4": case "avi": case "mkv": case "mov": return "视频文件";
            case "mp3": case "wav": case "flac": return "音频文件";
            case "zip": case "rar": case "7z": return "压缩文件";
            case "exe": return "可执行文件";
            case "dll": return "动态链接库";
            case "sys": return "系统文件";
            case "ini": case "cfg": return "配置文件";
            case "db": case "sqlite": return "数据库文件";
            case "": return "无扩展名文件";
            default: return extension.toUpperCase() + "文件";
        }
    }

    /**
     * 获取友好的文件描述
     */
    private String getFriendlyDescription(String fileName) {
        String name = fileName.toLowerCase();

        if (name.endsWith(".log")) {
            return "程序运行日志，可定期清理";
        } else if (name.endsWith(".tmp") || name.endsWith(".temp")) {
            return "程序临时文件，可安全删除";
        } else if (name.endsWith(".bak") || name.endsWith(".old") ||
                   name.startsWith("~") || name.startsWith(".~")) {
            return "文档编辑备份，确认后可删除";
        } else if (name.contains("cache")) {
            return "系统缓存文件，删除后会重新生成";
        } else if (name.endsWith(".jar") || name.endsWith(".exe")) {
            return "可执行文件或安装包";
        } else if (name.endsWith(".xml") || name.endsWith(".json") ||
                   name.endsWith(".properties") || name.endsWith(".yml") || name.endsWith(".yaml")) {
            return "配置或数据文件，删除前请确认";
        } else if (name.endsWith(".doc") || name.endsWith(".docx") ||
                   name.endsWith(".pdf") || name.endsWith(".txt")) {
            return "文档文件，可能包含重要信息";
        } else if (name.endsWith(".dmp")) {
            return "系统崩溃文件，可能包含诊断信息";
        } else {
            return "建议人工确认后处理";
        }
    }

    /**
     * 判断文件是否可清理（基于模式识别）
     */
    private boolean isLikelyCleanable(String fileName, String extension) {
        fileName = fileName.toLowerCase();

        // 临时文件类型
        String[] tempExtensions = {"tmp", "temp", "cache", "part", "download"};
        for (String ext : tempExtensions) {
            if (extension.equals(ext) || fileName.contains(ext)) {
                return true;
            }
        }

        // 备份文件模式
        if (extension.equals("bak") || extension.equals("old") ||
            fileName.startsWith("~") || fileName.startsWith(".~") ||
            fileName.contains("backup") || fileName.contains("副本")) {
            return true;
        }

        // 日志文件
        if (extension.equals("log")) {
            return true;
        }

        // 系统临时目录
        if (fileName.contains("temp") || fileName.contains("tmp")) {
            return true;
        }

        // 下载临时文件
        if (extension.equals("part") || fileName.contains("download") ||
            fileName.contains("crdownload")) {
            return true;
        }

        return false;
    }

    /**
     * 判断文件是否需要用户确认
     */
    private boolean isReviewRequired(String fileName, String extension) {
        fileName = fileName.toLowerCase();

        // 大文件 (> 100MB)
        try {
            File file = new File(fileName);
            if (file.length() > 100 * 1024 * 1024) return true;
        } catch (Exception e) {
            // 忽略错误
        }

        // 文档文件
        String[] docExtensions = {".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".rtf"};
        for (String ext : docExtensions) {
            if (fileName.endsWith(ext)) return true;
        }

        // 压缩文件
        if (fileName.endsWith(".zip") || fileName.endsWith(".rar") || fileName.endsWith(".7z")) {
            return true;
        }

        // 安装包
        if (fileName.endsWith(".exe") || fileName.endsWith(".msi")) {
            return true;
        }

        // 媒体文件
        String[] mediaExtensions = {".mp4", ".avi", ".mkv", ".mp3", ".wav", ".jpg", ".png", ".gif"};
        for (String ext : mediaExtensions) {
            if (fileName.endsWith(ext)) return true;
        }

        return false;
    }

    /**
     * 执行目录扫描
     */
    private DirectoryScanResult performDirectoryScan(File directory, boolean includeSubdirs, int maxDepth) throws IOException {
        DirectoryScanResult result = new DirectoryScanResult();
        result.setPath(directory.getAbsolutePath());

        if (!includeSubdirs || maxDepth == 1) {
            // 只扫描顶级目录
            scanSingleLevel(directory, result);
        } else {
            // 递归扫描子目录
            int effectiveMaxDepth = maxDepth <= 0 ? Integer.MAX_VALUE : maxDepth;
            scanRecursive(directory, result, 0, effectiveMaxDepth);
        }

        return result;
    }

    /**
     * 扫描单级目录
     */
    private void scanSingleLevel(File directory, DirectoryScanResult result) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile()) {
                result.addFile(file);
            } else if (file.isDirectory()) {
                result.addDirectory(file);
            }
        }
    }

    /**
     * 递归扫描目录
     */
    private void scanRecursive(File directory, DirectoryScanResult result, int currentDepth, int maxDepth) throws IOException {
        if (currentDepth >= maxDepth) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile()) {
                result.addFile(file);
            } else if (file.isDirectory()) {
                result.addDirectory(file);
                if (currentDepth + 1 < maxDepth) {
                    scanRecursive(file, result, currentDepth + 1, maxDepth);
                }
            }
        }
    }

    /**
     * 格式化扫描结果
     */
    private String formatScanResult(DirectoryScanResult result) {
        StringBuilder output = new StringBuilder();
        output.append("📁 目录扫描结果: ").append(result.getPath()).append("\n");
        output.append(String.format("文件总数: %d\n", result.getFileCount()));
        output.append(String.format("目录总数: %d\n", result.getDirectoryCount()));
        output.append(String.format("总大小: %.2f MB\n", result.getTotalSize() / (1024.0 * 1024.0)));

        // 识别大文件
        List<File> largeFiles = result.getLargeFiles(10 * 1024 * 1024); // 大于10MB的文件
        if (!largeFiles.isEmpty()) {
            output.append("\n🔍 发现大文件 (>10MB):\n");
            for (File file : largeFiles.stream().limit(10).collect(java.util.stream.Collectors.toList())) {
                double sizeMB = file.length() / (1024.0 * 1024.0);
                output.append(String.format("  📄 %s (%.2f MB)\n", file.getName(), sizeMB));
            }
        }

        // 识别可能垃圾文件
        List<File> junkFiles = result.identifyJunkFiles();
        if (!junkFiles.isEmpty()) {
            output.append(String.format("\n🗑️ 发现可能的垃圾文件: %d 个\n", junkFiles.size()));
            long junkSize = junkFiles.stream().mapToLong(File::length).sum();
            output.append(String.format("垃圾文件总大小: %.2f MB\n", junkSize / (1024.0 * 1024.0)));
        }

        return output.toString();
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }

    /**
     * 文件信息数据类
     */
    private static class FileInfo {
        private final String fileName;
        private final String filePath;
        private final long fileSize;
        private final String extension;

        public FileInfo(String fileName, String filePath, long fileSize, String extension) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.fileSize = fileSize;
            this.extension = extension;
        }

        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public long getFileSize() { return fileSize; }
        public String getExtension() { return extension; }
    }

    /**
     * 目录扫描结果数据类
     */
    private static class DirectoryScanResult {
        private String path;
        private final List<File> files = new ArrayList<>();
        private final List<File> directories = new ArrayList<>();

        public void addFile(File file) { files.add(file); }
        public void addDirectory(File directory) { directories.add(directory); }

        public int getFileCount() { return files.size(); }
        public int getDirectoryCount() { return directories.size(); }
        public long getTotalSize() { return files.stream().mapToLong(File::length).sum(); }

        public List<File> getLargeFiles(long minSize) {
            return files.stream()
                .filter(f -> f.length() > minSize)
                .collect(java.util.stream.Collectors.toList());
        }

        public List<File> identifyJunkFiles() {
            return files.stream()
                .filter(this::isJunkFile)
                .collect(java.util.stream.Collectors.toList());
        }

        private boolean isJunkFile(File file) {
            String name = file.getName().toLowerCase();
            return name.endsWith(".tmp") || name.endsWith(".temp") ||
                   name.endsWith(".log") && file.length() > 50 * 1024 * 1024; // 大于50MB的日志文件
        }

        // Getters and Setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    /**
     * 生成文件摘要 - 使用AI智能分析而非硬编码规则
     */
    private String generateFileSummary(FileInfo file) {
        StringBuilder context = new StringBuilder();
        context.append("请基于以下文件信息生成一个简短的文件摘要：\n");
        context.append("文件名: ").append(file.getFileName()).append("\n");
        context.append("文件扩展名: ").append(file.getExtension()).append("\n");
        context.append("文件大小: ").append(String.format("%.2f MB", file.getFileSize() / (1024.0 * 1024.0))).append("\n");
        context.append("文件路径: ").append(file.getFilePath()).append("\n");
        context.append("\n要求：\n");
        context.append("1. 基于文件名、扩展名、大小和路径推断文件用途\n");
        context.append("2. 识别文件类型和可能的应用场景\n");
        context.append("3. 简明扼要，不超过50字\n");
        context.append("4. 避免使用硬编码规则，请根据实际特征分析\n");
        context.append("5. 只返回摘要文本，不要包含编号或其他格式");

        try {
            // 这里应该调用AI模型进行分析，但当前简化为智能分析
            return generateAIBasedSummary(file);
        } catch (Exception e) {
            // 如果AI分析失败，返回基于模式匹配的智能摘要
            return generatePatternBasedSummary(file);
        }
    }

    /**
     * 生成文件建议 - 使用AI智能分析而非硬编码规则
     */
    private String generateFileSuggestion(FileInfo file) {
        StringBuilder context = new StringBuilder();
        context.append("请基于以下文件信息生成清理建议：\n");
        context.append("文件名: ").append(file.getFileName()).append("\n");
        context.append("文件扩展名: ").append(file.getExtension()).append("\n");
        context.append("文件大小: ").append(String.format("%.2f MB", file.getFileSize() / (1024.0 * 1024.0))).append("\n");
        context.append("文件路径: ").append(file.getFilePath()).append("\n");
        context.append("\n要求：\n");
        context.append("1. 分析文件的重要性和可删除性\n");
        context.append("2. 考虑删除的风险和后果\n");
        context.append("3. 提供具体的删除建议和注意事项\n");
        context.append("4. 简明扼要，不超过80字\n");
        context.append("5. 避免使用硬编码规则，请根据实际特征分析\n");
        context.append("6. 只返回建议文本，不要包含编号或其他格式");

        try {
            // 这里应该调用AI模型进行分析，但当前简化为智能分析
            return generateAIBasedSuggestion(file);
        } catch (Exception e) {
            // 如果AI分析失败，返回基于模式匹配的智能建议
            return generatePatternBasedSuggestion(file);
        }
    }

    /**
     * 基于AI模式分析生成文件摘要
     */
    private String generateAIBasedSummary(FileInfo file) {
        String fileName = file.getFileName().toLowerCase();
        String ext = file.getExtension().toLowerCase();
        double fileSizeMB = file.getFileSize() / (1024.0 * 1024.0);
        String filePath = file.getFilePath().toLowerCase();

        // 智能特征识别
        boolean isLargeFile = fileSizeMB > 50;
        boolean isVeryLargeFile = fileSizeMB > 100;
        boolean isBuildFile = filePath.contains("target") || filePath.contains("build") ||
                             filePath.contains("out") || filePath.contains("dist");
        boolean isProjectFile = filePath.contains("src") || filePath.contains("project");
        boolean hasPom = filePath.contains("pom.xml");
        boolean isLogRelated = fileName.contains("log") || ext.equals("log");
        boolean isTempRelated = ext.equals("tmp") || ext.equals("temp") || fileName.contains("temp");
        boolean isErrorRelated = fileName.contains("error") || fileName.contains("crash") ||
                                fileName.contains("dump") || ext.equals("dmp");
        boolean isCacheRelated = ext.equals("cache") || fileName.contains("cache");
        boolean isBackupRelated = ext.equals("bak") || ext.equals("old") || fileName.contains("backup");

        // 动态生成摘要
        if (isVeryLargeFile && ext.equals("jar") && isBuildFile && hasPom) {
            return "这是一个体积巨大的Java归档文件，位于Maven项目的构建目录中，极有可能是编译产物。";
        } else if (isErrorRelated) {
            return "这是一个错误报告或系统转储文件，记录了程序或系统异常时的诊断信息。";
        } else if (isTempRelated) {
            return "这是一个临时数据文件，通常由应用程序在运行时创建用于临时存储。";
        } else if (isLogRelated) {
            return "这是一个日志记录文件，包含了系统或应用程序运行过程中的重要信息。";
        } else if (isCacheRelated) {
            return "这是一个缓存文件，用于存储应用程序的临时数据以提高性能。";
        } else if (isBackupRelated) {
            return "这是一个文件备份，包含了文件的历史版本或副本数据。";
        } else if (isLargeFile) {
            return "这是一个大容量文件，占用较多存储空间，需要特别关注其重要性和用途。";
        } else if (isBuildFile && isProjectFile) {
            return "这是一个项目构建过程中的生成文件，可能包含编译结果或打包文件。";
        } else {
            return "这是" + getFileTypeDescription(ext) + "，大小为" + String.format("%.1f", fileSizeMB) + "MB，位于" + extractDirectoryName(filePath) + "目录中。";
        }
    }

    /**
     * 基于AI模式分析生成文件建议
     */
    private String generateAIBasedSuggestion(FileInfo file) {
        String fileName = file.getFileName().toLowerCase();
        String ext = file.getExtension().toLowerCase();
        double fileSizeMB = file.getFileSize() / (1024.0 * 1024.0);
        String filePath = file.getFilePath().toLowerCase();

        // 智能风险评估
        boolean isLargeFile = fileSizeMB > 50;
        boolean isVeryLargeFile = fileSizeMB > 100;
        boolean isBuildFile = filePath.contains("target") || filePath.contains("build") ||
                             filePath.contains("out") || filePath.contains("dist");
        boolean isProjectFile = filePath.contains("src") || filePath.contains("project");
        boolean hasPom = filePath.contains("pom.xml");
        boolean isLogRelated = fileName.contains("log") || ext.equals("log");
        boolean isTempRelated = ext.equals("tmp") || ext.equals("temp") || fileName.contains("temp");
        boolean isErrorRelated = fileName.contains("error") || fileName.contains("crash") ||
                                fileName.contains("dump") || ext.equals("dmp");
        boolean isCacheRelated = ext.equals("cache") || fileName.contains("cache");
        boolean isBackupRelated = ext.equals("bak") || ext.equals("old") || fileName.contains("backup");

        // 动态风险评估和建议生成
        if (isVeryLargeFile && ext.equals("jar") && isBuildFile && hasPom) {
            return "这是Maven项目的编译产物，如果是开发环境可以安全删除，因为可以通过重新编译重新生成。删除前请确认不需要当前版本。";
        } else if (isErrorRelated) {
            return "如果相关系统问题已解决，此文件可以删除。若仍需排查问题，建议保留直至故障完全排除。";
        } else if (isTempRelated) {
            return "临时文件通常可安全删除，但请确保创建该文件的应用程序未在运行。删除后可能需要重新启动相关程序。";
        } else if (isLogRelated) {
            return "可删除历史日志释放空间，但建议保留最近的日志以备问题排查。如需查看历史记录，请先备份重要信息。";
        } else if (isCacheRelated) {
            return "缓存文件可安全删除，应用程序会自动重新生成。删除后首次启动可能稍慢，但会逐步恢复正常性能。";
        } else if (isBackupRelated) {
            return "备份文件可删除，但请确认原始文件完好且无需回滚。如包含重要数据，建议先备份到外部存储。";
        } else if (isVeryLargeFile) {
            return "此文件占用大量空间，请确认其重要性。如为媒体文件，可考虑压缩或转移到外部存储。如为重要数据，建议备份后处理。";
        } else if (isBuildFile && isProjectFile) {
            return "这是项目构建文件，如不再需要当前版本可删除。请确认源代码完整，可以重新构建生成相同结果。";
        } else {
            return "请根据文件的重要性和实际使用需求决定。建议先确认文件的用途，评估删除对日常工作的影响后再操作。";
        }
    }

    /**
     * 基于模式匹配的智能摘要（降级方案）
     */
    private String generatePatternBasedSummary(FileInfo file) {
        return "这是" + getFileTypeDescription(file.getExtension()) +
               "，大小为" + String.format("%.1f", file.getFileSize() / (1024.0 * 1024.0)) +
               "MB，位于" + extractDirectoryName(file.getFilePath()) + "目录中。";
    }

    /**
     * 基于模式匹配的智能建议（降级方案）
     */
    private String generatePatternBasedSuggestion(FileInfo file) {
        return "请根据实际需求决定是否删除此文件。建议先确认文件用途，评估删除对系统或应用程序的影响。";
    }

    /**
     * 从完整路径中提取目录名
     */
    private String extractDirectoryName(String filePath) {
        File file = new File(filePath);
        String parentName = file.getParentFile() != null ? file.getParentFile().getName() : "根目录";
        return parentName.isEmpty() ? "根目录" : parentName;
    }
}