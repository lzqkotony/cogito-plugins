# Cogito 双端资源包

## 下载

- Java：`Cogito-Resources-JE.zip`
- Bedrock/Geyser：`Cogito-Resources-BE.mcpack`

Release 仓库：[lzqkotony/download](https://github.com/lzqkotony/download/releases/tag/v1.0.1)

国内 GitHub 代理地址：

```text
https://v4.gh-proxy.com/https://github.com/lzqkotony/download/releases/download/v1.0.1/Cogito-Resources-JE.zip
https://v4.gh-proxy.com/https://raw.githubusercontent.com/lzqkotony/download/v1.0.1/dist/Cogito-Resources-BE.zip
```

## Java 配置

`server.properties`：

```properties
resource-pack=https://v4.gh-proxy.com/https://github.com/lzqkotony/download/releases/download/v1.0.1/Cogito-Resources-JE.zip
resource-pack-sha1=30637ec6a8dfbc9259c6587b5ce1278f8bed30ed
require-resource-pack=false
```

## Bedrock / Geyser 配置

1. 把 `geyser/custom_mappings/cogito_golden_bough.json` 放入：
   `plugins/Geyser-Spigot/custom_mappings/`
2. 在 `plugins/Geyser-Spigot/config.yml` 设置：

```yaml
gameplay:
  enable-custom-content: true

advanced:
  resource-pack-urls:
    - "https://v4.gh-proxy.com/https://raw.githubusercontent.com/lzqkotony/download/v1.0.1/dist/Cogito-Resources-BE.zip"
```

## 物品模型约定

- Java item model：`cogito:golden_bough`
- Bedrock identifier：`cogito:golden_bough`
- Java 基础物品：`minecraft:dead_bush`
- Geyser mapping input：`minecraft:dead_bush` + `cogito:golden_bough`
