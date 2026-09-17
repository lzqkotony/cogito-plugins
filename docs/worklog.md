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

## 2026-09-16 · 第一台机器（玩家数据层 core data）

- 实现 **SQLite 玩家数据层**（`data/` 包）：`PlayerProfile`（等级 / 镇压次数 / E.G.O 解锁）、
  `SqlitePlayerDataStore`（三张表、WAL、事务 upsert）、`PlayerDataService`（内存缓存 + 每 30 秒异步落库 + 关服写回）
  - 驱动直接用服务端自带的 `org.xerial:sqlite-jdbc`，不额外引入依赖、不 shade
- 新命令：`/cogito set Lv <玩家> <数字>`、`/cogito reset player_information <玩家>`、
  `/cogito data <玩家>`、`/cogito debug db`（数据库读写自检）
- 版本 → **0.3.0-BETA**
- 用 **Docker** 起了个 Paper 1.21.11 实例（WSL Arch + itzg/minecraft-server 镜像）实际验证：
  插件加载、启用、物品注册、数据库初始化、Vault 连接全部正常，无异常
  （中途踩坑：Docker Hub 被墙 → 配国内镜像源；镜像自身要从 raw.githubusercontent.com 拉默认配置会卡住，与插件无关）
- 918（九一八纪念）插件的草稿写完但**未采用**，用户交给另一位开发者，草稿移到 `work/other-projects/mingji-918-draft/`

## 2026-09-17 · 新机器（Linux）· E.G.O. 伤害与抗性

- 从 GitHub 重新 clone，读取此前全部工作记录，确认接手点
- 明确 E.G.O. 伤害规则（用户拍板）：
  - 攻击侧只有 **红伤 / 蓝伤** 两条通道
  - **蓝伤只能由 E.G.O. 来源造成**
  - 防具侧每套 E.G.O. 只有一个统一抗性 `x`，对**所有传入伤害**生效，不按伤害类型区分
  - `x` 不设代码范围，直接使用 `ego.yml` 中标注的值
  - `y = 同一套有效 E.G.O. 防具件数`，不设置部位权重；武器与饰品不计入
  - 公式 `r = 1 - (1 - x) * y / 4`，最终伤害保持 `double` 交给 Minecraft
  - 混入原版防具或其他 E.G.O. 防具时，全部 E.G.O. 抗性失效，`r=1`
  - E.G.O. 防具不能附魔，不提供原版护甲值与盔甲韧性
- 实现 `ego/` 包：伤害通道、装备部位、套装注册、纯公式、伤害服务与监听器
- 新增 `ego.yml`，内置可测试的 `失乐园` 套装：`x=0.1`、四件防具、红/蓝测试武器、饰品
- 新增管理员自检：
  - `/cogito give ego <玩家> <套装id> <数量>` 发放整套 E.G.O.
  - `/cogito debug ego <玩家>` 查看当前套装、件数 `y`、抗性 `x` 与倍率 `r`
  - `/cogito debug attack <red|blue> <攻击者> <目标> <伤害>` 验证蓝伤来源限制
- 同步修复：**Cogito 不能被饮用**，右键不再触发跳跃药水效果
- 版本 → **0.4.0-BETA**，增加 Maven 单元测试与 GitHub Actions 自动构建 / Release
- 本地 `mvn clean test package`：BUILD SUCCESS，5/5 测试通过；JAR SHA-256 `e9b7e9c3a75365937a45d76068179e17d0472ee9ee8cc9d842a4e2607900084f`
- 正式服部署：停止实例 → 备份并停用 `Cogito-0.3.0-BETA.jar` → 写入 `Cogito-0.4.0-BETA.jar` → 启动实例
- 正式服加载验证：识别 `Cogito v0.4.0-BETA`、注册 5 个基础物品和 1 套 E.G.O.（7 件）、SQLite 与 Vault 正常
- 控制台自检：`/cogito items` 返回 12 个注册物品；`/cogito debug cogito` 识别到 `cogito:item=cogito`、堆叠上限 64、不可放置/合成

