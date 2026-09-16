#!/usr/bin/env bash
#
# Cogito 一键发布流程（Linux / macOS / WSL / Git Bash）
#
# 做五件事：
#   1. 导出本轮聊天记录到 chat/
#   2. 改 pom.xml 版本号并构建（mvn clean package）
#   3. git add / commit / push 到 main
#   4. 打标签 v<版本> 并推送
#   5. 发 GitHub Release（jar + sha256 + 说明），需要 --release
#
# 用法：
#   bash tools/ship.sh --version 0.3.0-BETA --message "异想体镇压产出"
#   bash tools/ship.sh --version 0.3.0-BETA --message "异想体镇压产出" \
#        --since 2026-09-16T02:00:00Z --notes docs/release-notes/v0.3.0-BETA.md --release
#   bash tools/ship.sh --version 0.3.0-BETA --message "试跑" --dry-run
#
# 依赖：git、mvn（JDK 21）、node（导出记录用）、gh（发 Release 用，且已 gh auth login）
# 本脚本不会弹出任何窗口。
#
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_DIR"

VERSION=""
MESSAGE=""
SESSION=""
SINCE=""
NOTES=""
TRANSCRIPT_NAME=""
DO_RELEASE=0
SKIP_TRANSCRIPT=0
SKIP_BUILD=0
DRY_RUN=0

usage() {
    sed -n '3,20p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    exit "${1:-0}"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -v|--version)        VERSION="$2"; shift 2 ;;
        -m|--message)        MESSAGE="$2"; shift 2 ;;
        -s|--session)        SESSION="$2"; shift 2 ;;
        --since)             SINCE="$2"; shift 2 ;;
        -n|--notes)          NOTES="$2"; shift 2 ;;
        --transcript-name)   TRANSCRIPT_NAME="$2"; shift 2 ;;
        -r|--release)        DO_RELEASE=1; shift ;;
        --skip-transcript)   SKIP_TRANSCRIPT=1; shift ;;
        --skip-build)        SKIP_BUILD=1; shift ;;
        --dry-run)           DRY_RUN=1; shift ;;
        -h|--help)           usage 0 ;;
        *) echo "未知参数: $1" >&2; usage 1 ;;
    esac
done

if [[ -z "$VERSION" || -z "$MESSAGE" ]]; then
    echo "必须提供 --version 与 --message" >&2
    usage 1
fi

step() { printf '\033[36m==> %s\033[0m\n' "$1"; }
info() { printf '    %s\n' "$1"; }

TRANSCRIPT="${TRANSCRIPT_NAME:-transcript-$(date +%F)-$VERSION.md}"
TRANSCRIPT_PATH="chat/$TRANSCRIPT"
JAR="target/Cogito-$VERSION.jar"
SHA_FILE="dist/Cogito-$VERSION.jar.sha256"

# ---------------------------------------------------------------- 1. 聊天记录
step "① 导出聊天记录"
if [[ $SKIP_TRANSCRIPT -eq 1 ]]; then
    info "已跳过（--skip-transcript）"
else
    if [[ -z "$SESSION" ]]; then
        SESSION="$(find "$HOME/.codex/sessions" -name 'rollout-*.jsonl' -printf '%T@ %p\n' 2>/dev/null \
            | sort -nr | head -n1 | cut -d' ' -f2- || true)"
    fi
    if [[ -z "$SESSION" ]]; then
        echo "没找到会话文件，请用 --session 指定" >&2
        exit 1
    fi
    info "会话文件：$SESSION"

    node_args=(tools/export-transcript.js "$SESSION" "$TRANSCRIPT_PATH" --title "Cogito 项目 · $VERSION 会话记录")
    if [[ -n "$SINCE" ]]; then
        node_args+=(--since "$SINCE")
    fi
    if [[ $DRY_RUN -eq 1 ]]; then
        info "[dry-run] node ${node_args[*]}"
    else
        node "${node_args[@]}"
        info "已写出 $TRANSCRIPT"
    fi
fi

# ------------------------------------------------------------------ 2. 版本号
step "② 更新 pom.xml 版本号并构建"
if grep -q "<version>$VERSION</version>" pom.xml; then
    info "版本号已经是 $VERSION，跳过改写"
