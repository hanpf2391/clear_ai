#!/bin/bash

# 智能清理助手 - Linux/Mac系统安装器

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_message() {
    echo -e "${GREEN}✅${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}❌${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

echo "==============================================="
echo "      智能清理助手 - Linux/Mac系统安装器"
echo "==============================================="
echo ""
echo "此安装器将："
echo "  ✓ 编译项目并打包成可执行jar"
echo "  ✓ 创建 cleaner 系统命令"
echo "  ✓ 添加到系统PATH环境变量"
echo "  ✓ 支持在任意终端输入 cleaner 启动"
echo ""

# 检查Java环境
print_info "正在检查Java环境..."
if command -v java &> /dev/null; then
    print_message "Java环境正常"
    java -version 2>&1 | head -1
else
    print_error "未找到Java环境，请先安装Java 17或更高版本"
    print_info "Ubuntu/Debian: sudo apt update && sudo apt install openjdk-17-jdk"
    print_info "macOS: brew install openjdk@17"
    print_info "其他: https://adoptium.net/"
    exit 1
fi

# 检查Maven
print_info "正在检查Maven环境..."
if command -v mvn &> /dev/null; then
    print_message "Maven环境正常"
else
    print_error "未找到Maven环境，请先安装Maven"
    print_info "Ubuntu/Debian: sudo apt install maven"
    print_info "macOS: brew install maven"
    exit 1
fi

# 编译和打包
print_info "正在编译和打包项目..."
if mvn clean package -q; then
    print_message "编译成功！"
else
    print_error "编译失败！"
    exit 1
fi

# 检查fat jar文件
JAR_FILE="target/cleaner-1.0-SNAPSHOT-fat.jar"
if [ -f "$JAR_FILE" ]; then
    print_message "找到Fat JAR文件: $JAR_FILE"
else
    print_error "未找到Fat JAR文件"
    print_info "当前文件:"
    ls -la target/*.jar 2>/dev/null || print_info "没有找到jar文件"
    exit 1
fi

# 确定安装目录
if [ "$EUID" -eq 0 ]; then
    # 管理员模式
    INSTALL_DIR="/opt/cleaner"
    BIN_DIR="/usr/local/bin"
    print_message "检测到管理员权限，将进行系统级安装"
else
    # 用户模式
    INSTALL_DIR="$HOME/.local/share/cleaner"
    BIN_DIR="$HOME/.local/bin"
    print_message "将进行用户级安装"
fi

# 创建安装目录
print_info "正在创建安装目录: $INSTALL_DIR"
mkdir -p "$INSTALL_DIR"

# 复制文件
print_info "正在复制文件..."
cp "$JAR_FILE" "$INSTALL_DIR/cleaner.jar"
print_message "复制JAR文件完成"

# 创建启动脚本
print_info "正在创建cleaner启动脚本..."
cat > "$INSTALL_DIR/cleaner" << 'EOF'
#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
java -jar "$SCRIPT_DIR/cleaner.jar" "$@"
EOF

chmod +x "$INSTALL_DIR/cleaner"
print_message "创建启动脚本完成"

# 创建符号链接到PATH
print_info "正在创建系统命令链接..."
if [ ! -L "$BIN_DIR/cleaner" ]; then
    mkdir -p "$BIN_DIR"
    ln -sf "$INSTALL_DIR/cleaner" "$BIN_DIR/cleaner"
    print_message "创建系统命令链接完成"
else
    print_info "系统命令链接已存在"
fi

# 检查PATH
print_info "正在检查PATH环境变量..."
if echo ":$PATH:" | grep -q ":$BIN_DIR:"; then
    print_message "$BIN_DIR 已在PATH中"
else
    print_warning "$BIN_DIR 不在PATH中，请手动添加到你的shell配置文件"
    print_info "添加以下行到 ~/.bashrc 或 ~/.zshrc:"
    print_info "  export PATH=\"$BIN_DIR:\$PATH\""
    print_info "然后运行: source ~/.bashrc (或 source ~/.zshrc)"
fi

# 创建桌面快捷方式 (如果支持)
if command -v desktop-file-install &> /dev/null; then
    print_info "正在创建桌面快捷方式..."
    mkdir -p "$HOME/.local/share/applications"
    cat > "$HOME/.local/share/applications/cleaner.desktop" << EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=智能清理助手
Comment=智能系统清理工具
Exec=$BIN_DIR/cleaner
Icon=system-cleaner
Terminal=true
Categories=System;Utility;
EOF
    update-desktop-database "$HOME/.local/share/applications" 2>/dev/null || true
    print_message "桌面快捷方式创建完成"
fi

# 创建卸载脚本
print_info "正在创建卸载脚本..."
cat > "$INSTALL_DIR/uninstall.sh" << EOF
#!/bin/bash
echo "正在卸载智能清理助手..."

# 删除符号链接
rm -f "$BIN_DIR/cleaner"

# 删除安装目录
rm -rf "$INSTALL_DIR"

# 删除桌面文件
rm -f "$HOME/.local/share/applications/cleaner.desktop"

echo "卸载完成！"
echo "请手动从 ~/.bashrc 或 ~/.zshrc 中移除 $BIN_DIR 的PATH配置"
EOF

chmod +x "$INSTALL_DIR/uninstall.sh"
print_message "卸载脚本创建完成"

# 显示安装信息
echo ""
echo "==============================================="
echo "               ✅ 安装完成！"
echo "==============================================="
echo ""
print_info "📦 安装信息:"
echo "  安装目录: $INSTALL_DIR"
echo "  JAR文件: cleaner.jar"
echo "  启动方式:"
echo "    • 在终端中输入: cleaner"
echo "    • 或直接运行: $BIN_DIR/cleaner"
echo ""
print_info "🚀 现在打开新的终端窗口，输入 cleaner 即可使用！"
echo ""
print_info "💡 卸载方法:"
echo "  运行: $INSTALL_DIR/uninstall.sh"
echo ""

# 询问是否立即启动
read -p "是否立即启动cleaner? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    print_info "正在启动cleaner..."
    "$BIN_DIR/cleaner"
fi

print_info "感谢使用智能清理助手！"