package com.hanpf.clearai.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JSON配置文件解析器
 * 负责读取和写入外置的JSON配置文件
 */
public class JsonConfigParser {

    private static final String EXTERNAL_CONFIG_FILE = "setting.json";
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
            .disableHtmlEscaping()
            .create();

    /**
     * 从外部配置文件加载配置
     * @return 配置数据，如果文件不存在或解析失败则返回null
     */
    public static AIConfigData loadExternalConfig() {
        Path configPath = Paths.get(EXTERNAL_CONFIG_FILE);

        if (!Files.exists(configPath)) {
            return null;
        }

        try (Reader reader = new FileReader(EXTERNAL_CONFIG_FILE)) {
            AIConfigData config = gson.fromJson(reader, AIConfigData.class);
            if (config == null) {
                return createDefaultConfig();
            }

            // 确保env配置存在
            if (config.getEnv() == null) {
                config.setEnv(new AIConfigData.AIEnvConfig());
            }

            // 确保permissions配置存在
            if (config.getPermissions() == null) {
                config.setPermissions(new AIConfigData.PermissionsConfig());
            }

            return config;
        } catch (IOException e) {
            System.err.println("读取外部配置文件失败: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("解析外部配置文件失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 保存配置到外部配置文件
     * @param config 要保存的配置数据
     * @return 是否保存成功
     */
    public static boolean saveExternalConfig(AIConfigData config) {
        try (FileWriter writer = new FileWriter(EXTERNAL_CONFIG_FILE)) {
            gson.toJson(config, writer);
            return true;
        } catch (IOException e) {
            System.err.println("保存外部配置文件失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 创建默认的外部配置文件
     * @return 是否创建成功
     */
    public static boolean createExternalConfigTemplate() {
        AIConfigData config = createDefaultConfig();
        return saveExternalConfig(config);
    }

    /**
     * 创建默认配置数据
     * @return 默认配置数据
     */
    private static AIConfigData createDefaultConfig() {
        AIConfigData config = new AIConfigData();

        // 设置环境配置
        AIConfigData.AIEnvConfig envConfig = new AIConfigData.AIEnvConfig();
        envConfig.setANTHROPIC_AUTH_TOKEN("your-auth-token-here");
        envConfig.setANTHROPIC_BASE_URL("https://open.bigmodel.cn/api/anthropic");
        envConfig.setCLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC(1);
        envConfig.setAPI_TIMEOUT_MS(60000);
        envConfig.setANTHROPIC_MODEL("glm-4.6");
        envConfig.setANTHROPIC_SMALL_FAST_MODEL("glm-4.6");
        envConfig.setPROVIDER_NAME("智普AI");
        envConfig.setTEMPERATURE(0.7);
        envConfig.setMAX_TOKENS(1000);

        config.setEnv(envConfig);

        // 设置权限配置
        AIConfigData.PermissionsConfig permissions = new AIConfigData.PermissionsConfig();
        permissions.setAllow(new String[]{});
        permissions.setDeny(new String[]{});

        config.setPermissions(permissions);

        return config;
    }

    /**
     * 检查外部配置文件是否存在
     * @return 是否存在
     */
    public static boolean externalConfigExists() {
        return Files.exists(Paths.get(EXTERNAL_CONFIG_FILE));
    }

    /**
     * 获取配置文件路径
     * @return 配置文件的绝对路径
     */
    public static String getConfigFilePath() {
        return Paths.get(EXTERNAL_CONFIG_FILE).toAbsolutePath().toString();
    }
}