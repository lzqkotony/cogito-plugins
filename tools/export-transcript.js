// 把 Codex 的 rollout jsonl 导出成可读的 Markdown 对话稿
//
// 用法:
//   node export-transcript.js <input.jsonl> <output.md> [--since 2026-09-15T23:00:00Z] [--title 标题]
//
// --since 只导出该时间点之后的记录（时间戳按 UTC 比较），用于同一线程分阶段导出。
const fs = require('fs');
const path = require('path');

const raw = process.argv.slice(2);
const positional = [];
const options = {};
for (let i = 0; i < raw.length; i++) {
  if (raw[i] === '--since' || raw[i] === '--title') {
    options[raw[i].slice(2)] = raw[++i];
  } else {
    positional.push(raw[i]);
  }
}
const [input, output] = positional;
const since = options.since ? Date.parse(options.since) : null;
const title = options.title || 'Cogito 项目 · 会话记录';

if (!input || !output) {
  console.error('用法: node export-transcript.js <input.jsonl> <output.md> [--since ISO时间] [--title 标题]');
  process.exit(1);
}

const allLines = fs.readFileSync(input, 'utf8').split('\n').filter(Boolean);
const lines = since
  ? allLines.filter((line) => {
      try {
        const record = JSON.parse(line);
        const time = Date.parse(record.timestamp || '');
        return Number.isFinite(time) ? time >= since : true;
      } catch {
        return true;
      }
    })
  : allLines;

// 时间戳转北京时间（UTC+8）
function toLocal(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const local = new Date(d.getTime() + 8 * 3600 * 1000);
  return local.toISOString().replace('T', ' ').slice(0, 19);
}

function truncate(text, max) {
  const s = String(text ?? '');
  return s.length > max ? `${s.slice(0, max)} …(已截断，原文 ${s.length} 字符)` : s;
}

function inline(text, max) {
  return truncate(text, max).replace(/\r?\n/g, ' ⏎ ').replace(/`/g, "'");
}

const parts = [];
let userCount = 0;
let assistantCount = 0;
let toolCount = 0;

parts.push(`# ${title}`);
parts.push('');
parts.push(`- 源文件：\`${path.basename(input)}\``);
if (since) {
  parts.push(`- 仅包含 ${new Date(since).toISOString()}（UTC）之后的记录`);
}
parts.push(`- 记录条数：${lines.length}`);
parts.push(`- 导出时间：${toLocal(new Date().toISOString())}（北京时间）`);
parts.push('');
parts.push('> 说明：这是把 Codex 的原始会话记录（jsonl）转成的可读版本，保留用户与助手的完整对话、以及工具调用的摘要。内部推理过程（reasoning）已省略，工具输出做了截断。原始文件在同目录，包含全部细节。');
parts.push('');
parts.push('---');

for (const line of lines) {
  let record;
  try {
    record = JSON.parse(line);
  } catch {
    continue;
  }
  if (record.type !== 'response_item' || !record.payload) continue;

  const payload = record.payload;
  const time = toLocal(record.timestamp);

  if (payload.type === 'message') {
    const role = payload.role;
    if (role !== 'user' && role !== 'assistant') continue;
    const content = payload.content || [];
    const texts = content
      .filter((c) => c.type === 'input_text' || c.type === 'output_text')
      .map((c) => c.text);
    const images = content.filter((c) => c.type === 'input_image').length;
    if (!texts.length && !images) continue;

    if (role === 'user') userCount++;
    else assistantCount++;

    parts.push('');
    parts.push(`## ${role === 'user' ? '👤 用户' : '🤖 Codex'}　\`${time}\``);
    parts.push('');
    for (const text of texts) {
      const trimmed = text.trim();
      if (trimmed) parts.push(trimmed, '');
    }
    if (images) parts.push(`> （这条消息附带了 ${images} 张图片，原图见 chat/attachments/）`, '');
    continue;
  }

  if (payload.type === 'function_call') {
    toolCount++;
    parts.push(`- 🔧 \`${payload.name}\`：\`${inline(payload.arguments, 180)}\``);
    continue;
  }

  if (payload.type === 'function_call_output') {
    const raw = typeof payload.output === 'string' ? payload.output : JSON.stringify(payload.output);
    parts.push(`  - ↳ 输出：\`${inline(raw, 200)}\``);
    continue;
  }

  if (payload.type === 'custom_tool_call') {
    toolCount++;
    parts.push(`- ✏️ 工具 \`${payload.name || 'custom'}\` 输入：\`${inline(payload.input, 160)}\``);
  }
}

parts.push('');
parts.push('---');
parts.push('');
parts.push(`**统计**：用户消息 ${userCount} 条，助手回复 ${assistantCount} 条，工具调用 ${toolCount} 次。`);

fs.writeFileSync(output, parts.join('\n'), 'utf8');
console.log(`已生成 ${output}`);
console.log(`  用户消息 ${userCount} 条 / 助手回复 ${assistantCount} 条 / 工具调用 ${toolCount} 次`);
console.log(`  大小 ${(fs.statSync(output).size / 1024).toFixed(1)} KB`);
