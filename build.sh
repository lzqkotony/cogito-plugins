#!/usr/bin/env bash
# 构建 Cogito 插件 jar（Linux / macOS / WSL 通用）
#
#   bash build.sh                    # 只构建
#   bash build.sh -d /opt/paper/plugins   # 构建后拷贝到服务器 plugins 目录
#
# 需要 JDK 21+ 与 Maven：
#   Rocky/RHEL: sudo dnf install -y java-21-openjdk-devel maven
#   Debian/Ubuntu: sudo apt install -y openjdk-21-jdk maven
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

command -v mvn >/dev/null 2>&1 || { echo "未找到 mvn，请先安装 Maven" >&2; exit 1; }
command -v javac >/dev/null 2>&1 || { echo "未找到 javac，请先安装 JDK 21" >&2; exit 1; }

echo "使用 JDK: $(javac -version 2>&1)"
mvn -B -ntp clean package

JAR="$(find "$PROJECT_DIR/target" -maxdepth 1 -name 'Cogito-*.jar' ! -name 'original-*' | head -n 1)"
[[ -n "$JAR" ]] || { echo "没找到构建产物" >&2; exit 1; }

echo
echo "构建完成: $JAR"
sha256sum "$JAR"

if [[ "${1:-}" == "-d" && -n "${2:-}" ]]; then
    DEPLOY_DIR="$2"
    [[ -d "$DEPLOY_DIR" ]] || { echo "目录不存在: $DEPLOY_DIR" >&2; exit 1; }
    rm -f "$DEPLOY_DIR"/Cogito-*.jar
    cp "$JAR" "$DEPLOY_DIR/"
    echo "已拷贝到: $DEPLOY_DIR（重启服务器或 /reload confirm 生效）"
fi
