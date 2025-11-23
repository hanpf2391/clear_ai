package com.hanpf.clearai.test;

import com.hanpf.clearai.config.AIConfigManager;
import com.hanpf.clearai.utils.ClearAILogger;

/**
 * Token分析测试
 * 用于诊断AI响应截断问题
 */
public class TokenAnalysisTest {
    public static void main(String[] args) {
        System.out.println("=== Token Usage Analysis ===\n");

        // 检查当前配置
        System.out.println("当前配置:");
        System.out.println("Max Tokens: " + AIConfigManager.getMaxTokens());
        System.out.println("Model: " + AIConfigManager.getCurrentModel());
        System.out.println("Provider: " + AIConfigManager.getProviderName());
        System.out.println("Timeout: " + AIConfigManager.getTimeout() + "s");
        System.out.println();

        // 估算JSON响应的token数量
        String sampleJson = generateSampleJson();
        int estimatedTokens = estimateTokens(sampleJson);

        System.out.println("示例JSON分析:");
        System.out.println("字符数: " + sampleJson.length());
        System.out.println("估算Token数: " + estimatedTokens);
        System.out.println("当前MaxTokens: " + AIConfigManager.getMaxTokens());
        System.out.println("剩余空间: " + (AIConfigManager.getMaxTokens() - estimatedTokens));
        System.out.println();

        if (estimatedTokens >= AIConfigManager.getMaxTokens()) {
            System.out.println("⚠️  警告: 估算的Token数已超过或接近限制！");
            System.out.println("建议增加maxTokens到 " + (estimatedTokens + 500));
        } else {
            System.out.println("✅ Token使用在安全范围内");
        }

        System.out.println("\n=== 示例JSON内容 ===");
        System.out.println(sampleJson);

        // 记录到日志
        ClearAILogger.info("Token analysis completed - Estimated: " + estimatedTokens +
                          ", Config: " + AIConfigManager.getMaxTokens());
    }

    /**
     * 生成示例JSON响应
     */
    private static String generateSampleJson() {
        return "{\n" +
               "  \"summary\": {\n" +
               "    \"totalFiles\": 6,\n" +
               "    \"totalSize\": \"27.1MB\",\n" +
               "    \"scanPath\": \"C:\\\\Users\\\\\\Downloads\",\n" +
               "    \"scanDate\": \"2025-11-21\"\n" +
               "  },\n" +
               "  \"safeGroups\": [\n" +
               "    {\n" +
               "      \"name\": \"临时截图文件\",\n" +
               "      \"description\": \"*.png 网络截图\",\n" +
               "      \"totalSize\": \"20.3KB\",\n" +
               "      \"fileCount\": 1,\n" +
               "      \"summary\": \"网络截图文件，文件大小仅20.3KB，可能是临时性网络页面截图\",\n" +
               "      \"advice\": \"可安全删除，这类截图通常没有保留价值，除非包含重要信息\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"reviewItems\": [\n" +
               "    {\n" +
               "      \"fileName\": \"job-info.log\",\n" +
               "      \"filePath\": \"C:\\\\Users\\\\\\Downloads\\\\\\job-info.log\",\n" +
               "      \"size\": \"26.7MB\",\n" +
               "      \"lastModified\": \"2025-11-21 09:04\",\n" +
               "      \"fileType\": \"工作日志文件\",\n" +
               "      \"summary\": \"26.7MB的大型日志文件，今天早上刚修改过，可能包含重要的工作记录或系统日志\",\n" +
               "      \"advice\": \"建议先检查文件内容，确认是否还需要保留。如果确认是过期的日志，可以删除以释放空间\"\n" +
               "    },\n" +
               "    {\n" +
               "      \"fileName\": \"合同协议电子签署电子签文件.zip\",\n" +
               "      \"filePath\": \"C:\\\\Users\\\\\\Downloads\\\\\\合同协议电子签署电子签文件.zip\",\n" +
               "      \"size\": \"999.8KB\",\n" +
               "      \"lastModified\": \"2024-11-05 09:20\",\n" +
               "      \"fileType\": \"法律文档压缩包\",\n" +
               "      \"summary\": \"合同相关文档，文件大小约1MB，最后修改于2024年11月，是一年前的法律文件\",\n" +
               "      \"advice\": \"法律文件通常需要长期保存，建议先确认是否还有法律效力或备份需求，谨慎处理\"\n" +
               "    },\n" +
               "    {\n" +
               "      \"fileName\": \"avatar2-128x128.png\",\n" +
               "      \"filePath\": \"C:\\\\Users\\\\\\Downloads\\\\\\avatar2-128x128.png\",\n" +
               "      \"size\": \"40.1KB\",\n" +
               "      \"lastModified\": \"2025-07-21 18:15\",\n" +
               "      \"fileType\": \"用户头像图片\",\n" +
               "      \"summary\": \"用户头像图片，40.1KB的小文件，7月份创建，可能是个人账户的头像\",\n" +
               "      \"advice\": \"如果这是程序自动生成的临时头像，可以考虑删除；如果是个人重要的头像图片，建议保留或备份到专门的相册目录\"\n" +
               "    }\n" +
               "  ]\n" +
               "}";
    }

    /**
     * 粗略估算token数量
     * 简单规则：约4个字符 ≈ 1个token
     */
    private static int estimateTokens(String text) {
        return (int) Math.ceil(text.length() / 4.0);
    }
}