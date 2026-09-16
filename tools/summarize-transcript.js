// 从导出的对话稿里提取「用户说了什么」和「最后几条助手回复」，方便快速掌握上下文
// 用法: node summarize-transcript.js <transcript.md> [最后N条助手回复]
const fs = require('fs');

const file = process.argv[2];
const tailCount = Number(process.argv[3] || 3);
const md = fs.readFileSync(file, 'utf8');

const header = /^## (👤 用户|🤖 Codex)\s*`([^`]*)`\s*$/gm;
const marks = [];
let m;
while ((m = header.exec(md)) !== null) {
  marks.push({ who: m[1], time: m[2], start: header.lastIndex, at: m.index });
}

const blocks = marks.map((mark, i) => ({
  who: mark.who,
  time: mark.time,
  text: md.slice(mark.start, i + 1 < marks.length ? marks[i + 1].at : md.length).trim(),
}));

const users = blocks.filter((b) => b.who === '👤 用户');
const assistants = blocks.filter((b) => b.who === '🤖 Codex');

const clip = (text, max) => (text.length > max ? `${text.slice(0, max)}\n…(截断，原文 ${text.length} 字符)` : text);

console.log(`文件：${file}`);
console.log(`用户消息 ${users.length} 条，助手回复 ${assistants.length} 条\n`);
console.log('======== 用户说过的每一句 ========\n');
for (const block of users) {
  console.log(`[${block.time}] ${clip(block.text.replace(/\n+/g, ' '), 700)}\n`);
}

console.log(`======== 最后 ${tailCount} 条助手回复 ========\n`);
for (const block of assistants.slice(-tailCount)) {
  console.log(`--- [${block.time}] ---`);
  console.log(clip(block.text, 2500));
  console.log();
}
