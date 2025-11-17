package com.hanpf.clearai;

import com.hanpf.clearai.config.AIConfig;
import com.hanpf.clearai.service.ChatService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class clearAi extends Application {

    private ListView<Node> chatHistory;
    private TextField messageInput;
    private TextField apiKeyInput;
    private Button sendButton;
    private Button setApiKeyButton;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private ChatService chatService;
    private String sessionId;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("智能清理助手");

        // 初始化会话ID和聊天服务
        sessionId = UUID.randomUUID().toString();
        chatService = AIConfig.createChatService();

        // --- 1. 创建UI组件 ---
        chatHistory = new ListView<>();
        chatHistory.getStyleClass().add("chat-list-view");

        messageInput = new TextField();
        messageInput.setPromptText("在这里输入消息...");
        messageInput.setDisable(true); // 初始时禁用，直到设置API密钥

        apiKeyInput = new TextField();
        apiKeyInput.setPromptText("请输入OpenAI API密钥...");

        sendButton = new Button("发送");
        sendButton.setDisable(true); // 初始时禁用

        setApiKeyButton = new Button("设置API密钥");

        // --- 2. 设计布局 ---
        // API密钥输入区域
        HBox apiKeyArea = new HBox(10, apiKeyInput, setApiKeyButton);
        apiKeyArea.setPadding(new Insets(10));
        apiKeyArea.setAlignment(Pos.CENTER);
        HBox.setHgrow(apiKeyInput, Priority.ALWAYS);

        // 输入区域的布局 (水平盒子)
        HBox inputArea = new HBox(10, messageInput, sendButton);
        inputArea.setPadding(new Insets(10));
        inputArea.setAlignment(Pos.CENTER);
        HBox.setHgrow(messageInput, Priority.ALWAYS); // 让输入框占据多余空间

        // 主布局 (垂直盒子)
        VBox root = new VBox(5, apiKeyArea, chatHistory, inputArea);
        root.setPadding(new Insets(10));
        VBox.setVgrow(chatHistory, Priority.ALWAYS); // 让聊天记录区域占据多余空间

        // --- 3. 绑定事件 ---
        // 设置API密钥的动作
        setApiKeyButton.setOnAction(event -> {
            String apiKey = apiKeyInput.getText();
            if (!apiKey.trim().isEmpty()) {
                AIConfig.setApiKey(apiKey);
                chatService = AIConfig.createChatService(); // 重新创建服务实例
                messageInput.setDisable(false);
                sendButton.setDisable(false);
                apiKeyInput.setDisable(true);
                setApiKeyButton.setDisable(true);
                displayMessage("API密钥已设置成功！现在可以开始聊天了。", false);
                messageInput.requestFocus();
            } else {
                displayMessage("请输入有效的API密钥！", false);
            }
        });

        // 定义发送消息的动作
        Runnable sendAction = () -> {
            String message = messageInput.getText();
            if (!message.trim().isEmpty() && chatService != null) {
                displayMessage(message, true); // true = isUser
                messageInput.clear();
                // 调用真实的AI服务
                callAiService(message);
            }
        };

        sendButton.setOnAction(event -> sendAction.run());
        messageInput.setOnAction(event -> sendAction.run()); // 按回车键发送

        // --- 4. 创建并显示场景 ---
        Scene scene = new Scene(root, 600, 800);
        // 加载CSS样式文件
        try {
            String cssPath = getClass().getResource("styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.out.println("CSS文件未找到，使用默认样式");
        }
        primaryStage.setScene(scene);
        primaryStage.show();

        // 添加初始欢迎消息
        displayMessage("你好！我是您的智能清理助手。请先输入OpenAI API密钥开始对话。", false);
    }

    /**
     * 在聊天记录中显示一条消息
     * @param text 消息内容
     * @param isUser 是否是用户发送的
     */
    private void displayMessage(String text, boolean isUser) {
        Label messageLabel = new Label(text);
        messageLabel.setWrapText(true); // 自动换行

        // 使用HBox来控制气泡的对齐
        HBox messageContainer = new HBox(messageLabel);
        if (isUser) {
            messageLabel.getStyleClass().add("chat-message-user");
            messageContainer.setAlignment(Pos.CENTER_RIGHT);
        } else {
            messageLabel.getStyleClass().add("chat-message-ai");
            messageContainer.setAlignment(Pos.CENTER_LEFT);
        }
        messageContainer.setPadding(new Insets(5));

        // 【关键】所有对UI的修改都必须在JavaFX应用线程中进行
        Platform.runLater(() -> {
            chatHistory.getItems().add(messageContainer);
            chatHistory.scrollTo(chatHistory.getItems().size() - 1); // 自动滚动到底部
        });
    }

    /**
     * 调用真实的AI服务
     * @param userInput 用户的输入
     */
    private void callAiService(String userInput) {
        sendButton.setDisable(true);
        messageInput.setDisable(true);

        // 将耗时任务放到后台线程执行，避免冻结UI
        executor.submit(() -> {
            try {
                // 先显示一个空的AI消息气泡
                Platform.runLater(() -> displayMessage("", false));

                // 调用AI服务
                String aiResponse = chatService.chatWithMemory(sessionId, userInput);

                // 模拟打字机效果显示AI回复
                typewriterEffect(aiResponse);

            } catch (Exception e) {
                // 处理错误
                String errorMessage = "抱歉，发生了错误：" + e.getMessage();
                Platform.runLater(() -> {
                    int lastIndex = chatHistory.getItems().size() - 1;
                    if (lastIndex >= 0) {
                        HBox container = (HBox) chatHistory.getItems().get(lastIndex);
                        Label label = (Label) container.getChildren().get(0);
                        label.setText(errorMessage);
                    }
                });
            } finally {
                // 任务结束后，在UI线程中重新启用输入控件
                Platform.runLater(() -> {
                    sendButton.setDisable(false);
                    messageInput.setDisable(false);
                    messageInput.requestFocus(); // 光标回到输入框
                });
            }
        });
    }

    /**
     * 打字机效果显示文本
     * @param text 要显示的文本
     */
    private void typewriterEffect(String text) {
        StringBuilder currentMessage = new StringBuilder();

        // 模拟打字机效果
        for (char c : text.toCharArray()) {
            currentMessage.append(c);
            // 实时更新UI
            Platform.runLater(() -> {
                int lastIndex = chatHistory.getItems().size() - 1;
                if (lastIndex >= 0) {
                    HBox container = (HBox) chatHistory.getItems().get(lastIndex);
                    Label label = (Label) container.getChildren().get(0);
                    label.setText(currentMessage.toString());
                }
            });

            try {
                Thread.sleep(30); // 控制打字速度
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void stop() {
        // 关闭应用时，优雅地关闭线程池
        executor.shutdownNow();
    }
}