# Cogito

Minecraft **Paper 1.21.11** 服务端插件 —— 脑叶服务器核心玩法系统。当前版本实现「脑啡肽」经济层。

- 目标服务端：Paper 1.21.11（Java 21）
- 依赖：Vault + 任意 Vault 兼容经济插件（EssentialsX / CMI 等）
- 玩法设计见 [docs/design.md](docs/design.md)

## 已实现（v0.1）

- **脑啡肽实体物品**：带 PersistentDataContainer 标签的附魔绿宝石块（材质、名字、Lore、附魔、CustomModelData 全部走配置）
- **Vault 经济兑换**：`/exchange <数量>`，100 元换 1 个脑啡肽，单向不可逆
- **管理命令**：发放 / 扣除 / 查看物品配置 / 重载配置
- **玩家持有量查询**：`/enkephalin`

## 结构

```
cogito/
├── pom.xml                                       # paper-api 1.21.11 + VaultAPI + Java 21 + shade
└── src/main
    ├── java/com/seewo/cogito
    │   ├── CogitoPlugin.java                     # 主类：生命周期、命令与监听器注册、配置重载
    │   ├── item/EnkephalinItem.java              # 脑啡肽物品工厂（造物 / 识别 / 统计 / 扣除 / 发放）
    │   ├── economy/VaultHook.java                # Vault 经济封装
    │   ├── command/EnkephalinCommand.java        # /enkephalin [give|take|info|reload]
    │   ├── command/ExchangeCommand.java          # /exchange <数量>
    │   ├── listener/PlayerJoinListener.java      # 进服提示（配置留空即关闭）
    │   └── text/Messages.java                    # MiniMessage 文本出口
    └── resources
        ├── plugin.yml                            # 元信息、命令、权限、softdepend: Vault
        └── config.yml                            # 物品定义 + 兑换价格
```

## 命令

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/enkephalin` | 查看自己持有的脑啡肽数量 | 所有人 |
| `/exchange <数量>` | 用钱兑换脑啡肽（单向） | 所有人 |
| `/enkephalin give <玩家> <数量>` | 发放脑啡肽 | `cogito.admin` |
| `/enkephalin take <玩家> <数量>` | 扣除脑啡肽 | `cogito.admin` |
| `/enkephalin info` | 查看当前物品/经济配置 | `cogito.admin` |
| `/enkephalin reload` | 重载 config.yml | `cogito.admin` |

## 构建与测试

```powershell
# 只编译打包（产物：target\Cogito-0.1.0-SNAPSHOT.jar）
mvn package

# 或者用封装脚本：打包 + 打印路径与 SHA256
C:\Users\seewo\Documents\Codex\2026-09-15\g\dev\build.ps1

# 打包后自动拷到你服务器的 plugins 目录（本地路径 / 映射盘 / 同步目录都行）
C:\Users\seewo\Documents\Codex\2026-09-15\g\dev\build.ps1 -DeployTo D:\paper\plugins
```

## 部署到服务器

（Rocky Linux / 纯 IPv6 服务器的逐步操作见 `outputs\Cogito-部署到Linux服务器.md`）

1. 把 `Cogito-0.1.0-SNAPSHOT.jar` 放进服务器的 `plugins` 目录
2. 服务器上要装 **Vault** + 一个经济插件（EssentialsX / CMI 等），否则 `/exchange` 会提示没有经济系统
3. 重启服务器，或执行 `/reload confirm`
4. 首次启动会生成 `plugins/Cogito/config.yml`，改完用 `/enkephalin reload` 热重载

## 后续（见 outputs/脑叶玩法系统-设计稿.md）

- v0.2：异想体定义与镇压产出（ALEPH 100~60 / WAW 60~30 / HE 30~20 / TETH 20~10 / ZAYIN 10~8）
- v0.3：E.G.O 定向开发（镇压 5 次解锁，50% / 25% / 0% 失败率三档）+ 随机开发抽卡
