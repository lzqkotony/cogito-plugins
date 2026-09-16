# 部署到 Linux 服务器（Rocky Linux / 纯 IPv6 示例）

以作者的服务器为例：`mc.ezmscc.dpdns.org`（纯 IPv6，Rocky Linux）。其他发行版同理，把 `dnf` 换成对应的包管理器即可。

## 一、服务器前置条件

| 需要 | 说明 |
| --- | --- |
| Java 21 | Paper 1.21.11 要求（`sudo dnf install -y java-21-openjdk`） |
| Vault | 经济接口，插件靠它读写玩家货币 |
| 经济插件 | EssentialsX / CMI 等任意 Vault 兼容插件 |
| 插件 jar | 见第二节 |

只装 Vault 不装经济插件时，插件照常启动，但 `/exchange` 会提示「没有可用的经济系统」。

## 二、拿到 jar

两种方式：

1. **从 Releases 下载**（推荐）：<https://github.com/lzqkotony/cogito-plugins/releases> —— Release 说明里附了 SHA256
2. **自己构建**：`mvn clean package` 或 `bash build.sh`，产物在 `target/Cogito-<版本>.jar`

## 三、上传（在 Windows 侧执行）

```powershell
# 域名走 IPv6 时加 -6
scp -6 "路径\Cogito-0.2.0-BETA.jar" 用户名@mc.ezmscc.dpdns.org:/服务器目录/plugins/

# 直接填 IPv6 字面量要用方括号
scp -6 .\Cogito-0.2.0-BETA.jar 用户名@[2400:xxxx:xxxx::1]:/服务器目录/plugins/
```

服务器侧也可以反向拉取：

```bash
rsync -6 -av 用户名@你的电脑:/path/Cogito-0.2.0-BETA.jar /服务器目录/plugins/
```

上传后在服务器上校验：

```bash
sha256sum /服务器目录/plugins/Cogito-0.2.0-BETA.jar    # 与 Release 页面上的 SHA256 对照
```

## 四、权限与生效

```bash
sudo chown mc:mc /服务器目录/plugins/Cogito-0.2.0-BETA.jar   # 属主改成跑服务端的用户
sudo chmod 644   /服务器目录/plugins/Cogito-0.2.0-BETA.jar

sudo systemctl restart paper        # systemd 管理的话
# 或者进服务端控制台：stop 后重启；想热重载用 reload confirm（对多数插件不算安全）
```

## 五、确认装好了

启动日志应该出现这三行：

```
[Cogito] 已注册 5 个物品：pe, pe-module, cogito, ego-blueprint, golden-bough
[Cogito] 已启用：脑啡肽物品 = EMERALD（标签 cogito:item）
[Cogito] Vault 经济已连接：EssentialsX Economy
```

如果出现 `未找到 Vault 经济（需要 Vault + 一个经济插件）`，说明服务端缺 Vault 或经济插件。

## 六、进服自测流程

```
/cogito items                       # 列出所有注册物品（OP）
/cogito debug pe                    # 造一个脑啡肽并打印材质/NBT/识别结果（OP，不用客户端也能验证）
/eco give <你的名字> 10000           # 给钱（EssentialsX）
/exchange 10                        # 花 1000 换 10 个脑啡肽
/enkephalin                         # 查看持有量
/cogito gui                         # 打开箱子界面，点按钮兑换
```

管理员命令：`/cogito give <物品id> <玩家> <数量>`、`/enkephalin give|take <玩家> <数量>`、`/cogito reload`。

## 七、配置文件

首次启动会在 `plugins/Cogito/` 生成两个文件：

- `config.yml` —— 经济（单价、单次上限）、箱子界面（开关、标题、行数、按钮档位）、进服提示、debug
- `items.yml` —— 物品注册表（脑啡肽、脑啡肽模块、Cogito、E.G.O 研发图纸、金枝的材质/名称/Lore/堆叠/能否放置与合成）

改完在游戏里执行 `/cogito reload` 即可热重载，不用重启服务器。

## 八、IPv6 相关的几点

- 插件本身不含任何网络代码，纯 IPv6 环境不影响它。
- 服务端要能收 IPv6 连接：`server.properties` 里 `server-ip=` 留空即监听所有网卡；想显式只绑 IPv6 可写 `server-ip=::`。
- 客户端所在网络也要支持 IPv6，否则连不上。

## 九、可选：在服务器上直接构建

```bash
sudo dnf install -y java-21-openjdk-devel maven
cd /你的项目目录 && bash build.sh -d /服务器目录/plugins
```

注意：纯 IPv6 的机器访问 Maven Central / JitPack 不一定通（这些仓库不一定提供 IPv6），
所以更稳的做法是**在开发机上构建好，再上传 jar**。
