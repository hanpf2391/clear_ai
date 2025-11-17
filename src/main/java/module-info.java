module com.hanpf.clearai {
    // 1. 声明我们的应用需要 JavaFX 的哪些模块
    requires javafx.controls;
    requires javafx.fxml;

    // 2. 声明我们的应用需要 LangChain4j 的哪些模块 (如果未来用到)
    // requires langchain4j;
    // requires langchain4j.open.ai;

    // 3. 声明我们要把哪个包暴露给 JavaFX 框架，以便它能启动我们的应用
    opens com.hanpf.clearai to javafx.fxml;
    exports com.hanpf.clearai;
}
