package com.hanpf.clearai.react.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanpf.clearai.cli.cleaning.react.PathInputParser;

import java.util.List;
import java.util.ArrayList;

/**
 * ReAct分析结果数据模型
 */
public class ReactAnalysisResult {

    @JsonProperty("reasoning")
    private String reasoning;

    @JsonProperty("actions")
    private List<String> actions;

    @JsonProperty("paths")
    private List<String> paths;

    @JsonProperty("strategy")
    private String strategy;

    @JsonProperty("warnings")
    private List<String> warnings;

    @JsonProperty("recommendations")
    private List<String> recommendations;

    @JsonProperty("estimatedTime")
    private String estimatedTime;

    // 默认构造函数
    public ReactAnalysisResult() {
        this.actions = new ArrayList<>();
        this.paths = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.recommendations = new ArrayList<>();
        this.reasoning = "";
        this.strategy = "";
        this.estimatedTime = "未知";
    }

    /**
     * 从JSON字符串解析分析结果
     */
    public static ReactAnalysisResult fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, ReactAnalysisResult.class);
        } catch (Exception e) {
            // 如果解析失败，返回默认结果
            ReactAnalysisResult result = new ReactAnalysisResult();
            result.setReasoning("解析失败，使用默认策略");
            result.setStrategy("基础扫描策略");
            result.addWarning("无法解析AI响应，将使用基础扫描方式");
            return result;
        }
    }

    /**
     * 添加警告
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * 添加建议
     */
    public void addRecommendation(String recommendation) {
        this.recommendations.add(recommendation);
    }

    /**
     * 验证路径有效性
     */
    public void validatePaths() {
        if (paths == null) {
            paths = new ArrayList<>();
            return;
        }

        PathInputParser parser = new PathInputParser();
        List<String> validPaths = new ArrayList<>();

        for (String path : paths) {
            if (parser.isValidPath(path)) {
                validPaths.add(path);
            } else {
                addWarning("路径无效或不可访问: " + path);
            }
        }

        this.paths = validPaths;
    }

    /**
     * 检查是否有重要警告
     */
    public boolean hasImportantWarnings() {
        return warnings.stream().anyMatch(w ->
            w.contains("系统目录") ||
            w.contains("系统稳定性") ||
            w.contains("重要文件")
        );
    }

    /**
     * 获取显示用的路径列表
     */
    public List<String> getDisplayPaths() {
        PathInputParser parser = new PathInputParser();
        List<String> displayPaths = new ArrayList<>();

        for (String path : paths) {
            displayPaths.add(parser.getDisplayName(path, 50));
        }

        return displayPaths;
    }

    /**
     * 生成扫描计划摘要
     */
    public String generateSummary() {
        StringBuilder summary = new StringBuilder();

        summary.append("📋 扫描计划摘要\n");
        summary.append("================\n\n");

        summary.append("📝 分析说明:\n").append(reasoning).append("\n\n");
        summary.append("🎯 扫描策略:\n").append(strategy).append("\n\n");

        if (!paths.isEmpty()) {
            summary.append("📁 将扫描 ").append(paths.size()).append(" 个路径:\n");
            for (int i = 0; i < paths.size(); i++) {
                summary.append("   ").append(i + 1).append(". ").append(paths.get(i)).append("\n");
            }
            summary.append("\n");
        }

        if (!warnings.isEmpty()) {
            summary.append("⚠️ 警告:\n");
            for (String warning : warnings) {
                summary.append("   • ").append(warning).append("\n");
            }
            summary.append("\n");
        }

        if (!recommendations.isEmpty()) {
            summary.append("💡 建议:\n");
            for (String recommendation : recommendations) {
                summary.append("   • ").append(recommendation).append("\n");
            }
            summary.append("\n");
        }

        summary.append("⏱️ 预计时间: ").append(estimatedTime).append("\n");

        return summary.toString();
    }

    // Getters and Setters
    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public String getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(String estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public boolean isEmpty() {
        return (reasoning == null || reasoning.trim().isEmpty()) &&
               (strategy == null || strategy.trim().isEmpty()) &&
               (paths == null || paths.isEmpty());
    }
}