elif [[ $DRY_RUN -eq 1 ]]; then
    info "[dry-run] 把 pom.xml 版本改成 $VERSION"
else
    awk -v ver="$VERSION" '
        {
            if (!seen && $0 ~ /<artifactId>cogito<\/artifactId>/) { seen = 1; print; next }
            if (seen && !done && $0 ~ /<version>/) {
                sub(/<version>[^<]*<\/version>/, "<version>" ver "</version>")
                done = 1
            }
            print
        }' pom.xml > pom.xml.tmp && mv pom.xml.tmp pom.xml
    info "pom.xml 版本 -> $VERSION"
fi

if [[ $SKIP_BUILD -eq 1 ]]; then
    info "已跳过构建（--skip-build）"
elif [[ $DRY_RUN -eq 1 ]]; then
    info "[dry-run] mvn -B -ntp clean package"
else
    if ! command -v mvn >/dev/null 2>&1; then
        echo "找不到 mvn，请先安装 Maven（Arch: sudo pacman -S maven jdk21-openjdk）" >&2
        exit 1
    fi
    mvn -B -ntp clean package
    if [[ ! -f "$JAR" ]]; then
        echo "没有找到构建产物：$JAR" >&2
        exit 1
    fi
    mkdir -p dist
    sha256sum "$JAR" | sed "s|$JAR|Cogito-$VERSION.jar|" > "$SHA_FILE"
    info "产物：$JAR（$(stat -c%s "$JAR" 2>/dev/null || echo '?') 字节）"
    info "SHA256：$(cut -d' ' -f1 "$SHA_FILE")"
fi

# --------------------------------------------------------------- 3. 提交推送
step "③ 提交并推送到 main"
if [[ -z "$(git status --porcelain)" ]]; then
    info "没有改动需要提交"
elif [[ $DRY_RUN -eq 1 ]]; then
    info "[dry-run] git add -A && git commit -m \"$MESSAGE\" && git push origin main"
else
    git add -A
    git commit -q -m "$MESSAGE"
    git push origin main
    info "已推送：$(git log --oneline -1)"
fi

# ------------------------------------------------------------------ 4. 标签
step "④ 打标签 v$VERSION"
if [[ $DRY_RUN -eq 1 ]]; then
    info "[dry-run] git tag -a v$VERSION && git push origin v$VERSION"
elif git rev-parse -q --verify "refs/tags/v$VERSION" >/dev/null; then
    info "标签 v$VERSION 已存在，跳过"
else
    git tag -a "v$VERSION" -m "Cogito $VERSION"
    git push origin "v$VERSION"
    info "标签已推送"
fi

# -------------------------------------------------------------- 5. Release
if [[ $DO_RELEASE -eq 0 ]]; then
    step "⑤ 跳过 Release（未加 --release）"
else
    step "⑤ 发 GitHub Release"
    if [[ $DRY_RUN -eq 1 ]]; then
        info "[dry-run] gh release create v$VERSION $JAR $SHA_FILE --title \"Cogito $VERSION\" --prerelease ${NOTES:+--notes-file $NOTES}"
    else
        if ! command -v gh >/dev/null 2>&1; then
            echo "找不到 gh，请安装 GitHub CLI 并 gh auth login（Arch: sudo pacman -S github-cli）" >&2
            exit 1
        fi
        if [[ ! -f "$SHA_FILE" ]]; then
            mkdir -p dist
            sha256sum "$JAR" | sed "s|$JAR|Cogito-$VERSION.jar|" > "$SHA_FILE"
        fi
        release_args=(release create "v$VERSION" "$JAR" "$SHA_FILE" --title "Cogito $VERSION" --prerelease)
        if [[ -n "$NOTES" ]]; then
            release_args+=(--notes-file "$NOTES")
        fi
        gh "${release_args[@]}"
    fi
fi

echo
printf '\033[32m完成：Cogito %s\033[0m\n' "$VERSION"
if [[ $DRY_RUN -eq 1 ]]; then
    printf '\033[33m（这是 dry-run，什么都没改）\033[0m\n'
fi
