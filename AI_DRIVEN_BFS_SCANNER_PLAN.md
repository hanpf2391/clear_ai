# 基于AI批量决策的广度优先文件扫描方案

## 1. 项目背景与目标

基于现有的ReAct架构清理助手，集成智能文件系统扫描功能，解决传统全量扫描效率低下、AI调用频繁的问题。

**核心目标：**
- 🔍 智能剪枝：AI根据语义判断是否深入扫描目录
- ⚡ 高性能：批量决策减少AI调用次数
- 🛡️ 安全防护：白名单保护系统重要文件
- 💰 成本控制：自动下钻浅层目录，节省Token

## 2. 核心设计理念

### 2.1 广度优先遍历 + AI决策
```
Level 0: C:\                     (自动下钻，不问AI)
Level 1: C:\Windows, C:\Users, ... (自动下钻，不问AI)
Level 2: 各种子目录              (开始AI决策)
Level 3+: 深层目录               (完全AI驱动)
```

### 2.2 批量决策策略
- **传统方式**: 每个目录调用1次AI → 扫描1000个目录需要1000次调用
- **批量方式**: 每层调用1次AI → 扫描1000个目录可能只需要10次调用

## 3. 技术架构设计

### 3.1 核心配置常量
```java
public class ScannerConfig {
    // 前2层自动下钻，不消耗AI Token
    public static final int AUTO_DRILL_DEPTH = 2;

    // 单次AI调用最多处理20个目录
    public static final int MAX_AI_BATCH_SIZE = 20;

    // 白名单：系统重要目录，直接跳过
    public static final Set<String> WHITELIST_DIRS = Set.of(
        "Windows", "Program Files", "Program Files (x86)",
        "$RECYCLE.BIN", "System Volume Information",
        ".git", ".idea", "node_modules", "Python"
    );
}
```

### 3.2 数据结构设计

#### ScanTask - 扫描任务
```java
public class ScanTask {
    private final Path path;        // 目录路径
    private final int depth;        // 当前深度
    private final String parentPath; // 父目录路径

    // 构造函数、getter方法...
}
```

#### FolderSummary - AI决策摘要
```java
public class FolderSummary {
    private final String path;           // 路径ID
    private final String name;           // 目录名
    private final long size;             // 目录大小
    private final List<String> hints;    // 内容提示(文件扩展名)

    // 构造函数、getter方法...
}
```

#### ScanResult - 扫描结果
```java
public class ScanResult {
    private final Path path;
    private final ScanResultType type;  // FILE, STOP_FOLDER, CONTINUE_FOLDER
    private final long size;

    public enum ScanResultType {
        FILE,           // 文件(叶子节点)
        STOP_FOLDER,    // 被AI叫停的目录
        CONTINUE_FOLDER // 需要继续扫描的目录
    }
}
```

### 3.3 核心服务类设计

#### AIScannerService - 核心扫描服务
```java
@Service
public class AIScannerService {

    private final ReActAgentExecutor aiExecutor;
    private final ScannerConfig config;

    /**
     * 执行智能扫描
     * @param rootPath 扫描根路径
     * @return 扫描结果统计
     */
    public ScanStatistics performIntelligentScan(Path rootPath) {
        Queue<ScanTask> scanQueue = new LinkedList<>();
        List<ScanResult> finalResults = new ArrayList<>();

        // 初始化：根目录入队
        scanQueue.offer(new ScanTask(rootPath, 0, ""));

        int currentLevel = 0;

        // 广度优先主循环
        while (!scanQueue.isEmpty()) {
            // 取出当前层的所有任务
            List<ScanTask> currentLevelTasks = extractCurrentLevelTasks(scanQueue, currentLevel);

            // 分流处理
            processTasks(currentLevelTasks, scanQueue, finalResults, currentLevel);

            currentLevel++;
        }

        return generateStatistics(finalResults);
    }

    /**
     * 分流处理任务
     */
    private void processTasks(List<ScanTask> tasks, Queue<ScanTask> nextQueue,
                              List<ScanResult> results, int currentLevel) {

        // 分流：自动下钻组 vs AI决策组
        List<ScanTask> autoDrillTasks = new ArrayList<>();
        List<ScanTask> aiDecisionTasks = new ArrayList<>();

        for (ScanTask task : tasks) {
            if (task.getDepth() < ScannerConfig.AUTO_DRILL_DEPTH) {
                autoDrillTasks.add(task);
            } else {
                aiDecisionTasks.add(task);
            }
        }

        // 处理自动下钻组
        processAutoDrillTasks(autoDrillTasks, nextQueue, results);

        // 处理AI决策组
        processAIDecisionTasks(aiDecisionTasks, nextQueue, results);
    }
}
```

### 3.4 与现有ReAct系统集成

#### 新增ReAct工具
```java
@ReActTool(
    name = "intelligent_scan_directory",
    description = "智能扫描目录，使用AI决策优化扫描效率",
    category = "scanning"
)
public String intelligentScanDirectory(
    @ToolParam(name = "path", description = "要扫描的根目录路径", required = true) String path,
    @ToolParam(name = "max_depth", description = "最大扫描深度(可选)", required = false) Integer maxDepth
) {
    try {
        Path scanPath = Paths.get(path);
        AIScannerService scanner = new AIScannerService(aiExecutor);

        ScanStatistics stats = scanner.performIntelligentScan(scanPath);

        // 使用通信工具汇报进度
        return formatScanStatistics(stats);

    } catch (Exception e) {
        ClearAILogger.error("智能扫描失败", e);
        return "扫描失败: " + e.getMessage();
    }
}
```

