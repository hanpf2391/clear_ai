package com.hanpf.clearai.test;

import com.hanpf.clearai.cli.cleaning.AIAnalysisService;
import com.hanpf.clearai.cli.cleaning.models.FileInfo;
import com.hanpf.clearai.cli.cleaning.models.AnalysisResult;
import com.hanpf.clearai.utils.ClearAILogger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件扫描测试
 * 专门测试AI响应截断问题修复
 */
public class FileScanTest {
    public static void main(String[] args) {
        System.out.println("=== 文件扫描AI测试 ===\n");

        try {
            // 创建分析服务
            AIAnalysisService analysisService = new AIAnalysisService();

            // 扫描Downloads目录
            String scanPath = "Downloads";
            List<FileInfo> files = scanDirectory(scanPath);

            if (files.isEmpty()) {
                System.out.println("没有找到文件，退出测试");
                return;
            }

            System.out.println("扫描到 " + files.size() + " 个文件");
            System.out.println("开始AI分析...\n");

            // AI分析
            AnalysisResult result = analysisService.analyzeFiles(files, scanPath, "请分析这些文件");

            // 显示结果
            System.out.println("分析结果:");
            System.out.println("总文件数: " + result.getSummary().getTotalFiles());
            System.out.println("总大小: " + result.getSummary().getTotalSize());
            System.out.println("扫描路径: " + result.getSummary().getScanPath());

            System.out.println("\n放心删除的文件组: " + result.getSafeGroups().size());
            System.out.println("需要确认的文件: " + result.getReviewItems().size());

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<FileInfo> scanDirectory(String path) {
        List<FileInfo> files = new ArrayList<>();
        try {
            Path dirPath = Paths.get(path);
            if (!Files.exists(dirPath)) {
                System.out.println("目录不存在: " + path);
                return files;
            }

            Files.walk(dirPath)
                .filter(Files::isRegularFile)
                .forEach(filePath -> {
                    File file = filePath.toFile();
                    if (file.length() > 0) { // 只处理非空文件
                        FileInfo fileInfo = new FileInfo(
                            file.getName(),
                            file.getAbsolutePath(),
                            file.length(),
                            file.lastModified(),
                            false  // 不是目录
                        );
                        files.add(fileInfo);
                    }
                });

        } catch (Exception e) {
            System.err.println("扫描目录失败: " + e.getMessage());
        }
        return files;
    }
}