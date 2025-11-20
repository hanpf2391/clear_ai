package com.hanpf.clearai.config;

import java.io.IOException;

/**
 * AI配置管理器 - 简化版本
 * 只支持外置setting.json配置文件
 */
public class AIConfigManager {
    private static AIConfigData externalConfig = null;

    static {
        loadConfig();
    }

    /**
     * 加载配置文件 - 只加载setting.json
     */
    private static void loadConfig() {
        // 只支持外置JSON配置文件 setting.json
        externalConfig = JsonConfigParser.loadExternalConfig();
        if (externalConfig != null && externalConfig.getEnv() != null) {
            System.out.println("已加载配置文件: " + JsonConfigParser.getConfigFilePath());
        } else {
            System.err.println("未找到配置文件 setting.json，程序将无法正常运行");
            System.err.println("请在程序同级目录创建 setting.json 文件并配置AI信息");
        }
    }

    /**
     * 检查配置模式
     */
    public static String getConfigMode() {
        return "JSON配置";
    }

    /**
     * 获取当前API密钥
     */
    public static String getCurrentApiKey() {
        if (externalConfig != null && externalConfig.getEnv() != null) {
            // 优先使用ANTHROPIC_AUTH_TOKEN，否则使用API_KEY
            String anthropicToken = externalConfig.getEnv().getANTHROPIC_AUTH_TOKEN();
            if (anthropicToken != null && !anthropicToken.trim().isEmpty()) {
                return anthropicToken;
            }
            return externalConfig.getEnv().getAPI_KEY();
        }
        return null;
    }

    /**
     * 获取API URL
     */
    public static String getApiUrl() {
        if (externalConfig != null && externalConfig.getEnv() != null) {
            // 优先使用ANTHROPIC_BASE_URL，否则使用BASE_URL
            String anthropicUrl = externalConfig.getEnv().getANTHROPIC_BASE_URL();
            if (anthropicUrl != null && !anthropicUrl.trim().isEmpty()) {
                return anthropicUrl;
            }
            return externalConfig.getEnv().getBASE_URL();
        }
        return null;
    }

    /**
     * 获取当前使用的模型名称
     */
    public static String getCurrentModel() {
        if (externalConfig != null && externalConfig.getEnv() != null) {
            // 优先使用ANTHROPIC_MODEL，否则使用MODEL
            String anthropicModel = externalConfig.getEnv().getANTHROPIC_MODEL();
            if (anthropicModel != null && !anthropicModel.trim().isEmpty()) {
                return anthropicModel;
            }
            return externalConfig.getEnv().getMODEL();
        }
        return null;
    }

    /**
     * 获取温度参数
     */
    public static double getTemperature() {
        if (externalConfig != null && externalConfig.getEnv() != null &&
            externalConfig.getEnv().getTEMPERATURE() != null) {
            return externalConfig.getEnv().getTEMPERATURE();
        }
        return 0.7;
    }

    /**
     * 获取最大tokens
     */
    public static int getMaxTokens() {
        if (externalConfig != null && externalConfig.getEnv() != null &&
            externalConfig.getEnv().getMAX_TOKENS() != null) {
            return externalConfig.getEnv().getMAX_TOKENS();
        }
        return 1000;
    }

    /**
     * 获取请求超时时间（秒）
     */
    public static int getTimeout() {
        if (externalConfig != null && externalConfig.getEnv() != null &&
            externalConfig.getEnv().getAPI_TIMEOUT_MS() != null) {
            return externalConfig.getEnv().getAPI_TIMEOUT_MS() / 1000;
        }
        return 60;
    }

    /**
     * 获取AI提供商名称（用于显示）
     */
    public static String getProviderName() {
        return (externalConfig != null && externalConfig.getEnv() != null)
               ? externalConfig.getEnv().getPROVIDER_NAME() : "未知";
    }

    /**
     * 检查API密钥是否已配置
     */
    public static boolean isApiKeyConfigured() {
        String apiKey = getCurrentApiKey();
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equals("your-api-key-here");
    }

    /**
     * 检查配置是否完整
     */
    public static boolean isConfigComplete() {
        if (!isApiKeyConfigured()) return false;

        String url = getApiUrl();
        String model = getCurrentModel();

        return url != null && !url.trim().isEmpty() &&
               model != null && !model.trim().isEmpty() &&
               !url.contains("your-api-endpoint.com") &&
               !getCurrentApiKey().contains("your-api-key-here");
    }

    /**
     * 创建配置文件模板
     */
    public static boolean createConfigTemplate() {
        return JsonConfigParser.createExternalConfigTemplate();
    }

    /**
     * 保存当前配置到文件
     */
    public static boolean saveCurrentConfig() {
        if (externalConfig == null) {
            externalConfig = new AIConfigData();
            AIConfigData.AIEnvConfig envConfig = new AIConfigData.AIEnvConfig();
            envConfig.setAPI_KEY(getCurrentApiKey());
            envConfig.setBASE_URL(getApiUrl());
            envConfig.setMODEL(getCurrentModel());
            envConfig.setTEMPERATURE(getTemperature());
            envConfig.setMAX_TOKENS(getMaxTokens());
            envConfig.setAPI_TIMEOUT_MS(getTimeout() * 1000);
            envConfig.setPROVIDER_NAME(getProviderName());
            externalConfig.setEnv(envConfig);
            externalConfig.setPermissions(new AIConfigData.PermissionsConfig());
        }

        return JsonConfigParser.saveExternalConfig(externalConfig);
    }

    /**
     * 重新加载配置
     */
    public static void reloadConfig() {
        externalConfig = null;
        loadConfig();
    }

    /**
     * 获取配置文件状态信息
     */
    public static String getConfigStatus() {
        StringBuilder info = new StringBuilder();
        info.append("=== 配置状态 ===\n");
        info.append("配置模式: ").append(getConfigMode()).append("\n");
        info.append("AI提供商: ").append(getProviderName()).append("\n");
        info.append("API URL: ").append(getApiUrl()).append("\n");
        info.append("模型: ").append(getCurrentModel()).append("\n");
        info.append("API密钥状态: ").append(isApiKeyConfigured() ? "已配置" : "未配置").append("\n");
        info.append("配置完整性: ").append(isConfigComplete() ? "完整" : "不完整").append("\n");
        info.append("配置文件路径: ").append(JsonConfigParser.getConfigFilePath()).append("\n");

        return info.toString();
    }
}