## 2026-09-17 · 第一台机器（E.G.O. 开发台与专属图纸）

- 设计稿（09-17 手写页）确认：开发台方块、专属图纸解锁研发、爆炸抗性同黑曜石、需钻石镐以上
- 用户拍板：**图纸不是消耗品**，每套 E.G.O. 有自己专属的一张，右键解锁后仍留在手上；
  研发规则用新的（解锁后每次研发只消耗脑啡肽）；代价与共鸣后面再说
- 实现：
  - `develop/DevelopTableManager`：开发台坐标记进**区块 PDC** + 上方 `TextDisplay` 全息文本
  - `listener/DevelopListener`：放置登记、钻石镐限制、爆炸保护、右键开界面、图纸右键解锁
  - `gui/DevelopMenu`：列出套装（抗性 / 消耗 / 是否解锁），点击消耗脑啡肽产出整套
  - 合成表：四角下界合金块 / 四边脑啡肽模块 / 中心工作台（`ShapedRecipe`，重载时自动替换）
  - 图纸物品由 `EgoRegistry` 按 `blueprint-<套装id>` 自动生成；`ego.yml` 新增 `develop.cost`
  - 修正 `ItemBehaviourListener`：插件自己的配方允许使用自定义物品当材料
- 版本 → **0.5.0-BETA**；`mvn clean test package` 通过（31 源文件、5/5 测试）

---

## 开发与交付方式

### 版本号约定（2026-09-17 用户定）

- **0.5.x 是同一个功能线**：开发台、图纸解锁以及后续的修复 / 小功能（E.G.O. 代价、共鸣、饰品四部位等）
  都按**补丁号**递增 —— **下一个版本是 `0.5.1`**，再往后 `0.5.2`、`0.5.3`……
- 只有**开启新功能线**时才升 minor（例如异想体镇压产出 → `0.6.0`）。
- 每次发版仍然走 `tools/ship.ps1` / `tools/ship.sh`（导出聊天记录 → 构建 → 推送 → 打 tag → Release）。

| 场景 | 做法 |
| --- | --- |
| 本机（Windows） | `dev\build.ps1` 打包（`-DeployTo <路径>` 可直接拷到服务器 plugins） |
| 本机（Arch WSL） | 仓库在 `~/cogito-plugins`，JDK21 + Maven + Node + gh 已就绪，构建 `mvn clean package`，发布 `bash tools/ship.sh`（见 `docs/dev-setup.md`） |
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
- **脑啡肽材质 = 绿宝石、脑啡肽模块 = 绿宝石块**（2026-09-16 确认，按第二批设计稿）
- 异想体与 E.G.O 的具体内容：后续再加

## 待拍板（详见 `design.md` 第 12 节）

1. ~~脑啡肽材质~~ ✅ 已确认：用新的（绿宝石 / 模块绿宝石块）
2. 抗性公式 `1-(1-x)y/4` 里 y 是件数还是权重、x 的上限
3. 工具型异想体的「代价」
4. E.G.O 与原版混搭：禁止穿 or 允许穿但特性失效
5. `/cogito set <rule>` 的 rule 清单
6. 「提取」与「镇压」的区别（是否消耗脑啡肽模块）

> 2~6 用户 2026-09-16 表示"先不管"，等后面需要时再定。

---

## 固定工作流（2026-09-16 起）

用户要求：**每轮工作结束都要提交源码 + 聊天记录，并按版本发 Release**。为此提供了 `tools/ship.ps1`（Windows 一键流程）：

```
1. 导出本轮聊天记录到 chat/（可用 -Since 只导增量）
2. 改 pom.xml 版本号 → mvn package 构建
3. git add/commit/push 到 main
4. 打 tag v<版本> 并推送
5. gh release create 发 Release（jar + sha256 + 说明）
```

用 `-DryRun` 可以只看流程不执行。
