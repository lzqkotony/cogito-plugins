# 开发工作记录

按时间线记录这个项目做过什么、谁在什么时候定了什么决策。**换电脑接手时，先看这份 + `design.md`**。

---

## 2026-09-15 · 第一台机器（Windows / seewo）

- 搭 Java 开发环境（JDK 21 与 26、Maven 3.9.16、Gradle 9.7.1，装在本地 `tools/`）
- 确定方向：做一个「脑叶」主题的 Paper 服务端插件（原型 Project Moon 三部曲）
- 实现 **v0.1**：
  - 脑啡肽做成**实体物品**（NBT 标签的附魔绿宝石块）
  - 接 **Vault 经济**：100 金钱 → 1 脑啡肽，**单向不可逆**
  - `/enkephalin`（查看/发放/扣除/查看配置/重载）、Tab 补全、权限节点
  - 全部文案走 MiniMessage，配置与日志 UTF-8
- 开源：GitHub `lzqkotony/cogito-plugins`，**GPL-3.0**，发布 **0.1.0-BETA** Release（jar + sha256）
- 导出交接包（聊天记录 + 工程 + 脚本），放到桌面，交给另一台电脑

## 2026-09-15 晚 · 第二台机器（Windows / Administrator）

- 读交接包 → 先搭**离线开发环境包**（JDK + Maven + Git + 本地仓库快照，约 324 MB）
- 新增**箱子 GUI**：`/cogito gui`（别名 `/cog`、`/box`，`/enkephalin box` 同效）
  - 5 行 45 格：持有量信息、余额、×1/×8/×64 兑换按钮、命令帮助、管理员配置/重载、关闭
  - 菜单物品锁死（点击/拖拽全部取消），不会被 shift 搬走
- 把两份聊天记录与手写设计稿原图放进 `chat/`，README 标注为 vibe coding 项目
- 推送 GitHub（`39abe29`、`930dc39`）

## 2026-09-16 · 第一台机器（回到本机，先 clone 再继续）

- 从 GitHub 克隆/快进到最新，读第二台机器的聊天记录，确认 GUI 改动
- 按**第二批手写稿**（08:08 拍的三页）实现**物品注册体系（v0.2.0-BETA）**：
  - `items.yml` 注册 5 个物品：`pe` 脑啡肽(绿宝石)、`pe-module` 脑啡肽模块(绿宝石块，别名 PE-BOX)、`cogito` Cogito(跳跃药水，堆叠上限改成 64)、`ego-blueprint` E.G.O 研发图纸(纸)、`golden-bough` 金枝(树枝)
  - 统一 NBT 标签 `cogito:item=<id>`；`legacy-tags` 兼容旧 `enkephalin` 标签，老物品不失效
  - **原版行为拦截**：注册物品不可放置、不可参与合成（可按物品配置）
  - 新命令：`/cogito items`、`/cogito give <物品id> <玩家> <数量>`、`/cogito debug <物品id>`（自检）
  - `config.yml`（经济/界面）与 `items.yml`（物品）分家
- **验证**（都是真跑出来的）：
  - Maven 编译 14 个源文件通过，产物 `Cogito-0.2.0-BETA.jar`
  - 本地 Paper 测试服**后台无窗口**启动，日志：`已注册 5 个物品：pe, pe-module, cogito, ego-blueprint, golden-bough`、`Vault 经济已连接：EssentialsX Economy`
  - `/cogito debug pe` → 材质 EMERALD、堆叠 64、标签 `cogito:item=pe`、识别通过
  - `/cogito debug cogito` → POTION、**堆叠上限 64**、识别通过
- 第二批设计稿整理进 `docs/design.md`（含命令清单与实现状态、物品表、E.G.O 抗性公式），三张手写稿原图入库
- 推送 GitHub（`60f6948`）

---

## 开发与交付方式

| 场景 | 做法 |
| --- | --- |
| 本机（Windows） | `dev\build.ps1` 打包（`-DeployTo <路径>` 可直接拷到服务器 plugins） |
| 其他机器（Linux/macOS） | `mvn clean package` 或 `bash build.sh` |
| 交付 | 把 jar 放进服务器 `plugins/`，重启或 `/reload confirm` |
| 服务器前置 | **Vault + 一个经济插件**（EssentialsX / CMI 等） |
| 用户正式服 | Rocky Linux、纯 IPv6、`mc.ezmscc.dpdns.org` |
| 聊天记录导出 | `tools/export-transcript.js`（默认读 `~/.codex/sessions/…/rollout-*.jsonl`） |

---

## 用户已拍板的设计决策

- 经济对接 **Vault**；脑啡肽是**实体物品**，不是余额数字
- 兑换**单向**：钱 → 脑啡肽，不提供反向
- 镇压异想体产出脑啡肽的区间：**ALEPH 100~60 / WAW 60~30 / HE 30~20 / TETH 20~10 / ZAYIN 10~8**
- E.G.O 定向开发：某个异想体累计**镇压 5 次**解锁；失败率 普通 **50%** / 高级 **25%**（费用 +20%）/ 决断 **0%**（费用 +50%）
- 随机开发（抽卡）与提取探索：**后续再做**
- 注册物品必须打 NBT 标签、不保留原型功能（不可放置/合成）、脑啡肽与 Cogito 可堆叠
- 异想体与 E.G.O 的具体内容：后续再加

## 待拍板（详见 `design.md` 第 12 节）

1. 脑啡肽材质从"绿宝石块"改成新稿的"绿宝石"（已实现 + 兼容旧物品）是否确认
2. 抗性公式 `1-(1-x)y/4` 里 y 是件数还是权重、x 的上限
3. 工具型异想体的「代价」
4. E.G.O 与原版混搭：禁止穿 or 允许穿但特性失效
5. `/cogito set <rule>` 的 rule 清单
6. 「提取」与「镇压」的区别（是否消耗脑啡肽模块）
