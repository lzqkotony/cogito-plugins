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
