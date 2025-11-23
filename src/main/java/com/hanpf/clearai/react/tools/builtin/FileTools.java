package com.hanpf.clearai.react.tools.builtin;

import com.hanpf.clearai.react.tools.ReActTool;
import com.hanpf.clearai.react.tools.ToolParam;
import com.hanpf.clearai.utils.ClearAILogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 文件工具集 - 提供文件操作和分析相关的工具
 */
public class FileTools {

    /**
     * 分析指定文件或目录的详细信息
     */
    @ReActTool(
        name = "analyze_file",
        description = "分析指定文件或目录的详细信息，包括大小、创建时间、修改时间、文件类型等。",
        category = "file"
    )
    public String analyzeFile(
        @ToolParam(name = "path", description = "要分析的文件或目录路径", required = true) String path
    ) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                return "❌ 文件或目录不存在: " + path;
            }

            StringBuilder analysis = new StringBuilder();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            analysis.append("📄 文件分析: ").append(file.getAbsolutePath()).append("\n");

            if (file.isFile()) {
                // 文件信息
                analysis.append("类型: 文件\n");
                analysis.append(String.format("大小: %.2f KB\n", file.length() / 1024.0));
                analysis.append("扩展名: ").append(getFileExtension(file.getName())).append("\n");
                analysis.append(String.format("最后修改: %s\n", dateFormat.format(new Date(file.lastModified()))));

                // 文件类型判断
                String fileType = determineFileType(file.getName());
                analysis.append("文件类型: ").append(fileType).append("\n");

            } else if (file.isDirectory()) {
                // 目录信息
                analysis.append("类型: 目录\n");

                File[] children = file.listFiles();
                if (children != null) {
                    int fileCount = 0;
                    int dirCount = 0;
                    long totalSize = 0;

                    for (File child : children) {
                        if (child.isFile()) {
                            fileCount++;
                            totalSize += child.length();
                        } else {
                            dirCount++;
                        }
                    }

                    analysis.append(String.format("包含: %d 个文件, %d 个子目录\n", fileCount, dirCount));
                    analysis.append(String.format("估算大小: %.2f MB\n", totalSize / (1024.0 * 1024.0)));
                }
            }

            // 权限信息
            analysis.append("可读: ").append(file.canRead() ? "是" : "否").append("\n");
            analysis.append("可写: ").append(file.canWrite() ? "是" : "否").append("\n");
            analysis.append("可执行: ").append(file.canExecute() ? "是" : "否").append("\n");

            // 使用Path获取更详细的信息
            try {
                Path filePath = Paths.get(path);
                BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                analysis.append(String.format("创建时间: %s\n",
                    dateFormat.format(new Date(attrs.creationTime().toMillis()))));

                if (attrs.isRegularFile()) {
                    analysis.append(String.format("最后访问: %s\n",
                        dateFormat.format(new Date(attrs.lastAccessTime().toMillis()))));
                }

            } catch (Exception e) {
                analysis.append("⚠️ 无法获取详细时间信息\n");
            }

            return analysis.toString();

        } catch (Exception e) {
            ClearAILogger.error("分析文件失败: " + e.getMessage(), e);
            return "❌ 分析文件时出错: " + e.getMessage();
        }
    }

    /**
     * 列出目录内容
     */
    @ReActTool(
        name = "list_directory",
        description = "列出指定目录的内容，可按大小、时间、类型等排序。",
        category = "file"
    )
    public String listDirectory(
        @ToolParam(name = "path", description = "要列出内容的目录路径", required = true) String path,
        @ToolParam(name = "sort_by", description = "排序方式: name(名称), size(大小), time(时间), type(类型)", required = false, defaultValue = "name") String sortBy,
        @ToolParam(name = "include_hidden", description = "是否包含隐藏文件", required = false, defaultValue = "false") boolean includeHidden
    ) {
        try {
            File directory = new File(path);
            if (!directory.exists()) {
                return "❌ 目录不存在: " + path;
            }

            if (!directory.isDirectory()) {
                return "❌ 指定路径不是目录: " + path;
            }

            File[] files = directory.listFiles();
            if (files == null) {
                return "❌ 无法访问目录内容: " + path;
            }

            // 过滤隐藏文件
            List<File> fileList = new ArrayList<>();
            for (File file : files) {
                if (includeHidden || !file.isHidden()) {
                    fileList.add(file);
                }
            }

            // 排序
            switch (sortBy.toLowerCase()) {
                case "size":
                    fileList.sort((a, b) -> Long.compare(b.length(), a.length()));
                    break;
                case "time":
                    fileList.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                    break;
                case "type":
                    fileList.sort((a, b) -> {
                        String extA = getFileExtension(a.getName());
                        String extB = getFileExtension(b.getName());
                        return extA.compareToIgnoreCase(extB);
                    });
                    break;
                default: // name
                    fileList.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                    break;
            }

            // 格式化输出
            StringBuilder result = new StringBuilder();
            result.append("📁 目录内容: ").append(path).append("\n");
            result.append(String.format("共 %d 个项目 (排序: %s)\n", fileList.size(), sortBy));
            result.append("─".repeat(80)).append("\n");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            for (File file : fileList) {
                char typeChar = file.isDirectory() ? '📁' : '📄';
                String sizeStr = file.isDirectory() ? "[目录]" : formatFileSize(file.length());
                String dateStr = dateFormat.format(new Date(file.lastModified()));

                result.append(String.format("%c %-30s %10s %s\n",
                    typeChar, truncateName(file.getName(), 30), sizeStr, dateStr));
            }

            return result.toString();

        } catch (Exception e) {
            ClearAILogger.error("列出目录内容失败: " + e.getMessage(), e);
            return "❌ 列出目录内容时出错: " + e.getMessage();
        }
    }

    /**
     * 搜索文件
     */
    @ReActTool(
        name = "search_files",
        description = "在指定目录中搜索文件，支持文件名模式匹配和文件类型过滤。",
        category = "file"
    )
    public String searchFiles(
        @ToolParam(name = "directory", description = "搜索目录", required = true) String directory,
        @ToolParam(name = "pattern", description = "搜索模式，支持通配符(*,?)", required = false, defaultValue = "*") String pattern,
        @ToolParam(name = "file_type", description = "文件类型过滤，如: txt, jpg, log", required = false) String fileType,
        @ToolParam(name = "max_results", description = "最大结果数量", required = false, defaultValue = "20") int maxResults
    ) {
        try {
            File searchDir = new File(directory);
            if (!searchDir.exists()) {
                return "❌ 搜索目录不存在: " + directory;
            }

            if (!searchDir.isDirectory()) {
                return "❌ 指定路径不是目录: " + directory;
            }

            List<File> foundFiles = new ArrayList<>();
            searchFilesRecursive(searchDir, pattern, fileType, foundFiles, 0, maxResults);

            StringBuilder result = new StringBuilder();
            result.append("🔍 文件搜索结果\n");
            result.append(String.format("搜索目录: %s\n", directory));
            result.append(String.format("搜索模式: %s\n", pattern));
            if (fileType != null && !fileType.isEmpty()) {
                result.append(String.format("文件类型: %s\n", fileType));
            }
            result.append(String.format("找到文件: %d 个\n", foundFiles.size()));
            result.append("─".repeat(80)).append("\n");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            for (File file : foundFiles) {
                String relativePath = file.getAbsolutePath().substring(directory.length());
                String sizeStr = file.isDirectory() ? "[目录]" : formatFileSize(file.length());
                String dateStr = dateFormat.format(new Date(file.lastModified()));

                result.append(String.format("📄 %-40s %10s %s\n",
                    truncatePath(relativePath, 40), sizeStr, dateStr));
            }

            if (foundFiles.size() >= maxResults) {
                result.append(String.format("\n⚠️ 结果已限制为前 %d 项\n", maxResults));
            }

            return result.toString();

        } catch (Exception e) {
            ClearAILogger.error("搜索文件失败: " + e.getMessage(), e);
            return "❌ 搜索文件时出错: " + e.getMessage();
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex + 1) : "";
    }

    /**
     * 确定文件类型
     */
    private String determineFileType(String fileName) {
        String ext = getFileExtension(fileName).toLowerCase();

        // 常见文件类型映射
        switch (ext) {
            case "txt": case "md": case "log": return "文本文件";
            case "jpg": case "jpeg": case "png": case "gif": case "bmp": return "图片文件";
            case "mp4": case "avi": case "mkv": case "mov": return "视频文件";
            case "mp3": case "wav": case "flac": return "音频文件";
            case "pdf": return "PDF文档";
            case "doc": case "docx": return "Word文档";
            case "xls": case "xlsx": return "Excel表格";
            case "ppt": case "pptx": return "PowerPoint演示文稿";
            case "zip": case "rar": case "7z": return "压缩文件";
            case "exe": return "可执行文件";
            case "dll": return "动态链接库";
            case "tmp": case "temp": return "临时文件";
            case "class": case "jar": return "Java文件";
            default:
                if (ext.isEmpty()) return "无扩展名文件";
                return ext.toUpperCase() + " 文件";
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * 截断文件名
     */
    private String truncateName(String name, int maxLength) {
        if (name.length() <= maxLength) return name;
        return name.substring(0, maxLength - 3) + "...";
    }

    /**
     * 截断路径
     */
    private String truncatePath(String path, int maxLength) {
        if (path.length() <= maxLength) return path;
        return "..." + path.substring(path.length() - maxLength + 3);
    }

    /**
     * 递归搜索文件
     */
    private void searchFilesRecursive(File dir, String pattern, String fileType,
                                    List<File> results, int depth, int maxResults) {
        if (results.size() >= maxResults || depth > 5) return; // 限制搜索深度和结果数量

        try {
            File[] files = dir.listFiles();
            if (files == null) return;

            for (File file : files) {
                if (results.size() >= maxResults) break;

                if (file.isDirectory()) {
                    searchFilesRecursive(file, pattern, fileType, results, depth + 1, maxResults);
                } else {
                    // 检查文件名匹配
                    if (matchesPattern(file.getName(), pattern)) {
                        // 检查文件类型过滤
                        if (fileType == null || fileType.isEmpty() ||
                            getFileExtension(file.getName()).equalsIgnoreCase(fileType)) {
                            results.add(file);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略搜索过程中的错误
        }
    }

    /**
     * 简单的模式匹配（支持*和?）
     */
    private boolean matchesPattern(String fileName, String pattern) {
        // 转换为正则表达式
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");

        return fileName.matches(regex);
    }
}