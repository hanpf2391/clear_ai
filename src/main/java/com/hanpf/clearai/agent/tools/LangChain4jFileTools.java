package com.hanpf.clearai.agent.tools;

import dev.langchain4j.agent.tool.Tool;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 基于LangChain4j的文件工具集
 */
public class LangChain4jFileTools {

    /**
     * 分析文件或目录详细信息
     */
    @Tool("分析指定文件或目录的详细信息，包括大小、创建时间、修改时间、文件类型等")
    public String analyzeFile(String path) {
        try {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                return "❌ 文件或目录不存在: " + path;
            }

            File file = filePath.toFile();
            StringBuilder info = new StringBuilder();

            info.append("📄 文件分析: ").append(path).append("\n");
            info.append("类型: ").append(file.isDirectory() ? "目录" : "文件").append("\n");
            info.append("大小: ").append(String.format("%.2f MB", file.length() / (1024.0 * 1024.0))).append("\n");

            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    info.append("包含项目数: ").append(files.length).append("\n");

                    long totalSize = Arrays.stream(files)
                        .mapToLong(File::length)
                        .sum();
                    info.append("总大小: ").append(String.format("%.2f MB", totalSize / (1024.0 * 1024.0))).append("\n");
                }
            }

            return info.toString();

        } catch (Exception e) {
            return "❌ 分析文件失败: " + e.getMessage();
        }
    }

    /**
     * 列出目录内容
     */
    @Tool("列出指定目录的内容，可按大小、时间、类型等排序")
    public String listDirectory(String path) {
        try {
            File directory = new File(path);
            if (!directory.exists() || !directory.isDirectory()) {
                return "❌ 目录不存在或不是目录: " + path;
            }

            File[] files = directory.listFiles();
            if (files == null) {
                return "❌ 无法读取目录内容: " + path;
            }

            StringBuilder listing = new StringBuilder();
            listing.append("📁 目录内容: ").append(path).append("\n");

            // 按类型和大小排序
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    if (f1.isDirectory() && f2.isFile()) return -1;
                    if (f1.isFile() && f2.isDirectory()) return 1;
                    return Long.compare(f2.length(), f1.length()); // 大文件在前
                }
            });

            listing.append("📂 目录:\n");
            for (File file : files) {
                if (file.isDirectory()) {
                    listing.append(String.format("  📁 %s/\n", file.getName()));
                }
            }

            listing.append("\n📄 文件:\n");
            for (File file : files) {
                if (file.isFile()) {
                    listing.append(String.format("  📄 %s (%.2f MB)\n",
                        file.getName(), file.length() / (1024.0 * 1024.0)));
                }
            }

            return listing.toString();

        } catch (Exception e) {
            return "❌ 列出目录失败: " + e.getMessage();
        }
    }
}