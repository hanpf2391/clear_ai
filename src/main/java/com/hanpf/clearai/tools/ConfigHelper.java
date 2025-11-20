package com.hanpf.clearai.tools;

import com.hanpf.clearai.config.AIConfigManager;
import com.hanpf.clearai.config.JsonConfigParser;

/**
 * 配置助手工具类 - 简化版本
 * 只支持外置setting.json配置文件
 */
public class ConfigHelper {

    /**
     * 创建配置文件模板
     */
    public static boolean createConfig() {
        boolean success = AIConfigManager.createConfigTemplate();
        if (success) {
            System.out.println("✅ 已创建配置文件: setting.json");
            System.out.println("📝 请编辑此文件，填入你的API配置信息");
            System.out.println("🔧 配置文件位置: " + JsonConfigParser.getConfigFilePath());
        } else {
            System.out.println("❌ 创建配置文件失败");
        }
        return success;
    }

    /**
     * 重新加载配置
     */
    public static void reloadConfig() {
        AIConfigManager.reloadConfig();
        System.out.println("🔄 配置已重新加载");
        System.out.println("📊 当前配置模式: " + AIConfigManager.getConfigMode());
    }

    /**
     * 显示当前配置信息
     */
    public static String showCurrentConfig() {
        return AIConfigManager.getConfigStatus();
    }

    /**
     * 显示配置文件示例
     */
    public static String showConfigExample() {
        return """
            === 配置文件示例 (setting.json) ===
            {
              "env": {
                "API_KEY": "your-api-key-here",
                "BASE_URL": "https://open.bigmodel.cn/api/paas/v4/chat/completions",
                "MODEL": "glm-4.5-air",
                "PROVIDER_NAME": "智普AI",
                "API_TIMEOUT_MS": 60000,
                "TEMPERATURE": 0.7,
                "MAX_TOKENS": 1000
              },
              "permissions": {
                "allow": [],
                "deny": []
              }
            }

            === 不同AI平台配置示例 ===

            1. 智普AI:
            "BASE_URL": "https://open.bigmodel.cn/api/paas/v4/chat/completions"
            "MODEL": "glm-4.5-air"

            2. OpenAI:
            "BASE_URL": "https://api.openai.com/v1/chat/completions"
            "MODEL": "gpt-3.5-turbo"

            3. 百度文心一言:
            "BASE_URL": "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions"
            "MODEL": "ernie-bot-turbo"

            4. 阿里云通义千问:
            "BASE_URL": "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"
            "MODEL": "qwen-turbo"
            """;
    }

    /**
     * 保存当前配置为文件
     */
    public static boolean saveCurrentConfig() {
        boolean success = AIConfigManager.saveCurrentConfig();
        if (success) {
            System.out.println("✅ 已保存当前配置为: setting.json");
            System.out.println("📁 文件位置: " + JsonConfigParser.getConfigFilePath());
        } else {
            System.out.println("❌ 保存配置文件失败");
        }
        return success;
    }

    /**
     * 检查配置完整性并提供建议
     */
    public static String checkConfigHealth() {
        StringBuilder report = new StringBuilder();
        report.append("=== 配置健康检查 ===\n");

        // 检查配置文件
        boolean hasConfig = JsonConfigParser.externalConfigExists();
        report.append("配置文件: ").append(hasConfig ? "✅ 存在" : "❌ 不存在").append("\n");

        if (hasConfig) {
            report.append("配置文件路径: ").append(JsonConfigParser.getConfigFilePath()).append("\n");
        }

        // 检查配置完整性
        boolean isComplete = AIConfigManager.isConfigComplete();
        report.append("配置完整性: ").append(isComplete ? "✅ 完整" : "❌ 不完整").append("\n");

        if (!isComplete) {
            report.append("\n=== 配置问题 ===\n");
            if (!AIConfigManager.isApiKeyConfigured()) {
                report.append("❌ API密钥未配置或为默认值\n");
            }

            String url = AIConfigManager.getApiUrl();
            if (url == null || url.trim().isEmpty()) {
                report.append("❌ API URL未配置\n");
            }

            String model = AIConfigManager.getCurrentModel();
            if (model == null || model.trim().isEmpty()) {
                report.append("❌ 模型名称未配置\n");
            }
        }

        // 提供建议
        if (!isComplete || !hasConfig) {
            report.append("\n=== 建议 ===\n");
            if (!hasConfig) {
                report.append("💡 创建配置文件: ConfigHelper.createConfig()\n");
            }
            if (!isComplete) {
                report.append("💡 编辑配置文件，填入正确的API信息\n");
                report.append("💡 参考配置示例: ConfigHelper.showConfigExample()\n");
            }
        } else {
            report.append("\n🎉 配置状态良好，可以正常使用AI功能！\n");
        }

        return report.toString();
    }
}