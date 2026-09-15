# Cogito

> 脑叶服务器核心玩法插件 —— Minecraft **Paper 1.21.11**
>
> A Paper plugin for the "Brain Leaf" style server gameplay: an **Enkephalin** item economy wired to **Vault**.

[![License](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
[![Paper](https://img.shields.io/badge/Paper-1.21.11-orange.svg)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21-red.svg)](https://adoptium.net)

Cogito 把「脑叶」玩法的第一层——**脑啡肽**——做成了一套可用的经济系统：脑啡肽是实体物品，玩家通过镇压异想体、提取探索、用钱兑换获得，用于后续的 E.G.O 开发。

---

## 功能

- **脑啡肽实体物品**：带 `PersistentDataContainer` NBT 标签的附魔绿宝石块（材质、名称、Lore、附魔、光效、CustomModelData 全部走配置）
- **Vault 经济兑换**：`/exchange` 用服务器货币按 100:1 单向兑换脑啡肽（价格可配）
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

1. 下载 `Cogito-x.y.z.jar`（Releases 页面，或用下方命令自行构建）
2. 放进服务端的 `plugins/` 目录
3. 确保服务端已安装 **Vault + 经济插件**
4. 重启服务端（或执行 `/reload confirm`）
5. 看到这两行日志就说明装好了：

```
[Cogito] 已启用：脑啡肽物品 = EMERALD_BLOCK（标签 enkephalin）
[Cogito] Vault 经济已连接：EssentialsX Economy
```

首次启动会生成 `plugins/Cogito/config.yml`，改完用 `/enkephalin reload` 热重载，不用重启。

## 命令与权限

| 命令 | 说明 | 权限 | 默认 |
| --- | --- | --- | --- |
| `/enkephalin`（`/enk`） | 查看自己持有的脑啡肽数量 | `cogito.use` | 所有人 |
| `/exchange [数量]`（`/exch`） | 用钱兑换脑啡肽，单向不可逆 | `cogito.use` | 所有人 |
| `/enkephalin give <玩家> <数量>` | 发放脑啡肽 | `cogito.admin` | OP |
| `/enkephalin take <玩家> <数量>` | 扣除脑啡肽 | `cogito.admin` | OP |
| `/enkephalin info` | 查看当前物品与经济配置 | `cogito.admin` | OP |
| `/enkephalin reload` | 重载 `config.yml` | `cogito.admin` | OP |

## 配置

```yaml
enkephalin:
  material: EMERALD_BLOCK          # 基础材质
  display-name: "<gradient:#22d3a8:#3b82f6>脑啡肽</gradient>"   # MiniMessage 格式
  lore:
    - "<gray>从异想体中提取出的能量结晶"
  enchantment: unbreaking          # 让物品发光的附魔
  enchantment-level: 1
  hide-enchants: true              # 隐藏附魔名，只留光效
  glint: true                      # 强制光效
  custom-model-data: 0             # 以后接材质包时填
  tag: enkephalin                  # NBT 标签：cogito:enkephalin

economy:
  price-per-enkephalin: 100        # 每个脑啡肽需要多少「钱」
  max-per-exchange: 64             # 单次兑换上限
```

> 物品身份由 NBT 标签 `cogito:enkephalin` 决定，**改名称/Lore 不会让已有脑啡肽失效**（`material` 保持不变即可）。

## 从源码构建

需要 JDK 21 与 Maven：

```bash
mvn clean package                       # 产物：target/Cogito-0.1.0-SNAPSHOT.jar
bash build.sh                           # 同上，Linux / macOS / WSL 友好
bash build.sh -d /opt/paper/plugins     # 构建后直接拷贝到服务端 plugins 目录
```

## 项目结构

```
src/main/java/com/seewo/cogito/
├── CogitoPlugin.java               # 主类：生命周期、命令注册、配置重载
├── item/EnkephalinItem.java        # 脑啡肽物品工厂：造物 / 识别 / 统计 / 扣除 / 发放
├── economy/VaultHook.java          # Vault 经济对接
├── command/EnkephalinCommand.java  # /enkephalin
├── command/ExchangeCommand.java    # /exchange
├── listener/PlayerJoinListener.java
└── text/Messages.java              # MiniMessage 文本出口
```

## 玩法路线图

完整玩法设计见 [docs/design.md](docs/design.md)。

| 版本 | 内容 | 状态 |
| --- | --- | --- |
| v0.1 | 脑啡肽实体物品 + Vault 兑换 + 管理命令 | ✅ 已完成 |
| v0.2 | 异想体定义与镇压产出（ALEPH 100~60 / WAW 60~30 / HE 30~20 / TETH 20~10 / ZAYIN 10~8） | 🚧 计划中 |
| v0.3 | E.G.O 定向开发（镇压 5 次解锁；普通 50% / 高级 25%（费用 +20%）/ 决断 0%（费用 +50%）失败率） | 📋 计划中 |
| v0.4 | 随机 E.G.O 开发（抽卡与卡池）、提取探索 | 📋 计划中 |

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
