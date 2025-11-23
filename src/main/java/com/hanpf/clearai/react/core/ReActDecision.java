package com.hanpf.clearai.react.core;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * ReAct决策数据结构
 * 表示AI在每次循环中的决策结果
 */
public class ReActDecision {

    private String thought;        // AI的思考过程
    private ReActAction action;    // 要执行的工具调用
    private String finalAnswer;    // 最终答案

    public ReActDecision() {}

    // 判断是否是最终答案
    public boolean isFinalAnswer() {
        return finalAnswer != null && !finalAnswer.trim().isEmpty();
    }

    // 判断是否有工具调用
    public boolean hasAction() {
        return action != null && action.getToolName() != null;
    }

    // Getters and Setters
    public String getThought() {
        return thought;
    }

    public void setThought(String thought) {
        this.thought = thought;
    }

    public ReActAction getAction() {
        return action;
    }

    public void setAction(ReActAction action) {
        this.action = action;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public void setFinalAnswer(String finalAnswer) {
        this.finalAnswer = finalAnswer;
    }

    @Override
    public String toString() {
        if (isFinalAnswer()) {
            return "FinalAnswer: " + finalAnswer;
        } else if (hasAction()) {
            return "Action: " + action.getToolName();
        } else {
            return "Invalid Decision";
        }
    }
}