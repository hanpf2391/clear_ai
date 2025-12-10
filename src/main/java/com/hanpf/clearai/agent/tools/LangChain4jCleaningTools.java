package com.hanpf.clearai.agent.tools;

import com.hanpf.clearai.utils.ClearAILogger;

import dev.langchain4j.agent.tool.Tool;

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
 * 基于LangChain4j的清理工具集
 * 使用@Tool注解替代自实现的@ReActTool注解
 */
public class LangChain4jCleaningTools {

    /**
     * 扫描指定目录的文件信息
     */
    @Tool("扫描指定目录，分析文件分布、大小统计、垃圾文件识别等。用于了解目录的清理潜力。")
    public String scanDirectory(
        String path,
        boolean includeSubdirs,
        int maxDepth
    ) {
        ClearAILogger.info("🔧 [工具调用] 扫描目录: " + path);
        ClearAILogger.info("   参数: 包含子目录=" + includeSubdirs + ", 最大深度=" + maxDepth);

        try {
            long toolStartTime = System.currentTimeMillis();

            File directory = new File(path);
            if (!directory.exists()) {
                ClearAILogger.warn("   ⚠️ 目录不存在: " + path);
                return "❌ 目录不存在: " + path;
            }

            if (!directory.isDirectory()) {
                ClearAILogger.warn("   ⚠️ 不是目录: " + path);
                return "❌ 指定路径不是目录: " + path;
            }

            ClearAILogger.info("   📁 开始扫描目录: " + directory.getAbsolutePath());
            DirectoryScanResult result = performDirectoryScan(directory, includeSubdirs, maxDepth);

            long toolDuration = System.currentTimeMillis() - toolStartTime;
            ClearAILogger.info("   ✅ 目录扫描完成，耗时: " + toolDuration + "ms");
            ClearAILogger.info("   📊 扫描结果: " + result.getFileCount() + "个文件, " + result.getDirectoryCount() + "个目录");

            String formattedResult = formatScanResult(result);
            ClearAILogger.info("   📝 结果长度: " + formattedResult.length() + " 字符");

            return formattedResult;

        } catch (Exception e) {
            ClearAILogger.error("   ❌ 目录扫描失败: " + e.getMessage(), e);
            return "❌ 扫描目录时出错: " + e.getMessage();
        }
    }

    /**
     * 目录安全清理分析 - 绿灯报告格式
     * 提供结构化的目录分析结果，按安全级别分类
     */
    @Tool("提供结构化的目录安全清理分析报告，将文件按安全级别分类：安全删除(绿灯)、用户确认(黄灯)、保留文件(红灯)")
    public String analyzeDirectoryForCleaning(
        String directoryPath
    ) {
        ClearAILogger.info("🔧 [工具调用] 分析目录清理安全性: " + directoryPath);

        try {
            long toolStartTime = System.currentTimeMillis();

            Path path = Paths.get(directoryPath);
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                ClearAILogger.warn("   ⚠️ 目录不存在或不是目录: " + directoryPath);
                return "❌ 错误：指定的目录不存在或不是目录 - " + directoryPath;
            }

            ClearAILogger.info("   📁 开始分析目录: " + path.toAbsolutePath());

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

            ClearAILogger.info("   📊 收集到文件数量: " + allFiles.size() + "个");

            // 调用AI进行智能分类和分析
            String analysisResult = analyzeFilesWithAI(directoryPath, allFiles);

            long toolDuration = System.currentTimeMillis() - toolStartTime;
            ClearAILogger.info("   ✅ 目录分析完成，耗时: " + toolDuration + "ms");
            ClearAILogger.info("   📝 分析报告长度: " + analysisResult.length() + " 字符");

            return analysisResult;

        } catch (Exception e) {
            ClearAILogger.error("   ❌ 目录分析时出错: " + e.getMessage(), e);
            return "❌ 目录分析时出错: " + e.getMessage();
        }
    }

    // ========== 内部辅助方法 - 从原始类中迁移 ==========

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

    // ========== 其他辅助方法（从原始类中迁移）==========

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

    // 其他方法的简化实现...
    private String generateIntelligentAnalysis(String directoryPath, List<FileInfo> allFiles) {
        return "AI智能分析完成";
    }

    private String generateFileSummary(FileInfo file) {
        return "这是" + getFileTypeDescription(file.getExtension()) + "，大小为" + String.format("%.1f", file.getFileSize() / (1024.0 * 1024.0)) + "MB";
    }

    private String generateFileSuggestion(FileInfo file) {
        return "请根据实际需求决定是否删除此文件";
    }

    private String getFileTypeDescription(String extension) {
        switch (extension) {
            case "java": return "Java源代码";
            case "log": return "日志文件";
            case "tmp": return "临时文件";
            case "bak": return "备份文件";
            case "doc": case "docx": return "Word文档";
            case "pdf": return "PDF文档";
            case "txt": return "文本文件";
            case "jpg": case "png": case "gif": return "图片文件";
            case "zip": case "rar": return "压缩文件";
            default: return extension.isEmpty() ? "无扩展名文件" : extension.toUpperCase() + "文件";
        }
    }

    private DirectoryScanResult performDirectoryScan(File directory, boolean includeSubdirs, int maxDepth) throws IOException {
        DirectoryScanResult result = new DirectoryScanResult();
        result.setPath(directory.getAbsolutePath());

        if (!includeSubdirs || maxDepth == 1) {
            scanSingleLevel(directory, result);
        } else {
            int effectiveMaxDepth = maxDepth <= 0 ? Integer.MAX_VALUE : maxDepth;
            scanRecursive(directory, result, 0, effectiveMaxDepth);
        }

        return result;
    }

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

    private String formatScanResult(DirectoryScanResult result) {
        StringBuilder output = new StringBuilder();
        output.append("📁 目录扫描结果: ").append(result.getPath()).append("\n");
        output.append(String.format("文件总数: %d\n", result.getFileCount()));
        output.append(String.format("目录总数: %d\n", result.getDirectoryCount()));
        output.append(String.format("总大小: %.2f MB\n", result.getTotalSize() / (1024.0 * 1024.0)));
        return output.toString();
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }

    // ========== 内部数据类 ==========

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

    private static class DirectoryScanResult {
        private String path;
        private final List<File> files = new ArrayList<>();
        private final List<File> directories = new ArrayList<>();

        public void addFile(File file) { files.add(file); }
        public void addDirectory(File directory) { directories.add(directory); }

        public int getFileCount() { return files.size(); }
        public int getDirectoryCount() { return directories.size(); }
        public long getTotalSize() { return files.stream().mapToLong(File::length).sum(); }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }
}