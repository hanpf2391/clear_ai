package com.hanpf.clearai.test;

import com.hanpf.clearai.config.AIConfigManager;
import com.hanpf.clearai.utils.ClearAILogger;

/**
 * 配置验证测试
 * 验证用户自定义的MAX_TOKENS是否正确加载
 */
public class ConfigValidationTest {
    public static void main(String[] args) {
        System.out.println("=== ClearAI 配置验证 ===\n");

        // 验证配置加载
        boolean configComplete = AIConfigManager.isConfigComplete();
        System.out.println("配置完整性: " + (configComplete ? "✅ 完整" : "❌ 不完整"));

        if (configComplete) {
            // 显示关键配置
            System.out.println("\n📋 当前配置:");
            System.out.println("Max Tokens: " + AIConfigManager.getMaxTokens());
            System.out.println("Model: " + AIConfigManager.getCurrentModel());
            System.out.println("Provider: " + AIConfigManager.getProviderName());
            System.out.println("API URL: " + AIConfigManager.getApiUrl());
            System.out.println("Timeout: " + AIConfigManager.getTimeout() + "秒");
            System.out.println("Temperature: " + AIConfigManager.getTemperature());

            // Token使用建议
            System.out.println("\n💡 Token使用建议:");
            int maxTokens = AIConfigManager.getMaxTokens();

            if (maxTokens < 1500) {
                System.out.println("⚠️  当前设置较小，适合日常小规模清理");
                System.out.println("   建议: 处理复杂文件时可能遇到截断");
            } else if (maxTokens <= 3000) {
                System.out.println("✅ 设置合理，适合中等规模清理");
                System.out.println("   适用: 20-100个文件的典型场景");
            } else if (maxTokens <= 6000) {
                System.out.println("🎯 设置良好，适合企业级分析");
                System.out.println("   适用: 100-500个文件的深度分析");
            } else {
                System.out.println("🚀 设置很大，适合大规模分析");
                System.out.println("   注意: 确保API配额充足");
            }

            // 预估成本
            System.out.println("\n💰 成本估算:");
            System.out.println("以当前设置(" + maxTokens + " tokens)，");
            System.out.println("每次AI调用成本 ≈ " + estimateCost(maxTokens) + " 元");

            // 根据maxTokens给出使用建议
            System.out.println("\n🎯 使用建议:");
            giveRecommendation(maxTokens);

        } else {
            System.out.println("❌ 配置不完整，请检查 setting.json 文件");
        }

        // 记录到日志
        ClearAILogger.logConfiguration("Configuration validation",
            "MaxTokens: " + AIConfigManager.getMaxTokens() +
            ", Complete: " + configComplete);
    }

    /**
     * 估算成本（简化版）
     * 智于智谱AI的大概定价
     */
    private static double estimateCost(int tokens) {
        // 智谱AI大致定价：0.001元/1000 tokens（简化估算）
        return (double) tokens / 1000 * 0.001;
    }

    /**
     * 根据token数量给出使用建议
     */
    private static void giveRecommendation(int maxTokens) {
        if (maxTokens <= 1000) {
            System.out.println("🏠 适合: 日常清理，小文件处理");
            System.out.println("📁 建议范围: Downloads, Temp, 少量文档");
        } else if (maxTokens <= 2000) {
            System.out.println("🏠 适合: 家庭用户，标准清理");
            System.out.println("📁 建议范围: 用户文档，临时文件，系统缓存");
        } else if (maxTokens <= 4000) {
            System.out.println("💼 适合: 办公用户，深度清理");
            System.out.println("📁 建议范围: 大型文档，日志文件，项目缓存");
        } else if (maxTokens <= 6000) {
            System.out.println("🏢 适合: 企业用户，全面分析");
            System.out.println("📁 建议范围: 整个用户目录，系统分析");
        } else {
            System.out.println("🔬 适合: 专业用户，超级分析");
            System.out.println("📁 建议范围: C盘全盘，大规模文件分析");
        }

        System.out.println("\n📊 性能提示:");
        System.out.println("• 更大的maxTokens = 更详细的AI分析");
        System.out.println("• 但也会 = 更慢的响应和更高的成本");
        System.out.println("• 建议根据实际需求调整");
    }
}