# 基于AI批量决策的广度优先文件扫描方案

## 1. 项目背景与目标

基于现有的ReAct架构清理助手，集成智能文件系统扫描功能，解决传统全量扫描效率低下、AI调用频繁的问题。

**核心目标：**
- 🔍 智能剪枝：AI根据语义判断是否深入扫描目录
- ⚡ 高性能：批量决策减少AI调用次数
- 🛡️ 安全防护：利用现有白名单文件保护系统重要文件
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

    // 白名单文件路径（相对于项目根目录）
    public static final String USER_WHITELIST_FILE = "whitelist.txt";
    public static final String SYSTEM_WHITELIST_FILE = "system_whitelist.txt";

    // 从文件加载的白名单规则
    private static List<String> userWhitelist = new ArrayList<>();
    private static List<String> systemWhitelist = new ArrayList<>();

    /**
     * 初始化时加载白名单文件
     */
    public static void loadWhitelists() {
        userWhitelist = loadWhitelistFile(USER_WHITELIST_FILE);
        systemWhitelist = loadWhitelistFile(SYSTEM_WHITELIST_FILE);
    }

    /**
     * 检查路径是否在白名单中
     */
    public static boolean isWhitelisted(Path path) {
        String normalizedPath = normalizePath(path.toString());
        return matchesWhitelist(normalizedPath, systemWhitelist) ||
               matchesWhitelist(normalizedPath, userWhitelist);
    }

    private static List<String> loadWhitelistFile(String filename) {
        List<String> rules = new ArrayList<>();
        try {
            Path whitelistPath = Paths.get(filename);
            if (Files.exists(whitelistPath)) {
                Files.lines(whitelistPath)
                    .filter(line -> !line.trim().isEmpty() && !line.startsWith("#"))
                    .forEach(rules::add);
            }
        } catch (IOException e) {
            ClearAILogger.warn("无法加载白名单文件: " + filename, e);
        }
        return rules;
    }

    /**
     * 路径标准化处理
     */
    private static String normalizePath(String path) {
        // 处理环境变量替换
        String normalized = path;
        normalized = normalized.replace("%SystemRoot%", System.getenv("SystemRoot"));
        normalized = normalized.replace("%ProgramFiles%", System.getenv("ProgramFiles"));
        normalized = normalized.replace("%ProgramFiles(x86)%", System.getenv("ProgramFiles(x86)"));
        normalized = normalized.replace("%ProgramData%", System.getenv("ProgramData"));
        normalized = normalized.replace("%USERPROFILE%", System.getProperty("user.home"));
        normalized = normalized.replace("%APPDATA%", System.getenv("APPDATA"));
        normalized = normalized.replace("%LOCALAPPDATA%", System.getenv("LOCALAPPDATA"));

        // 路径分隔符标准化
        return normalized.replace("/", "\\").toLowerCase();
    }

    /**
     * 检查路径是否匹配白名单规则
     */
    private static boolean matchesWhitelist(String path, List<String> whitelist) {
        for (String rule : whitelist) {
            if (matchRule(path, rule)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 单个规则匹配（支持通配符）
     */
    private static boolean matchRule(String path, String rule) {
        // 简单的通配符匹配实现
        if (rule.contains("*")) {
            // 将通配符转换为正则表达式
            String regex = rule.replace(".", "\\.")
                            .replace("*", ".*")
                            .replace("?", ".");
            return path.toLowerCase().matches(regex.toLowerCase());
        } else {
            return path.toLowerCase().contains(rule.toLowerCase());
        }
    }
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

### 3.3 白名单管理服务
```java
@Service
public class WhitelistService {

    private final ScannerConfig config;

    /**
     * 初始化白名单
     */
    @PostConstruct
    public void init() {
        ScannerConfig.loadWhitelists();
        ClearAILogger.info("白名单加载完成");
    }

    /**
     * 检查路径是否需要跳过（在白名单中）
     */
    public boolean shouldSkip(Path path) {
        return ScannerConfig.isWhitelisted(path);
    }

    /**
     * 获取白名单统计信息
     */
    public WhitelistStats getStats() {
        int systemRules = ScannerConfig.systemWhitelist.size();
        int userRules = ScannerConfig.userWhitelist.size();
        return new WhitelistStats(systemRules, userRules);
    }
}
```

### 3.4 核心服务类设计

#### AIScannerService - 核心扫描服务
```java
@Service
public class AIScannerService {

    private final ReActAgentExecutor aiExecutor;
    private final WhitelistService whitelistService;

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

    /**
     * 处理自动下钻任务
     */
    private void processAutoDrillTasks(List<ScanTask> tasks, Queue<ScanTask> nextQueue,
                                      List<ScanResult> results) {
        for (ScanTask task : tasks) {
            try {
                scanDirectory(task, nextQueue, results, false); // false表示不需要AI决策
            } catch (IOException e) {
                ClearAILogger.warn("扫描目录失败: " + task.getPath(), e);
            }
        }
    }

    /**
     * 处理AI决策任务
     */
    private void processAIDecisionTasks(List<ScanTask> tasks, Queue<ScanTask> nextQueue,
                                       List<ScanResult> results) {
        if (tasks.isEmpty()) return;

        // 分片处理
        for (int i = 0; i < tasks.size(); i += ScannerConfig.MAX_AI_BATCH_SIZE) {
            int end = Math.min(i + ScannerConfig.MAX_AI_BATCH_SIZE, tasks.size());
            List<ScanTask> batch = tasks.subList(i, end);

            // 准备AI决策摘要
            List<FolderSummary> summaries = batch.stream()
                .map(this::createFolderSummary)
                .collect(Collectors.toList());

            // 调用AI批量决策
            Map<String, String> decisions = queryAIBatchDecisions(summaries);

            // 执行决策
            for (ScanTask task : batch) {
                String decision = decisions.get(task.getPath().toString());
                if ("CONTINUE".equals(decision)) {
                    try {
                        scanDirectory(task, nextQueue, results, true); // true表示需要AI决策
                    } catch (IOException e) {
                        ClearAILogger.warn("扫描目录失败: " + task.getPath(), e);
                    }
                } else {
                    // STOP决策，直接添加到结果
                    results.add(new ScanResult(task.getPath(), ScanResultType.STOP_FOLDER, 0));
                }
            }
        }
    }
}
```

### 3.5 与现有ReAct系统集成

#### 新增ReAct工具
```java
@ReActTool(
    name = "intelligent_scan_directory",
    description = "智能扫描目录，使用AI决策优化扫描效率，支持白名单保护",
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

## 4. 现有白名单文件分析

### 4.1 system_whitelist.txt 内容分析
```
# Windows系统目录保护
- C:\Windows\*
- C:\Program Files\*
- C:\Program Files (x86)\*
- C:\ProgramData\*
- %SystemRoot%\*
- %ProgramFiles%\*
- %ProgramFiles(x86)%\*
- %ProgramData%\*

# 系统重要文件保护
- pagefile.sys
- hiberfil.sys
- swapfile.sys

# 用户配置目录保护
- %USERPROFILE%\AppData\Local\Microsoft\*
- %USERPROFILE%\AppData\Roaming\Microsoft\*
- %APPDATA%\*
- %LOCALAPPDATA%\*
```

### 4.2 whitelist.txt 内容分析
```
# 用户白名单文件
# 支持通配符 * 和 ?
# 支持环境变量 %USERPROFILE%, %APPDATA% 等
# 以 # 开头的行为注释

# 目前文件为空，用户可自行添加需要保护的路径
```

## 5. AI交互设计

### 5.1 决策Prompt模板
```
你是一个文件系统扫描策略师。下面是一个目录列表，请根据目录名称和内容特征判断是否需要继续深入扫描。

返回格式：JSON数组，与输入顺序对应，每个元素为"CONTINUE"或"STOP"

判断标准：
- CONTINUE: 目录名称模糊或可能包含垃圾文件(如"temp", "cache", "backup", "download", "logs")
- STOP: 明确的系统目录、项目目录或用户重要目录

目录信息：
${folderSummaries}

请返回决策数组：
```

### 5.2 批量决策处理
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

## 6. 性能优化策略

### 6.1 白名单拦截（源头过滤）
```java
private boolean shouldSkipDirectory(Path dirPath) {
    return whitelistService.shouldSkip(dirPath);
}
```

### 6.2 智能内容提示
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

### 6.3 内存控制
```java
// 使用流式处理，避免全量加载
// 及时清理不需要的对象引用
// 控制单次处理的目录数量
```

## 7. 集成到现有项目

### 7.1 项目结构调整
```
src/main/java/com/hanpf/clearai/
├── scanning/                         # 新增包
│   ├── AIScannerService.java       # 核心扫描服务
│   ├── WhitelistService.java        # 白名单管理服务
│   ├── ScanTask.java              # 扫描任务
│   ├── FolderSummary.java         # AI决策摘要
│   ├── ScanResult.java           # 扫描结果
│   └── ScannerConfig.java         # 配置常量
├── react/tools/builtin/             # 现有包
│   └── ScanningTools.java          # 新增ReAct工具
└── config/                          # 现有包
    ├── setting.json               # 现有配置文件
    ├── whitelist.txt               # 现有用户白名单
    └── system_whitelist.txt        # 现有系统白名单
```

### 7.2 依赖配置（pom.xml）
```xml
<!-- 已有依赖保持不变 -->
<!-- 可能需要添加的依赖 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
```

## 8. 使用示例

### 8.1 基本扫描
```
用户: 智能扫描 C:\Users\用户名\Downloads 目录

AI响应:
📢 开始智能扫描 Downloads 目录...
⏳ 正在分析目录结构...
📊 扫描完成：发现 150 个文件，发现 3 个可能的垃圾目录(2.3 GB)
⚠️ 建议清理：temp_files, cache, backup_old
```

### 8.2 与清理工具集成
```
用户: 清理 Downloads 目录中的垃圾文件

AI决策过程：
1. 调用 intelligent_scan_directory 扫描
2. AI决策发现垃圾目录
3. 调用 clean_temp_files 清理
4. 汇报清理结果
```

## 9. 监控与日志

### 9.1 关键指标
- AI调用次数 vs 传统扫描次数对比
- 扫描效率提升百分比
- Token使用量统计
- 误判率统计
- 白名单拦截统计

### 9.2 日志输出
```java
ClearAILogger.info("智能扫描开始: {}", rootPath);
ClearAILogger.info("白名单加载完成: 系统规则{}条, 用户规则{}条",
    systemRuleCount, userRuleCount);
ClearAILogger.info("当前层级: {}, 目录数量: {}", currentLevel, folders.size());
ClearAILogger.info("白名单拦截: {}个目录", skippedCount);
ClearAILogger.info("AI决策结果: CONTINUE={}, STOP={}", continueCount, stopCount);
ClearAILogger.info("扫描完成，总耗时: {}ms", duration);
```

## 10. 下一步实现计划

### 10.1 第一阶段：核心扫描框架搭建
- [ ] 实现 WhitelistService 白名单管理服务
- [ ] 实现基本 BFS 扫描逻辑
- [ ] 添加自动下钻功能
- [ ] 集成白名单过滤

### 10.2 第二阶段：AI决策集成
- [ ] 实现 FolderSummary AI 决策摘要
- [ ] 实现批量 AI 调用逻辑
- [ ] 完善 Prompt 设计
- [ ] 添加 ReAct 工具接口

### 10.3 第三阶段：性能优化
- [ ] 添加分片处理逻辑
- [ ] 内存使用优化
- [ ] 异常处理完善
- [ ] 性能监控指标

### 10.4 第四阶段：测试与调优
- [ ] 单元测试覆盖
- [ ] 性能基准测试
- [ ] 白名单规则测试
- [ ] 用户体验优化

## 11. 风险评估与应对

### 11.1 技术风险
- **风险**：AI决策质量不稳定
- **应对**：添加置信度阈值，人工确认关键决策

### 11.2 性能风险
- **风险**：大目录扫描内存溢出
- **应对**：流式处理，控制单次处理量

### 11.3 成本风险
- **风险**：AI调用成本过高
- **应对**：自动下钻+批量决策大幅降低调用次数

### 11.4 安全风险
- **风险**：白名单规则不完善导致误删
- **应对**：利用现有白名单文件，支持用户自定义规则

---

这个方案完美利用了您现有的白名单文件系统，通过智能剪枝大幅提升扫描效率，同时保持了AI决策的灵活性。建议按阶段逐步实现，确保每个阶段都充分测试和验证。