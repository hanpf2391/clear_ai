package com.hanpf.clearai.react.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.ArrayList;

/**
 * 路径分析结果 - 支持增强的AI分析功能
 */
public class PathAnalysisResult {

    @JsonProperty("needClarification")
    private boolean needClarification;

    @JsonProperty("clarificationQuestion")
    private String clarificationQuestion;

    @JsonProperty("analysis")
    private String analysis;

    @JsonProperty("confidence")
    private String confidence;

    @JsonProperty("reasoning")
    private String reasoning;

    @JsonProperty("detectedPaths")
    private List<String> detectedPaths;

    @JsonProperty("suggestions")
    private List<String> suggestions;

    public PathAnalysisResult() {
        this.detectedPaths = new ArrayList<>();
        this.suggestions = new ArrayList<>();
    }

    public static PathAnalysisResult fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, PathAnalysisResult.class);
        } catch (Exception e) {
            // 解析失败时返回默认结果
            PathAnalysisResult result = new PathAnalysisResult();
            result.setNeedClarification(true);
            result.setClarificationQuestion("请问您想清理哪个具体的目录？");
            result.setReasoning("解析失败，需要用户澄清路径");
            return result;
        }
    }

    // Getters and Setters
    public boolean isNeedClarification() {
        return needClarification;
    }

    public void setNeedClarification(boolean needClarification) {
        this.needClarification = needClarification;
    }

    public String getClarificationQuestion() {
        return clarificationQuestion;
    }

    public void setClarificationQuestion(String clarificationQuestion) {
        this.clarificationQuestion = clarificationQuestion;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public List<String> getDetectedPaths() {
        return detectedPaths;
    }

    public void setDetectedPaths(List<String> detectedPaths) {
        this.detectedPaths = detectedPaths != null ? detectedPaths : new ArrayList<>();
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions != null ? suggestions : new ArrayList<>();
    }
}