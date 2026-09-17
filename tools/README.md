# tools

聊天记录的导出工具（Node.js，无需安装依赖）。

## export-transcript.js

把 Codex 的会话文件转成可读 Markdown，用来往 `chat/` 里补记录。

```bash
# 完整导出
node tools/export-transcript.js ~/.codex/sessions/2026/09/16/rollout-xxx.jsonl chat/transcript-2026-09-16-xxx.md

# 只导出某个时间点之后的部分（同一线程分阶段导出时用，时间戳按 UTC）
node tools/export-transcript.js \
  ~/.codex/sessions/2026/09/15/rollout-xxx.jsonl \
  chat/transcript-2026-09-16-machine3-items-registry.md \
  --since 2026-09-15T23:30:00Z \
  --title "Cogito 项目 · 第三台机器（物品注册表）会话记录"
```

Windows 上的会话文件位置：`C:\Users\<用户名>\.codex\sessions\年\月\日\rollout-*.jsonl`。
导出内容 = 用户消息 + 助手回复 + 工具调用摘要（内部推理与超长工具输出会被裁掉）。

## summarize-transcript.js

把导出的 md 压缩成「用户说过什么 + 最后几条助手回复」，用来快速接手上下文。

```bash
node tools/summarize-transcript.js chat/transcript-2026-09-15-machine2-env-gui.md 2
```

> 提醒：**原始的 jsonl 会话文件不要提交进仓库**——除了对话，它还包含 Codex 的系统提示与完整内部推理，不适合公开。

## ship.ps1

> 版本号约定：同一功能线内递增补丁号（例如当前线是 `0.5.x`，下一个版本就是 `0.5.1`），
> 开新功能线才升 minor（`0.6.0`）。

一键发布流程（Windows）：**导出聊天记录 → 改版本号并构建 → 提交推送 → 打标签 → 发 Release**。

```powershell
# 常规一轮：提交源码 + 推聊天记录，不发 Release
powershell -ExecutionPolicy Bypass -File tools\ship.ps1 -Version 0.3.0-BETA -Message "异想体镇压产出"

# 完整发布（含 Release，附件是 jar + sha256）
powershell -ExecutionPolicy Bypass -File tools\ship.ps1 `
  -Version 0.3.0-BETA -Message "异想体镇压产出" `
  -Since 2026-09-16T02:00:00Z -NotesFile ..\release-notes-0.3.0-BETA.md -Release

# 只演练不执行
powershell -ExecutionPolicy Bypass -File tools\ship.ps1 -Version 0.3.0-BETA -Message "试跑" -DryRun
```

参数：`-Session`（会话 jsonl，默认取最近修改的那个）、`-Since`（只导出该 UTC 时间之后的记录）、`-NotesFile`（Release 说明）、`-SkipTranscript` / `-SkipBuild`、`-Release`、`-DryRun`。
脚本不弹任何窗口；`gh` 优先用 PATH 里的，其次用开发机上的 `tools\gh\bin\gh.exe`。

> ⚠️ 仓库里的 `.ps1` 必须存成 **UTF-8 with BOM**：Windows PowerShell 5.1 会按系统 GBK 读无 BOM 的脚本，
> 中文注释会让它直接语法报错。改完脚本记得检查前三字节是不是 `EF BB BF`。

## ship.sh

`ship.ps1` 的 Linux / macOS / WSL / Git Bash 版本，流程和参数完全对应：

```bash
# 常规一轮：提交源码 + 推聊天记录
bash tools/ship.sh --version 0.3.0-BETA --message "异想体镇压产出"

# 完整发布（附 jar + sha256）
bash tools/ship.sh --version 0.3.0-BETA --message "异想体镇压产出" \
  --since 2026-09-16T02:00:00Z --notes docs/release-notes/v0.3.0-BETA.md --release

# 演练
bash tools/ship.sh --version 0.3.0-BETA --message "试跑" --dry-run
```

Arch Linux 上需要的基本工具：`sudo pacman -S --needed git jdk21-openjdk maven nodejs github-cli`。

**WSL 用户注意**：WSL 里的 `$HOME` 是 Linux 家目录，脚本默认找不到 Windows 那边的 Codex 会话文件，
要用 `--session /mnt/c/Users/<用户名>/.codex/sessions/年/月/日/rollout-*.jsonl` 显式指定；
另外 WSL 里通常需要自己装 `nodejs` 与 `github-cli`（`sudo pacman -S --needed nodejs github-cli`）。

| 功能 | ship.ps1 | ship.sh |
| --- | --- | --- |
| 指定版本 / 提交信息 | `-Version` / `-Message` | `--version` / `--message` |
| 指定会话文件 / 起始时间 | `-Session` / `-Since` | `--session` / `--since` |
| Release 说明 / 记录文件名 | `-NotesFile` / `-TranscriptName` | `--notes` / `--transcript-name` |
| 发 Release / 跳过步骤 / 演练 | `-Release` / `-SkipTranscript` `-SkipBuild` / `-DryRun` | `--release` / `--skip-transcript` `--skip-build` / `--dry-run` |
