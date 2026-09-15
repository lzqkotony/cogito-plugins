# 聊天记录与设计稿

这个目录放着 Cogito 从零到现在（v0.1.0-BETA + 箱子 GUI）的完整对话记录。
目的很直接：**换台电脑 `git clone` 下来，把这里的 md 丢给 Codex，就能接着写**，不用再从零解释一遍。

## 文件

| 文件 | 内容 |
| --- | --- |
| `transcript-2026-09-15-machine1-v0.1.md` | 第一台机器：搭 Java 环境 → 定玩法 → 写脑啡肽经济与 Vault 兑换 → 开源 → 发布 0.1.0 Beta → 打交接包 |
| `transcript-2026-09-15-machine2-env-gui.md` | 第二台机器：读交接包 → 搭离线开发环境 → 加箱子 GUI（`/cogito gui`）→ 推 GitHub |
| `attachments/design-sketch-handwritten.jpg` | 最初的手写玩法设计稿原图（脑啡肽产出区间、E.G.O 三档失败率等） |

## 新机器上怎么用

1. `git clone git@github.com:lzqkotony/cogito-plugins.git`
2. 让 Codex 先读 `README.md` + `docs/design.md`（当前状态与玩法设计）
3. 想要完整上下文，再把 `chat/transcript-*.md` 一起给它——里面有你每次拍的板（比如"数字是镇压异想体给的脑啡肽范围""抽卡池子和提取探索后续再做"）以及我解释过的取舍

## 几点说明

- 时间戳是 UTC+8；工具调用做了截断，内部推理（reasoning）没有导出。
- **原始的 jsonl 会话文件没有上传**：除了对话，它还包含 Codex 的系统提示与完整内部推理，不适合放进公开仓库。需要的话在本机 `~/.codex/sessions/年/月/日/` 里能找到。
- 离线开发环境包（JDK + Maven + Git + Maven 仓库快照，约 324 MB）也没进仓库，体积太大；需要时用 [tools/bin/make-offline-package.ps1] 那个脚本重新生成，或者在第二台机器上直接跑现成的那份。
- 项目是 vibe coding 出来的：代码、文档、脚本基本由 AI 写，人负责出设计和拍板。看到奇怪的实现别太惊讶，欢迎提 issue。