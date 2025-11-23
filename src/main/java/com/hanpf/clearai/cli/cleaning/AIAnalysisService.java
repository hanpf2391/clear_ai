package com.hanpf.clearai.cli.cleaning;

import com.hanpf.clearai.cli.cleaning.models.AnalysisResult;
import com.hanpf.clearai.cli.cleaning.models.FileInfo;
import com.hanpf.clearai.config.AIConfig;
import com.hanpf.clearai.service.ChatService;
import com.hanpf.clearai.utils.ClearAILogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI分析服务 - 使用现有的AIConfig集成
 */
public class AIAnalysisService {
    private final ChatService chatService;
    private final Gson gson;
    private final boolean useMockAI;

    public AIAnalysisService() {
        this.chatService = AIConfig.createChatService();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.useMockAI = chatService == null;
    }

    /**
     * 获取ChatService实例用于普通聊天
     */
    public ChatService getChatService() {
        return chatService;
    }

    /**
     * 分析文件列表
     */
    public AnalysisResult analyzeFiles(List<FileInfo> files, String scanPath, String userIntent) {
        if (useMockAI) {
            System.out.println("使用模拟AI分析模式");
            return mockAnalyzeFiles(files, scanPath);
        }

        try {
            // 构建文件列表JSON
            String fileListJson = buildFileListJson(files);

            // 构建分析Prompt
            String prompt = buildAnalysisPrompt(fileListJson, userIntent);

            // 使用现有的ChatService调用AI服务
            String aiResponse = chatService.chat(prompt);

            // 解析响应
            return parseAIResponse(aiResponse, files, scanPath);

        } catch (Exception e) {
            // 如果AI服务失败，回退到模拟分析
            System.err.println("AI分析失败，使用模拟分析: " + e.getMessage());
            return mockAnalyzeFiles(files, scanPath);
        }
    }

    /**
     * 生成清理命令
     */
    public List<String> generateCleanupCommands(String userInstruction, AnalysisResult context) {
        if (useMockAI) {
            return mockGenerateCommands(userInstruction, context);
        }

        try {
            String prompt = buildCommandPrompt(userInstruction, context);
            String aiResponse = chatService.chat(prompt);
            return extractCommandsFromResponse(aiResponse);
        } catch (Exception e) {
            System.err.println("AI命令生成失败，使用模拟生成: " + e.getMessage());
            return mockGenerateCommands(userInstruction, context);
        }
    }

