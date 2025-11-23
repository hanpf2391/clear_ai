package com.hanpf.clearai.react.agent;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ReAct路径智能代理 - 简化版本
 * 负责分析用户意图，智能识别路径信息和清理需求
 * 暂时使用规则引擎代替AI调用，确保基础功能可用
 */
public class ReActPathAgent {

    private final ObjectMapper objectMapper;

    public ReActPathAgent() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 分析路径需求 - 使用智能规则引擎
     */
    public String analyzePathRequirement(String userInput) {
        // 暂时使用规则引擎进行智能分析
        return generateFallbackResponse(userInput);
    }

    /**
     * 构建用于分析用户意图的提示词
     */
    private String buildAnalysisPrompt(String userInput) {
        return String.format("""
            你是一个智能文件清理助手的路径分析专家。请分析用户的输入，判断是否包含明确的路径信息。

            用户输入: "%s"

            请分析并返回JSON格式的响应：
            {
              "needClarification": true/false,
              "clarificationQuestion": "如果需要澄清，提出友好的问题",
              "detectedPaths": ["识别到的路径数组"],
              "reasoning": "分析过程",
              "suggestions": ["建议的路径或操作"]
            }

            判断规则：
            1. 如果用户提到"算了"、"取消"、"不要了"等，设置needClarification为false，并在clarificationQuestion中回复"好的，已取消操作"
            2. 如果用户提到"C盘"、"D盘"但没有具体路径，提供常见目录建议
            3. 如果用户输入包含完整路径，直接提取路径
            4. 保持对话的自然和友好
            """, userInput);
    }

    /**
     * 生成智能响应（基于规则引擎）
     */
    private String generateFallbackResponse(String userInput) {
        String lowerInput = userInput.toLowerCase();

        // 检测取消意图
        if (lowerInput.contains("算了") || lowerInput.contains("取消") || lowerInput.contains("不要了") ||
            lowerInput.contains("不用了") || lowerInput.contains("没事了") || lowerInput.contains("算了吧")) {
            return """
                {
                  "needClarification": false,
                  "clarificationQuestion": "好的，已取消操作",
                  "detectedPaths": [],
                  "reasoning": "用户表示取消操作",
                  "suggestions": []
                }
                """;
        }

        // 检测C盘清理意图
        if (lowerInput.contains("c盘") || lowerInput.contains("c:") || lowerInput.contains("系统盘")) {
            return """
                {
                  "needClarification": true,
                  "clarificationQuestion": "我来帮您清理C盘。您想清理哪个具体目录呢？比如：下载文件夹、临时文件、回收站等？",
                  "detectedPaths": ["C:\\"],
                  "reasoning": "用户想要清理C盘，但需要更具体的目录信息",
                  "suggestions": ["下载文件夹", "临时文件", "回收站", "系统缓存"]
                }
                """;
        }

        // 检测下载文件夹清理意图
        if (lowerInput.contains("下载") || lowerInput.contains("downloads")) {
            String downloadsPath = System.getProperty("user.home") + "\\Downloads";
            return String.format("""
                {
                  "needClarification": false,
                  "clarificationQuestion": "我找到了您的下载文件夹，准备开始扫描分析。",
                  "detectedPaths": ["%s"],
                  "reasoning": "用户想要清理下载文件夹，已自动识别路径",
                  "suggestions": []
                }
                """, downloadsPath.replace("\\", "\\\\"));
        }

        // 检测桌面清理意图
        if (lowerInput.contains("桌面") || lowerInput.contains("desktop")) {
            String desktopPath = System.getProperty("user.home") + "\\Desktop";
            return String.format("""
                {
                  "needClarification": false,
                  "clarificationQuestion": "我找到了您的桌面文件夹，准备开始扫描分析。",
                  "detectedPaths": ["%s"],
                  "reasoning": "用户想要清理桌面，已自动识别路径",
                  "suggestions": []
                }
                """, desktopPath.replace("\\", "\\\\"));
        }

        // 检测临时文件清理意图
        if (lowerInput.contains("临时") || lowerInput.contains("temp") || lowerInput.contains("垃圾")) {
            String tempPath = System.getProperty("java.io.tmpdir");
            return String.format("""
                {
                  "needClarification": false,
                  "clarificationQuestion": "我找到了临时文件目录，准备开始扫描分析。",
                  "detectedPaths": ["%s"],
                  "reasoning": "用户想要清理临时文件，已自动识别路径",
                  "suggestions": []
                }
                """, tempPath.replace("\\", "\\\\"));
        }

        // 检测直接路径输入
        if (userInput.matches(".*[A-Za-z]:\\\\.*") || userInput.contains("@")) {
            String path = userInput.replace("@", "").trim();
            return String.format("""
                {
                  "needClarification": false,
                  "clarificationQuestion": "已识别您指定的路径，准备开始扫描分析。",
                  "detectedPaths": ["%s"],
                  "reasoning": "用户提供了明确的路径信息",
                  "suggestions": []
                }
                """, path.replace("\\", "\\\\"));
        }

        // 检测用户想要帮助或建议
        if (lowerInput.contains("建议") || lowerInput.contains("怎么") || lowerInput.contains("什么") ||
            lowerInput.contains("帮我") || lowerInput.contains("不知道")) {
            return """
                {
                  "needClarification": true,
                  "clarificationQuestion": "我来帮您分析一下。通常这些位置容易积累垃圾文件：下载文件夹、临时文件、浏览器缓存等。您想从哪个开始呢？",
                  "detectedPaths": [],
                  "reasoning": "用户需要清理建议，提供常见清理选项",
                  "suggestions": ["下载文件夹", "临时文件", "浏览器缓存", "系统日志", "回收站"]
                }
                """;
        }

        // 默认友好响应
        return """
            {
              "needClarification": true,
              "clarificationQuestion": "我来帮您清理电脑。请告诉我您想清理哪个目录，比如：下载文件夹、桌面文件，或者直接提供完整路径？",
              "detectedPaths": [],
              "reasoning": "用户的意图不够明确，需要更多引导信息",
              "suggestions": ["下载文件夹", "桌面文件", "临时文件", "C盘清理"]
            }
            """;
    }
}