## 4. AI交互设计

### 4.1 决策Prompt模板
```
你是一个文件系统扫描策略师。下面是一个目录列表，请根据目录名称和内容特征判断是否需要继续深入扫描。

返回格式：JSON数组，与输入顺序对应，每个元素为"CONTINUE"或"STOP"

判断标准：
- CONTINUE: 目录名称模糊或可能包含垃圾文件(如"temp", "cache", "backup", "download")
- STOP: 明确的系统目录或项目目录(如"Windows", "Program Files", ".git")

目录信息：
${folderSummaries}

请返回决策数组：
```

### 4.2 批量决策处理
```java
private Map<String, String> queryAIBatchDecisions(List<FolderSummary> folders) {
    // 分片处理
    Map<String, String> allDecisions = new HashMap<>();

    for (int i = 0; i < folders.size(); i += ScannerConfig.MAX_AI_BATCH_SIZE) {
        List<FolderSummary> batch = folders.subList(i,
            Math.min(i + ScannerConfig.MAX_AI_BATCH_SIZE, folders.size()));

        // 构造Prompt
        String prompt = buildDecisionPrompt(batch);

        // 调用AI
        String response = aiExecutor.processInput(prompt);

        // 解析响应
        Map<String, String> batchDecisions = parseAIDecisions(response, batch);
        allDecisions.putAll(batchDecisions);
    }

    return allDecisions;
}
```

## 5. 性能优化策略

### 5.1 白名单拦截（源头过滤）
```java
private boolean shouldSkipDirectory(Path dirPath) {
    String dirName = dirPath.getFileName().toString();
    return ScannerConfig.WHITELIST_DIRS.contains(dirName);
}
```

### 5.2 智能内容提示
```java
private List<String> extractContentHints(Path dirPath) {
    List<String> hints = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
        int count = 0;
        for (Path entry : stream) {
            if (count >= 5) break; // 只看前5个文件

            if (Files.isRegularFile(entry)) {
                String fileName = entry.getFileName().toString();
                String extension = fileName.contains(".") ?
                    fileName.substring(fileName.lastIndexOf('.')) : "";
                hints.add(extension);
                count++;
            }
        }
    } catch (IOException e) {
        // 忽略错误
    }
    return hints;
}
```

### 5.3 内存控制
```java
// 使用流式处理，避免全量加载
// 及时清理不需要的对象引用
// 控制单次处理的目录数量
```

## 6. 集成到现有项目

### 6.1 项目结构调整
```
src/main/java/com/hanpf/clearai/
├── scanning/                    # 新增包
│   ├── AIScannerService.java    # 核心扫描服务
│   ├── ScanTask.java           # 扫描任务
│   ├── FolderSummary.java      # AI决策摘要
│   ├── ScanResult.java        # 扫描结果
│   └── ScannerConfig.java      # 配置常量
├── react/tools/builtin/        # 现有包
│   └── ScanningTools.java      # 新增ReAct工具
└── ...
```

### 6.2 依赖配置（pom.xml）
```xml
<!-- 已有依赖保持不变 -->
<!-- 可能需要添加的依赖 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
```

## 7. 使用示例

### 7.1 基本扫描
```
用户: 智能扫描 C:\Users\用户名\Downloads 目录

AI响应:
📢 开始智能扫描 Downloads 目录...
⏳ 正在分析目录结构...
📊 扫描完成：发现 150 个文件，发现 3 个可能的垃圾目录(2.3 GB)
⚠️ 建议清理：temp_files, cache, backup_old
```

### 7.2 与清理工具集成
```
用户: 清理 Downloads 目录中的垃圾文件

AI决策过程：
1. 调用 intelligent_scan_directory 扫描
2. AI决策发现垃圾目录
3. 调用 clean_temp_files 清理
4. 汇报清理结果
```

## 8. 监控与日志

### 8.1 关键指标
- AI调用次数 vs 传统扫描次数对比
- 扫描效率提升百分比
- Token使用量统计
- 误判率统计

### 8.2 日志输出
```java
ClearAILogger.info("智能扫描开始: {}", rootPath);
ClearAILogger.info("当前层级: {}, 目录数量: {}", currentLevel, folders.size());
ClearAILogger.info("AI决策结果: CONTINUE={}, STOP={}", continueCount, stopCount);
ClearAILogger.info("扫描完成，总耗时: {}ms", duration);
```

## 9. 下一步实现计划

1. **第一阶段**：核心扫描框架搭建
   - 实现基本BFS扫描逻辑
   - 添加自动下钻功能
   - 集成白名单过滤

2. **第二阶段**：AI决策集成
   - 实现批量AI调用
   - 完善Prompt设计
   - 添加ReAct工具接口

3. **第三阶段**：性能优化
   - 添加分片处理
   - 内存使用优化
   - 异常处理完善

4. **第四阶段**：测试与调优
   - 单元测试覆盖
   - 性能基准测试
   - 用户体验优化

## 10. 风险评估与应对

### 10.1 技术风险
- **风险**：AI决策质量不稳定
- **应对**：添加置信度阈值，人工确认关键决策

### 10.2 性能风险
- **风险**：大目录扫描内存溢出
- **应对**：流式处理，控制单次处理量

### 10.3 成本风险
- **风险**：AI调用成本过高
- **应对**：自动下钻+批量决策大幅降低调用次数

---

这个方案完美结合了您现有的ReAct架构，通过智能剪枝大幅提升扫描效率，同时保持了AI决策的灵活性。建议按阶段逐步实现，确保每个阶段都充分测试。