# Cogito

> 脑叶服务器核心玩法插件 —— Minecraft **Paper 1.21.11**
>
> A Paper plugin for the "Brain Leaf" style server gameplay: an **Enkephalin** item economy wired to **Vault**.

[![License](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/lzqkotony/cogito-plugins?include_prereleases&label=release)](https://github.com/lzqkotony/cogito-plugins/releases)
[![Paper](https://img.shields.io/badge/Paper-1.21.11-orange.svg)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21-red.svg)](https://adoptium.net)

Cogito 把「脑叶」玩法的第一层——**脑啡肽**——做成了一套可用的经济系统：脑啡肽是实体物品，玩家通过镇压异想体、提取探索、用钱兑换获得，用于后续的 E.G.O 开发。

---

## 这是一个 Vibe Coding 项目

这个仓库里的代码、文档、构建脚本，基本都是在 Codex（AI）里边聊边写出来的：人负责出设计、给方向、拍板验收，
AI 负责实现和验证——也就是所谓 **vibe coding**。

完整对话记录（包括最初的手写设计稿照片）放在 [`chat/`](chat/)，换台电脑 `git clone` 下来就能接着开发，
不用重新解释一遍来龙去脉。

> 说好听点是「人机协作」，说直白点就是想到哪写到哪、跑得起来就算数。所以看到奇怪的实现别太惊讶，欢迎提 issue。

---

## 功能

- **物品注册表（items.yml）**：脑啡肽、脑啡肽模块、Cogito、E.G.O 研发图纸、金枝，全部配置化定义（材质、名称、Lore、附魔、光效、堆叠上限、能否放置/合成）
- **脑啡肽实体物品**：带 `PersistentDataContainer` NBT 标签（`cogito:item=pe`）的自定义物品；玩家改名、附魔、堆叠都不会失效，也无法伪造
- **E.G.O. 攻击通道**：只提供红伤 / 蓝伤两条通道；蓝伤只能由手持 `attack-channel: BLUE` 的 E.G.O. 武器发起
- **E.G.O. 统一防具抗性**：整套防具共享一个 `x`，对全部 Minecraft 传入伤害统一生效；按穿着件数 `y` 插值，混搭时抗性失效。鞘翅是唯一例外：允许占用胸甲槽且不会使套装失效，但不计入件数 `y`
- **E.G.O. 装备限制**：不提供原版护甲值与盔甲韧性、无限耐久、不可附魔，武器和饰品不计入抗性件数
- **原版行为拦截**：注册物品不可放置、不可参与合成（可在 items.yml 关掉）；Cogito 也不能被当作药水饮用
- **Vault 经济兑换**：`/exchange` 用服务器货币按 100:1 单向兑换脑啡肽（价格可配）
- **玩家数据层（core data）**：SQLite 存档（等级、各异想体镇压次数、已解锁 E.G.O），上线读档 / 退服存档 / 定时异步落库
- **E.G.O. 开发台与一次性蓝图**：蓝图装载后绑定玩家并消耗；护甲与武器分开研发，支持普通 / 进阶 / 高级 / 完全四档费用与成功率；开发台方块爆炸抗性同黑曜石、需钻石镐以上才能破坏
- **套装能力与武器技能**：支持按防具件数 `y` 生效的最大生命、攻速、攻击力等套装属性和被动技能；`正义裁决者` 会在满蓄力挥剑时自动发动前方范围攻击
- **失乐园 E.G.O.**：铁甲 + 红石 `wayfinder` 纹饰，按件数提供生命恢复、抗性提升、抗火与水下呼吸；所有伤害都会进入抗性和 `x=0.1` 计算，随后由每 `10s` 增长 1 点、最高 `20` 点的“神圣”黄盾抵挡，不再有单次伤害免疫。铁锄空挥会攻击以玩家为中心、前后左右上下各 8 格内的生物，排除本人与召唤使徒；每 20 秒发动黑色痕迹特殊横扫，右键直接召唤 12 位使徒并令二者悬空 6 秒后落地
- **黄盾 UI**：失乐园 Y=4 的精确黄盾值会通过 ActionBar 显示，例如 `神圣黄盾 13.5/20`，破盾时显示提示
- **拟态 E.G.O.**：红皮革 + 金色纹饰的三阶段套装；拟态 I/III 提供攻击加成与命中回复，致死伤害触发拟态 II，III 阶段通过受伤/造成伤害积累模仿层并使用单体穿刺或范围爆发
- **服主 E.G.O. 获取白名单**：白名单按套装配置，支持 Java 名、`BE_` 名称与 UUID；命令发放、研发、地面拾取和容器取出都会校验，OP 也不能绕过，非白名单玩家登录时会自动移除已有受限物品
- **PALE / 蓝伤百分比**：蓝伤按 Project Moon 规则折算为目标最大生命值的百分比；`5` 点蓝伤表示 `5%` 最大生命值，最终仍经过 E.G.O. 抗性结算
- **箱子 GUI**：`/cogito gui` 打开箱子样式的菜单，看持有量 / 余额，点按钮直接兑换（物品锁死，拿不走）
- **准确的背包统计**：识别只看 NBT 标签，玩家改名、附魔、堆叠都不会失效，也无法伪造
- **完整的管理命令**：发放 / 扣除 / 查看配置 / 热重载，带 Tab 补全与权限节点
- **中文友好**：所有文案走 MiniMessage（支持渐变、颜色），配置与日志使用 UTF-8

## 环境要求

| 组件 | 版本 | 必需 |
| --- | --- | --- |
| 服务端 | Paper 1.21.11（或同 API 版本的分支） | ✅ |
| Java | 21 | ✅ |
| Vault | 1.7+ | ✅（使用 `/exchange` 必需） |
| 经济插件 | EssentialsX / CMI / 其他 Vault 兼容插件 | ✅（同上） |

> 只有 Vault 没有经济插件时，插件照常启动，但 `/exchange` 会提示「没有可用的经济系统」。

## 安装

（Rocky Linux / 纯 IPv6 服务器的一步步操作见 [docs/deploy-linux.md](docs/deploy-linux.md)）

1. 从 [Releases](https://github.com/lzqkotony/cogito-plugins/releases) 下载最新的 `Cogito-x.y.z.jar`（也可以按下方说明自行构建）
2. 放进服务端的 `plugins/` 目录
3. 确保服务端已安装 **Vault + 经济插件**
4. 重启服务端（或执行 `/reload confirm`）
5. 看到下面这些日志就说明装好了：

```
[Cogito] 已启用：脑啡肽物品 = EMERALD（标签 cogito:item）
[Cogito] Vault 经济已连接：EssentialsX Economy
```

首次启动会生成 `plugins/Cogito/config.yml`（经济、箱子界面、其它）、`plugins/Cogito/items.yml`（基础物品）和 `plugins/Cogito/ego.yml`（E.G.O. 套装），改完用 `/cogito reload` 热重载，不用重启。

## 命令与权限

| 命令 | 说明 | 权限 | 默认 |
| --- | --- | --- | --- |
| `/enkephalin`（`/enk`） | 查看自己持有的脑啡肽数量 | `cogito.use` | 所有人 |
| `/cogito gui`（`/cog`、`/box`） | 打开脑啡肽箱子（GUI 菜单） | `cogito.use` | 所有人 |
| `/enkephalin box` | 同上，打开箱子界面 | `cogito.use` | 所有人 |
| `/exchange [数量]`（`/exch`） | 用钱兑换脑啡肽，单向不可逆 | `cogito.use` | 所有人 |
| `/enkephalin give <玩家> <数量>` | 发放脑啡肽 | `cogito.admin` | OP |
| `/enkephalin take <玩家> <数量>` | 扣除脑啡肽 | `cogito.admin` | OP |
| `/enkephalin info` | 查看当前物品与经济配置 | `cogito.admin` | OP |
| `/enkephalin reload` | 重载 `config.yml` | `cogito.admin` | OP |
| `/cogito reload` | 同上，管理员重载配置 | `cogito.admin` | OP |
| `/cogito items` | 列出所有已注册物品 | `cogito.admin` | OP |
| `/cogito give <物品id> <玩家> <数量>` | 发放注册物品（`pe` / `pe-module` / `cogito` …） | `cogito.admin` | OP |
| `/cogito give ego <玩家> <套装id> <数量>` | 发放一整套 E.G.O.（防具、武器与饰品） | `cogito.admin` | OP |
| `/cogito give blueprint <玩家> <套装id> [数量]` | 发放某套 E.G.O. 的专属蓝图（装载时消耗 1 张） | `cogito.admin` | OP |
| `/cogito debug <物品id>` | 造一个物品并打印材质、NBT、识别自检结果 | `cogito.admin` | OP |
| `/cogito debug ego <玩家>` | 查看套装、防具件数 `y`、抗性 `x` 与最终倍率 `r` | `cogito.admin` | OP |
| `/cogito debug attack <red\|blue> <攻击者> <目标> <伤害>` | 自检伤害通道与蓝伤来源限制 | `cogito.admin` | OP |
| `/cogito set Lv <玩家> <数字>` | 设置玩家等级 | `cogito.admin` | OP |
| `/cogito reset player_information <玩家>` | 重置该玩家数据 | `cogito.admin` | OP |
| `/cogito data <玩家>` | 查看玩家数据（在线/离线均可） | `cogito.admin` | OP |
| `/cogito debug db` | 数据库读写自检（写入 → 读回 → 删除） | `cogito.admin` | OP |

## 配置

`config.yml`（经济与界面）：

```yaml
economy:
  price-per-enkephalin: 100        # 每个脑啡肽需要多少「钱」
  max-per-exchange: 64             # 单次兑换上限

gui:
  enabled: true                    # 关掉后 /cogito gui 会提示已关闭
  title: "<gradient:#22d3a8:#3b82f6>脑啡肽箱子</gradient>"
  rows: 5                          # 界面固定按 5 行布局，写小会自动按 5 行处理
  exchange-amounts: [1, 8, 64]     # 三个兑换按钮的档位，超过 max-per-exchange 会被截断

debug: false
join-message: ""                   # 玩家进服提示，留空则不发送
```

`items.yml`（物品注册表，节选）：

| id | 名称 | 原型材质 | 说明 |
| --- | --- | --- | --- |
| `pe` | 脑啡肽 | 绿宝石 | 基础货币，别名 `PE` / `enkephalin` |
| `pe-module` | 脑啡肽模块 | 绿宝石块 | 提取异想体所必备，别名 `PE-BOX` |
| `cogito` | Cogito | 跳跃药水 | 堆叠上限已改成 64 |
| `ego-blueprint` | E.G.O 研发图纸 | 纸 | 后续 E.G.O 开发消耗 |
| `golden-bough` | 金枝 | 枯木 + `cogito:golden_bough` 模型 | 脑叶公司奇点的核心浓缩 |

每个物品都能配：`material`、`display-name`、`lore`、`potion-type`、`enchantment`、`glint`、`custom-model-data`、`stack-size`、`stackable`、`placeable`、`craftable`、`aliases`、`legacy-tags`。

`ego.yml`（E.G.O. 套装，节选）：

```yaml
sets:
  paradise-lost:
    display-name: "<gradient:#facc15:#a855f7>失乐园</gradient>"
    armor-resistance: 0.1       # 统一 x，不限制代码范围
    armor:
      helmet:     { material: NETHERITE_HELMET,     enchantable: false, unbreakable: true, remove-vanilla-attributes: true }
      chestplate: { material: NETHERITE_CHESTPLATE, enchantable: false, unbreakable: true, remove-vanilla-attributes: true }
      leggings:   { material: NETHERITE_LEGGINGS,   enchantable: false, unbreakable: true, remove-vanilla-attributes: true }
      boots:      { material: NETHERITE_BOOTS,      enchantable: false, unbreakable: true, remove-vanilla-attributes: true }
    weapons:
      blue-sword:
        material: NETHERITE_SWORD
        attack-channel: BLUE   # 蓝伤按最大生命值百分比结算
      red-sword:
        material: NETHERITE_SWORD
        attack-channel: RED
```

抗性公式：

```text
y = 同一套有效 E.G.O. 防具件数（0~4，武器与饰品不计）
r = 1 - (1 - x) * y / 4
最终伤害 = 当前伤害 * r
```

混入原版防具或另一套 E.G.O. 防具时，整套抗性失效并统一按 `r=1`。计算全程使用 `double`，不主动取整。

### 箱子 GUI 布局（5 行 45 格）

| 槽位 | 内容 |
| --- | --- |
| 11 / 15 / 21 | 兑换按钮 ×1 / ×8 / ×64（档位来自 `gui.exchange-amounts`） |
| 13 | 脑啡肽信息：当前持有量、兑换单价、单次上限 |
| 23 | 我的余额：经济插件名、余额、还能兑换几个，点击刷新 |
| 30 | 命令帮助（点击后关掉箱子，在聊天栏列出命令） |
| 32 / 34 | 管理：配置信息 / 管理：重载配置（仅 `cogito.admin` 可见） |
| 40 | 关闭 |
| 其余 | 黑色玻璃板边框 |

菜单里的物品都是「幽灵物品」：点击会被取消，拿不走、拖不动，也没法用 shift 搬进背包
（不做这一步的话，shift 点击会把真物品塞进菜单，关掉界面就丢了）。

> 物品身份由 NBT 标签 `cogito:item` 决定。老版本用过的标签写进 `legacy-tags`（脑啡肽已写 `enkephalin`），服务器里的老物品照样能被识别，不会因为改版变废纸。

## 资源包（金枝自定义外观）

- Java 资源包：[Cogito-Resources-JE.zip](https://github.com/lzqkotony/download/releases/download/v1.0.1/Cogito-Resources-JE.zip)
- Bedrock / Geyser：[Cogito-Resources-BE.mcpack](https://github.com/lzqkotony/download/releases/download/v1.0.1/Cogito-Resources-BE.zip)
- 国内 GitHub 代理：在 GitHub 下载链接前加 `https://v4.gh-proxy.com/`
- Java 版通过 `minecraft:item_model` 使用 `cogito:golden_bough`；Bedrock 版通过 Geyser `custom_mappings` 映射同名模型

资源包仓库：[lzqkotony/download](https://github.com/lzqkotony/download)

详细配置见 [docs/resource-pack.md](docs/resource-pack.md)。

## 从源码构建

需要 JDK 21 与 Maven：

```bash
mvn clean package                       # 产物：target/Cogito-0.5.17.jar
bash build.sh                           # 同上，Linux / macOS / WSL 友好
bash build.sh -d /opt/paper/plugins     # 构建后直接拷贝到服务端 plugins 目录
```

## 项目结构

```
src/main/java/com/seewo/cogito/
├── CogitoPlugin.java               # 主类：生命周期、命令注册、配置重载
├── item/ItemRegistry.java          # 物品注册表：读取 items.yml
├── item/CustomItem.java            # 单个物品定义：造物 / 识别 / 堆叠上限
├── item/EnkephalinItem.java        # 脑啡肽在命令与 GUI 层的入口
├── ego/EgoRegistry.java            # 读取 ego.yml、识别套装并计算件数/倍率
├── ego/EgoDamageService.java       # 红伤 / 蓝伤统一伤害入口
├── economy/VaultHook.java          # Vault 经济对接
├── command/EnkephalinCommand.java  # /enkephalin
├── command/ExchangeCommand.java    # /exchange
├── command/CogitoCommand.java      # /cogito（gui / give / debug / 数据管理 / reload）
├── gui/Menu.java  gui/MenuListener.java  gui/EnkephalinMenu.java   # 箱子界面
├── listener/ItemBehaviourListener.java  # 拦截放置、合成与 Cogito 饮用
├── listener/EgoDamageListener.java      # E.G.O. 统一抗性与神圣黄盾
├── listener/EgoAcquisitionListener.java # 受限 E.G.O. 的获取、拾取与容器白名单拦截
├── listener/MimicService.java           # 拟态三阶段、模仿层与右键技能
├── listener/ParadiseLostService.java    # 失乐园武器、特殊横扫与 12 使徒召唤
├── listener/EgoEnchantListener.java     # 阻止 E.G.O. 附魔
├── listener/PlayerJoinListener.java
└── text/Messages.java              # MiniMessage 文本出口
```

## 玩法路线图

完整玩法设计见 [docs/design.md](docs/design.md)。

| 版本 | 内容 | 状态 |
| --- | --- | --- |
| v0.1 | 脑啡肽实体物品 + Vault 兑换 + 管理命令 | ✅ 已发布 0.1 Beta |
| v0.2 | 箱子 GUI + 物品注册表（脑啡肽/模块/Cogito/图纸/金枝） | ✅ 已完成（未发 Release） |
| v0.3 | 玩家数据层 core data（SQLite：等级 / 镇压次数 / E.G.O 解锁） | ✅ 已发布 0.3.0-Beta |
| v0.4 | E.G.O. 红/蓝伤害通道、统一防具抗性、无限耐久与不可附魔；修复 Cogito 可饮用 | ✅ 已发布 0.4.0-Beta |
| v0.5 | E.G.O. 开发台（自定义方块）+ 专属图纸解锁 + 研发流程 | ✅ 已发布 0.5.0-Beta |
| v0.5.1 | 一次性蓝图装载、四档定向研发、护甲/武器分类、正义裁决者、套装动态属性 | ✅ 已发布 0.5.1 |
| v0.5.2 | 正义裁决者统一命名、PALE 蓝伤百分比、满蓄力挥剑触发 8~10 段范围伤害、修正套装攻速叠加 | ✅ 已发布 0.5.2 |
| v0.5.3 | 改用手臂挥动事件触发正义裁决者、服主剑左键击杀、未装载蓝图装备仍显示、正义裁决者全套铁甲+钻石海岸纹饰 | ✅ 已发布 0.5.3 |
| v0.5.4 | 正义裁决者简化为单段固定 5~6 蓝伤、4.0 攻速、6 格距离；服主套改为钻石甲+末地纹饰+金锭装饰 | ✅ 已发布 0.5.4 |
| v0.5.5 | 金枝改为枯木底材 + `item_model` 自定义模型，接入 Java/Bedrock 双端资源包和 GitHub 代理下载 | ✅ 已发布 0.5.5 |
| v0.5.6 | 正义裁决者灵魂攻击机制：5 段普通、40% 特殊攻击、4 格距离、40 tick 硬冷却 | ✅ 已发布 0.5.6 |
| v0.5.7 | 灵魂攻击改为 2 秒时间轴多段结算，范围改为前方 4 / 宽 1 / 高 4 | ✅ 已发布 0.5.7 |
| v0.5.8 | 负抗性改为伤害反转治疗：服主 `x=-10` 可免疫并回血 | ✅ 已发布 0.5.8 |
| v0.5.9 | 失乐园套装、神圣黄盾、铁锄武器、20 秒特殊横扫与 12 使徒召唤 | ✅ 已发布 0.5.9 |
| v0.5.10 | 神圣黄盾改用精确保留半颗心精度的原版吸收值显示 | ✅ 已发布 0.5.10 |
| v0.5.11 | 免伤改判 `x=0.1` 后 `<5`；召唤者与使徒悬空 6 秒，并增加聊天栏台词 | ✅ 已发布 0.5.11 |
| v0.5.12 | 失乐园改为 `±8` 立方体范围，排除本人与使徒；使徒不攻击召唤者，右键不再锄地 | ✅ 已发布 0.5.12 |
| v0.5.13 | 失乐园改为结算前免疫单次原始伤害 `≤5`，超过后才计算抗性与 `x=0.1` | ✅ 已发布 0.5.13 |
| v0.5.14 | 允许鞘翅占用胸甲槽混搭；不影响套装识别，但不计入 `y`；增加黄盾 ActionBar | ✅ 已发布 0.5.14 |
| v0.5.15 | 黄盾首次生成立即给 1 点，并在每次扣盾前重新校验 Y=4 | ✅ 已发布 0.5.15 |
| v0.5.16 | 删除失乐园单次伤害免疫；所有伤害统一进入抗性、`x=0.1` 与黄盾流程 | ✅ 已发布 0.5.16 |
| v0.5.17 | 新增拟态 E.G.O.：三阶段、模仿层数、右键单体/范围技能、红皮革金纹护甲 | ✅ 本版 |
| v0.5.x | 同一功能线的修复与小功能：E.G.O. 使用代价、共鸣、饰品四部位…… | 🚧 下一个 0.5.18 起 |
| v0.6 | 异想体定义与镇压产出（ALEPH 100~60 / WAW 60~30 / HE 30~20 / TETH 20~10 / ZAYIN 10~8） | 📋 计划中 |
| v0.7 | 随机 E.G.O. 开发（抽卡与卡池）、提取探索 | 📋 计划中 |

> **版本号约定**：同一功能线内只递增补丁号（0.5.1、0.5.2…），开新功能线才升 minor。

## 常见问题

**`/exchange` 提示没有可用的经济系统？**
服务端缺 Vault 或经济插件。装好 Vault 与 EssentialsX / CMI 后重启即可。

**兑换可以反悔吗？**
不能，设计上就是**单向**的：钱 → 脑啡肽。反向不提供（管理员可用 `/enkephalin take` 手动收回）。

**背包满了怎么办？**
装不下的部分会掉在玩家脚下，并给出提示。

**玩家改名或合并物品会弄丢脑啡肽吗？**
不会。识别只认 NBT 标签，不认名称或 Lore，玩家也无法用铁砧伪造。

**中文显示乱码？**
服务端启动参数加 `-Dfile.encoding=UTF-8`，并保证控制台/终端使用 UTF-8 编码。

## 许可证

本项目采用 **GNU General Public License v3.0**，全文见 [LICENSE](LICENSE)。

## 免责声明

本项目为第三方插件，与 Mojang Studios、Microsoft、PaperMC 均无关联。“Minecraft”是 Mojang Studios 的商标。