    /**
     * 构建文件列表JSON
     */
    private String buildFileListJson(List<FileInfo> files) {
        List<Map<String, Object>> fileList = new ArrayList<>();

        for (FileInfo file : files) {
            Map<String, Object> fileMap = new HashMap<>();
            fileMap.put("name", file.getName());
            fileMap.put("path", file.getFullPath());
            fileMap.put("size", file.getFormattedSize());
            fileMap.put("sizeBytes", file.getSize());
            fileMap.put("lastModified", file.getFormattedLastModified());
            fileMap.put("extension", file.getExtension());
            fileMap.put("isDirectory", file.isDirectory());
            fileMap.put("isSafeToDelete", file.isSafeToDelete());
            fileMap.put("isDangerous", file.isDangerous());
            fileList.add(fileMap);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("files", fileList);
        request.put("totalFiles", files.size());
        request.put("scanTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return gson.toJson(request);
    }

    /**
     * 构建分析Prompt
     */
    private String buildAnalysisPrompt(String fileListJson, String userIntent) {
        return String.format("""
            你是一个专业的文件清理专家。我给你一份文件列表，请分析并分类：

            用户意图：%s

            任务：
            1. 将文件分为两类：Safe（放心删）和 Review（需确认）
            2. 对**每一类文件**都要生成详细的摘要和建议
            3. 分析文件的时间特征、类型特征和用途
            4. 统计总体情况

            返回JSON格式：
            {
              "summary": {
                "totalFiles": 156,
                "totalSize": "2.4GB",
                "scanPath": "D:\\\\Downloads",
                "scanDate": "2024-11-20"
              },
              "safeGroups": [
                {
                  "name": "临时文件",
                  "description": "*.tmp, *.log",
                  "totalSize": "500MB",
                  "fileCount": 120,
                  "summary": "2024年11月生成的临时文件，包含系统日志和缓存",
                  "advice": "可安全删除，不影响系统正常运行"
                }
              ],
              "reviewItems": [
                {
                  "fileName": "photoshop_installer.exe",
                  "filePath": "D:\\\\Downloads\\\\photoshop_installer.exe",
                  "size": "1.2GB",
                  "lastModified": "2023-06-15",
                  "fileType": "应用程序安装包",
                  "summary": "Photoshop安装程序包，下载于2023年6月",
                  "advice": "如果已经安装了Photoshop，这个安装包可以删除"
                }
              ]
            }

            分析要求：
            - Safe类：必须是明确可以删除的文件（临时文件、缓存、日志等）
            - Review类：可能有价值的文件，需要用户确认
            - 摘要要简洁：50字以内，包含关键信息
            - 建议要明确：直接给出处理建议
            - 特别注意大文件（>100MB）
            - 确保JSON格式完整，不要截断

            文件列表：
            %s
            """, userIntent, fileListJson);
    }

    
    /**
     * 解析AI响应
     */
    private AnalysisResult parseAIResponse(String aiResponse, List<FileInfo> files, String scanPath) {
        try {
            // 记录AI响应到日志文件
            ClearAILogger.logAIResponse("File analysis request for: " + scanPath, aiResponse);

            // 尝试提取JSON部分
            String jsonPart = extractJsonFromResponse(aiResponse);

            // 检查JSON完整性
            if (!isJsonComplete(jsonPart)) {
                ClearAILogger.logJSONError("incomplete_json_response", jsonPart,
                    new Exception("JSON response appears to be incomplete"));
                ClearAILogger.info("JSON incomplete, falling back to mock analysis");
                return mockAnalyzeFiles(files, scanPath);
            }

            // 记录JSON提取操作到日志
            ClearAILogger.logJSON("extractJsonFromResponse", jsonPart.substring(0, Math.min(200, jsonPart.length())) + "...", "extracted");

            // 解析完整的响应结构
            Map<String, Object> response = gson.fromJson(jsonPart, Map.class);

            if (response == null) {
                ClearAILogger.logJSONError("gson.fromJson", jsonPart, new Exception("Parsed response is null"));
                return mockAnalyzeFiles(files, scanPath);
            }

            AnalysisResult result = new AnalysisResult();

            // 解析摘要
            Map<String, Object> summaryMap = (Map<String, Object>) response.get("summary");
            if (summaryMap == null) {
                ClearAILogger.logJSONError("missing summary field", jsonPart, new Exception("Summary field is null"));
                return mockAnalyzeFiles(files, scanPath);
            }

            AnalysisResult.Summary summary = new AnalysisResult.Summary(
                ((Number) summaryMap.get("totalFiles")).intValue(),
                (String) summaryMap.get("totalSize"),
                (String) summaryMap.get("scanPath"),
                (String) summaryMap.get("scanDate")
            );
            result.setSummary(summary);

            // 解析安全组
            List<Map<String, Object>> safeGroupsList = (List<Map<String, Object>>) response.get("safeGroups");
            if (safeGroupsList != null) {
                for (Map<String, Object> groupMap : safeGroupsList) {
                    AnalysisResult.SafeGroup group = new AnalysisResult.SafeGroup(
                        (String) groupMap.get("name"),
                        (String) groupMap.get("description"),
                        (String) groupMap.get("totalSize"),
                        ((Number) groupMap.get("fileCount")).intValue(),
                        (String) groupMap.get("summary"),
                        (String) groupMap.get("advice")
                    );
                    result.addSafeGroup(group);
                }
            }

            // 解析需要审查的项目
            List<Map<String, Object>> reviewItemsList = (List<Map<String, Object>>) response.get("reviewItems");
            if (reviewItemsList != null) {
                for (Map<String, Object> itemMap : reviewItemsList) {
                    AnalysisResult.ReviewItem item = new AnalysisResult.ReviewItem(
                        (String) itemMap.get("fileName"),
                        (String) itemMap.get("filePath"),
                        (String) itemMap.get("size"),
                        (String) itemMap.get("lastModified"),
                        (String) itemMap.get("fileType"),
                        (String) itemMap.get("summary"),
                        (String) itemMap.get("advice")
                    );
                    result.addReviewItem(item);
                }
            }

            return result;

        } catch (JsonSyntaxException e) {
            // 记录JSON解析错误到日志，不在控制台显示
            ClearAILogger.logJSONError("parseAIResponse", aiResponse, e);
            // 回退到模拟分析
            return mockAnalyzeFiles(files, scanPath);
        } catch (Exception e) {
            // 记录其他解析错误到日志
            ClearAILogger.error("AI response parsing failed for path: " + scanPath, e);
            return mockAnalyzeFiles(files, scanPath);
        }
    }

    /**
     * 检查JSON是否完整
     */
    private boolean isJsonComplete(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }

        String trimmed = json.trim();

        // 基本结构检查
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return false;
        }

        // 检查必要的字段是否存在
        if (!trimmed.contains("\"summary\"") ||
            !trimmed.contains("\"safeGroups\"") ||
            !trimmed.contains("\"reviewItems\"")) {
            return false;
        }

        // 检查括号平衡
        int braceCount = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                }
            }
        }

        return braceCount == 0;
    }

    /**
     * 从响应中提取JSON部分
     */
    private String extractJsonFromResponse(String response) {
        // 查找JSON代码块
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1);
        }

        return response;
    }

    /**
     * 模拟AI分析 - 备选方案
     */
    private AnalysisResult mockAnalyzeFiles(List<FileInfo> files, String scanPath) {
        AnalysisResult result = new AnalysisResult();

        // 计算统计信息
        long totalSize = files.stream().mapToLong(FileInfo::getSize).sum();
        String totalSizeFormatted = formatFileSize(totalSize);

        AnalysisResult.Summary summary = new AnalysisResult.Summary(
            files.size(),
            totalSizeFormatted,
            scanPath,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );
        result.setSummary(summary);

        // 分类文件
        List<FileInfo> safeFiles = new ArrayList<>();
        List<FileInfo> reviewFiles = new ArrayList<>();

        for (FileInfo file : files) {
            if (file.isSafeToDelete()) {
                safeFiles.add(file);
            } else if (!file.isDirectory()) {
                reviewFiles.add(file);
            }
        }

        // 按类型分组安全文件
        Map<String, List<FileInfo>> safeFileGroups = safeFiles.stream()
                .collect(Collectors.groupingBy(f -> f.getExtension().isEmpty() ? "unknown" : f.getExtension()));

        for (Map.Entry<String, List<FileInfo>> entry : safeFileGroups.entrySet()) {
            String extension = entry.getKey();
            List<FileInfo> groupFiles = entry.getValue();

            long groupSize = groupFiles.stream().mapToLong(FileInfo::getSize).sum();

            AnalysisResult.SafeGroup group = new AnalysisResult.SafeGroup(
                getExtensionGroupName(extension),
                "*." + extension,
                formatFileSize(groupSize),
                groupFiles.size(),
                getExtensionSummary(extension, groupFiles),
                getExtensionAdvice(extension)
            );
            result.addSafeGroup(group);
        }

        // 添加需要审查的文件
        for (FileInfo file : reviewFiles.stream()
                .sorted((a, b) -> Long.compare(b.getSize(), a.getSize()))
                .limit(10) // 只显示前10个最大的文件
                .collect(Collectors.toList())) {

            AnalysisResult.ReviewItem item = new AnalysisResult.ReviewItem(
                file.getName(),
                file.getFullPath(),
                file.getFormattedSize(),
                file.getFormattedLastModified(),
                getFileTypeDescription(file),
                getFileSummary(file),
                getFileAdvice(file)
            );
            result.addReviewItem(item);
        }

        return result;
    }

    /**
     * 模拟生成清理命令
     */
    private List<String> mockGenerateCommands(String userInstruction, AnalysisResult context) {
        List<String> commands = new ArrayList<>();
        String lowerInstruction = userInstruction.toLowerCase();

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        if (lowerInstruction.contains("放心删") || lowerInstruction.contains("safe")) {
            for (AnalysisResult.SafeGroup group : context.getSafeGroups()) {
                String pattern = group.getDescription();
                if (isWindows) {
                    commands.add(String.format("del /s /q \"%s\\%s\"",
                        context.getSummary().getScanPath(), pattern));
                } else {
                    commands.add(String.format("find \"%s\" -name \"%s\" -delete",
                        context.getSummary().getScanPath(), pattern));
                }
            }
        }

        if (lowerInstruction.contains("备份") || lowerInstruction.contains("backup")) {
            for (AnalysisResult.ReviewItem item : context.getReviewItems()) {
                if (item.getFileName().toLowerCase().contains("backup") ||
                    item.getFileName().toLowerCase().contains("zip")) {
                    if (isWindows) {
                        commands.add(String.format("echo \"保留备份文件: %s\"", item.getFileName()));
                    }
                }
            }
        }

        return commands;
    }

    /**
     * 构建命令生成Prompt
     */
    private String buildCommandPrompt(String userInstruction, AnalysisResult context) {
        return String.format("""
            根据用户指令和文件分析结果，生成相应的清理命令。

            用户指令：%s

            分析结果：
            %s

            请生成具体的系统命令来执行用户的清理需求。
            只返回命令列表，不需要解释。
            每行一个命令。
            """, userInstruction, gson.toJson(context));
    }

    /**
     * 从AI响应中提取命令
     */
    private List<String> extractCommandsFromResponse(String aiResponse) {
        List<String> commands = new ArrayList<>();

        // 查找代码块中的命令
        String[] lines = aiResponse.split("\\n");
        boolean inCodeBlock = false;

        for (String line : lines) {
            if (line.contains("```")) {
                inCodeBlock = !inCodeBlock;
                continue;
            }

            if (inCodeBlock && !line.trim().isEmpty()) {
                commands.add(line.trim());
            }
        }

        return commands;
    }

    // 辅助方法

    private String formatFileSize(long size) {
        if (size < 1024) return size + "B";
        if (size < 1024 * 1024) return String.format("%.1fKB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1fMB", size / (1024.0 * 1024.0));
        return String.format("%.1fGB", size / (1024.0 * 1024.0 * 1024.0));
    }

    private String getExtensionGroupName(String extension) {
        switch (extension.toLowerCase()) {
            case "tmp": return "临时文件";
            case "log": return "日志文件";
            case "cache": return "缓存文件";
            case "bak": return "备份文件";
            case "temp": return "临时文件";
            default: return extension.toUpperCase() + "文件";
        }
    }

    private String getExtensionSummary(String extension, List<FileInfo> files) {
        long totalSize = files.stream().mapToLong(FileInfo::getSize).sum();
        return String.format("%s个%s文件，总计%s",
            files.size(), getExtensionGroupName(extension), formatFileSize(totalSize));
    }

    private String getExtensionAdvice(String extension) {
        switch (extension.toLowerCase()) {
            case "tmp":
            case "temp":
                return "临时文件，可安全删除";
            case "log":
                return "日志文件，可删除但建议保留最近的";
            case "cache":
                return "缓存文件，删除后程序会重新生成";
            case "bak":
                return "备份文件，建议确认不再需要后删除";
            default:
                return "可以安全删除的文件";
        }
    }

    private String getFileTypeDescription(FileInfo file) {
        String ext = file.getExtension().toLowerCase();
        if (file.isDangerous()) {
            return "可执行文件";
        } else if (Arrays.asList("jpg", "png", "gif", "bmp", "svg").contains(ext)) {
            return "图片文件";
        } else if (Arrays.asList("mp4", "avi", "mkv", "mov", "wmv").contains(ext)) {
            return "视频文件";
        } else if (Arrays.asList("mp3", "wav", "flac", "m4a").contains(ext)) {
            return "音频文件";
        } else if (Arrays.asList("pdf", "doc", "docx", "txt", "rtf").contains(ext)) {
            return "文档文件";
        } else if (Arrays.asList("zip", "rar", "7z", "tar", "gz").contains(ext)) {
            return "压缩文件";
        } else {
            return ext.isEmpty() ? "文件" : ext.toUpperCase() + "文件";
        }
    }

    private String getFileSummary(FileInfo file) {
        return String.format("%s文件，大小为%s，最后修改于%s",
            getFileTypeDescription(file),
            file.getFormattedSize(),
            file.getFormattedLastModified());
    }

    private String getFileAdvice(FileInfo file) {
        if (file.isDangerous()) {
            return "可执行文件，请确认来源安全后再删除";
        } else if (file.getSize() > 100 * 1024 * 1024) {
            return "大文件，建议确认内容后再决定是否删除";
        } else {
            return "需要您确认是否仍需要此文件";
        }
    }
}