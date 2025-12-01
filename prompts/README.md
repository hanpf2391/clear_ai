# AI提示词管理目录

本目录存放CLEAR AI项目的所有提示词文件，方便管理和维护。

## 文件说明

### 主要提示词
- `main-system-prompt.md` - 主要系统提示词，包含ReAct工作模式和所有核心规则
- `basic-chat-prompt.md` - 基础聊天提示词，用于简单的AI对话场景
- `user-interaction-prompt.md` - 用户交互提示词，用于非ReAct模式的对话
- `welcome-prompt.md` - 欢迎页提示词，用于友好地回应用户的清理需求
- `path-analysis-prompt.md` - 路径分析提示词，用于识别和解析用户输入的路径信息
- `chat-session-prompt.md` - 聊天会话提示词，用于上下文相关的文件清理对话
- `error-handling-prompt.md` - 错误处理提示词，用于编码错误等异常情况

## 对应的Java类位置

- `main-system-prompt.md` → `DynamicPromptBuilder.buildSystemPrompt()`
- `basic-chat-prompt.md` → `AIConfig.createChatService()`
- `user-interaction-prompt.md` → `DynamicPromptBuilder.buildWelcomePrompt()` (备用)
- `welcome-prompt.md` → `DynamicPromptBuilder.buildWelcomePrompt()`
- `path-analysis-prompt.md` → `ReActPathAgent.buildAnalysisPrompt()`
- `chat-session-prompt.md` → `ChatSessionManager.buildPrompt()`
- `error-handling-prompt.md` → `ReActAgentExecutor.parseDecision()` (编码错误)

## 使用方法

1. 提示词文件通过相应的类动态加载
2. 支持热更新，修改文件后重启程序即可生效
3. 文件路径相对于项目根目录
4. 部分提示词仍然硬编码在Java类中，可以逐步迁移到外部文件

## 维护规则

1. 保持提示词简洁明了
2. 使用Markdown格式，方便查看和编辑
3. 重要规则使用数字编号，便于修改
4. 特殊指令使用【】标记，醒目识别
5. JSON格式要求保持一致性

## 最近更新

- 添加了数字输入理解规则（第13条）
- 优化了目录分析优先级
- 增强了任务完成检测逻辑
- 发现并提取了散布在各个Java类中的提示词
- 添加了路径分析和聊天会话专用提示词