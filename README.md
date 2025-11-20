# 智能清理助手

现代化的智能系统清理工具，支持图形界面和命令行两种使用方式。

## 🎯 核心特性

- **🧹 智能清理**: 自动识别和清理系统垃圾文件
- **📊 系统分析**: 提供详细的系统使用情况分析
- **🤖 AI驱动**: 基于AI的智能清理建议
- **💻 命令行支持**: 现代化交互式CLI界面
- **🔧 灵活配置**: 支持多种配置选项

## 📁 项目结构

```
clearAi/
├── src/main/java/com/hanpf/clearai/
│   ├── cli/                           # 命令行界面
│   │   ├── WorkingCLI.java           # 主CLI类 (功能完整)
│   │   ├── CleanerCLI.java           # 简单CLI类 (备用)
│   │   ├── Command.java               # 命令接口
│   │   ├── CommandRegistry.java      # 命令注册表
│   │   ├── OutputFormatter.java      # 输出格式化器
│   │   └── commands/                  # 命令实现
│   │       ├── HelpCommand.java
│   │       ├── VersionCommand.java
│   │       ├── ScanCommand.java
│   │       ├── CleanCommand.java
│   │       ├── InfoCommand.java
│   │       ├── AICommand.java
│   │       └── ConfigCommand.java
│   ├── config/                        # 配置管理
│   │   ├── AIConfig.java
│   │   ├── AIConfigData.java
│   │   ├── AIConfigManager.java
│   │   └── JsonConfigParser.java
│   └── tools/                         # 工具类
│       └── ConfigHelper.java
├── run-working-cli.bat               # 开发启动脚本
├── install-cleaner.bat               # Windows安装脚本
├── install-cleaner.sh                # Linux/Mac安装脚本
├── PROJECT-STRUCTURE.md              # 项目结构说明
├── pom.xml                            # Maven配置
└── README.md                          # 本文档
```

## 🚀 快速开始

### 环境要求

- Java 17 或更高版本
- Maven 3.6 或更高版本

### 开发环境运行

```bash
# 方式1: 使用启动脚本 (推荐)
run-working-cli.bat

# 方式2: 使用Maven
mvn exec:java -Dexec.mainClass="com.hanpf.clearai.cli.WorkingCLI"
```

### 生产环境部署

```bash
# 1. 打包
mvn clean package -DskipTests

# 2. 运行
java -jar target/cleaner-1.0-SNAPSHOT-fat.jar
```

### 用户安装

```bash
# Windows用户 (右键管理员运行)
install-cleaner.bat

# Linux/Mac用户
./install-cleaner.sh

# 安装后可直接使用
cleaner
```

## 💻 CLI功能

### 交互式特性
- ✅ **Tab自动补全**: 智能命令和参数补全
- ✅ **历史记录**: 上下键浏览历史命令
- ✅ **语法高亮**: 实时输入高亮显示
- ✅ **彩色输出**: 美观的彩色界面
- ✅ **错误处理**: 友好的错误提示

### 可用命令

```bash
/help          # 查看帮助信息
/version       # 显示版本信息
/scan          # 扫描系统垃圾文件
/clean         # 清理垃圾文件
/info          # 显示系统信息
/ai            # AI分析和建议
/config        # 配置管理
/exit          # 退出程序
```

## 🎮 使用示例

### CLI交互界面

启动后会看到：

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                    智能清理助手 - 完全可用版本                        ║
║                                                                              ║
║                    🎉 欢迎使用智能清理工具！                           ║
║                                                                              ║
║                    ✨ 功能特性：                                      ║
║    • 命令自动补全    • 历史记录功能    • 友好错误处理                   ║
║    • 语法高亮显示    • 交互式体验      • 跨平台支持                   ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝

🚀 系统就绪！我是您的智能清理助手，有什么可以帮您的吗？
[时间] 智能清理助手 >
```

### 基本使用

```bash
# 查看帮助
/help

# 扫描系统
/scan

# 清理垃圾
/clean

# 查看系统信息
/info

# 退出
/exit
```

## 🔧 配置

应用支持多种配置方式：

- 配置文件: 支持JSON和Properties格式
- 环境变量: 支持环境变量配置
- 命令行参数: 支持命令行参数配置

## 📦 分发指南

### 用户安装步骤

1. **发送项目文件夹给用户**
2. **用户运行安装脚本**:
   - Windows: 右键管理员运行 `install-cleaner.bat`
   - Linux/Mac: 运行 `./install-cleaner.sh`
3. **用户开始使用**:
   - 打开CMD/终端
   - 输入 `cleaner` 启动

### 安装脚本功能

- ✅ 自动检测Java环境
- ✅ 创建系统命令 `cleaner`
- ✅ 添加到PATH环境变量
- ✅ 创建桌面快捷方式
- ✅ 支持卸载功能

## 🛠️ 开发

### 添加新命令

1. 在 `src/main/java/com/hanpf/clearai/cli/commands/` 创建新命令类
2. 实现 `Command` 接口
3. 在 `WorkingCLI.java` 中注册命令

### 测试

```bash
# 编译
mvn clean compile

# 测试CLI
mvn exec:java -Dexec.mainClass="com.hanpf.clearai.cli.WorkingCLI"

# 测试打包
mvn clean package
java -jar target/cleaner-1.0-SNAPSHOT-fat.jar
```

## 🎉 项目特点

- **简洁**: 清理了冗余文件，结构清晰
- **稳定**: 经过充分测试，运行稳定
- **易用**: 一键安装，开箱即用
- **现代**: 支持现代CLI的所有特性
- **跨平台**: Windows/Linux/Mac全支持

---

**智能清理助手 - 让系统清理变得简单高效！** 🚀