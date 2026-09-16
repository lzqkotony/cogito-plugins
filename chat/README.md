# 聊天记录与设计稿

这个目录放着 Cogito 从零到现在（v0.4.0-BETA）的完整对话记录。
目的很直接：**换台电脑 `git clone` 下来，把这里的 md 丢给 Codex，就能接着写**，不用再从零解释一遍。

## 文件

| 文件 | 内容 |
| --- | --- |
| `transcript-2026-09-15-machine1-v0.1.md` | 第一台机器：搭 Java 环境 → 定玩法 → 写脑啡肽经济与 Vault 兑换 → 开源 → 发布 0.1.0 Beta → 打交接包 |
| `transcript-2026-09-15-machine2-env-gui.md` | 第二台机器：读交接包 → 搭离线开发环境 → 加箱子 GUI（`/cogito gui`）→ 推 GitHub |
| `transcript-2026-09-16-machine3-items-registry.md` | 第三轮（回到第一台机器）：clone 最新代码 → 按第二批手写稿做物品注册表、自检命令 → 推 GitHub |
| `transcript-2026-09-17-0.4.0-BETA.md` | 第四轮：确认 E.G.O. 红/蓝伤害与统一抗性规则 → 实现套装/防具/伤害通道 → 阻止 Cogito 饮用 → 部署正式服 → 发布 0.4.0 Beta |
| `attachments/design-sketch-handwritten.jpg` | 最初的手写玩法设计稿原图（脑啡肽产出区间、E.G.O 三档失败率等） |
| `../docs/worklog.md` | 开发工作记录：各轮做了什么、用户拍板过什么、待确认什么 |
| `../docs/design.md` | 完整玩法设计（两批手写稿的整理版） |
| `attachments/design-sketch-2026-09-16-1.jpg` | 第二批手写稿之一：整体设计思路、OP/非 OP 命令清单 |
| `attachments/design-sketch-2026-09-16-2.jpg` | 第二批手写稿之二：注册物品表（脑啡肽/模块/Cogito/图纸/金枝）与"打 NBT 标签、不可放置合成"要求 |
| `attachments/design-sketch-2026-09-16-3.jpg` | 第二批手写稿之三：异想体五阶、E.G.O 护甲/武器/饰品、抗性值与伤害公式 |

## 新机器上怎么用

1. `git clone git@github.com:lzqkotony/cogito-plugins.git`
2. 让 Codex 先读 `README.md` + `docs/design.md`（当前状态与玩法设计）
3. 想要完整上下文，再把 `chat/transcript-*.md` 一起给它——里面有你每次拍的板（比如"数字是镇压异想体给的脑啡肽范围""抽卡池子和提取探索后续再做"）以及我解释过的取舍

## 几点说明

- 时间戳是 UTC+8；工具调用做了截断，内部推理（reasoning）没有导出。
- **原始的 jsonl 会话文件没有上传**：除了对话，它还包含 Codex 的系统提示与完整内部推理，不适合放进公开仓库。需要的话在本机 `~/.codex/sessions/年/月/日/` 里能找到。
- 离线开发环境包（JDK + Maven + Git + Maven 仓库快照，约 324 MB）也没进仓库，体积太大；那份在第二台机器上，需要时在那边重新生成或直接拷贝。
- 本仓库的 `tools/export-transcript.js` 就是生成上面这些 md 的脚本：它把 Codex 的会话文件（`~/.codex/sessions/年/月/日/rollout-*.jsonl`）转成可读 Markdown，支持 `--since` 只导出某时间点之后的部分。
- 项目是 vibe coding 出来的：代码、文档、脚本基本由 AI 写，人负责出设计和拍板。看到奇怪的实现别太惊讶，欢迎提 issue。
