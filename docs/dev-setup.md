# 开发环境搭建（Linux / WSL）

作者的开发环境有两个：Windows（用 `dev/build.ps1` + `tools/ship.ps1`）和 **Arch Linux（含这台机器的 Arch WSL）**。
这份文档记录 Linux 侧的完整搭法，换机器照抄即可。

## 一、装依赖（Arch Linux）

```bash
sudo pacman -S --needed git jdk21-openjdk maven nodejs github-cli openssh
```

其他发行版对应包名：`openjdk-21-jdk` / `maven` / `nodejs` / `gh` / `openssh`。
**JDK 必须是 21**（Paper 1.21.11 的要求）；Maven 用发行版自带的 3.9.x 即可。

## 二、拿到仓库

```bash
# 放到 Linux 自己的文件系统里（不要放 /mnt/c，构建会明显变慢）
git clone git@github.com:lzqkotony/cogito-plugins.git ~/cogito-plugins
cd ~/cogito-plugins
git config user.name  lzqkotony
git config user.email "133836840+lzqkotony@users.noreply.github.com"
```

### SSH 认证

```bash
ssh-keygen -t ed25519 -C "你的备注" -f ~/.ssh/id_ed25519_github -N ''
cat ~/.ssh/id_ed25519_github.pub      # 把这行贴到 https://github.com/settings/keys

cat >> ~/.ssh/config <<'EOF'
Host github.com
  HostName ssh.github.com
  Port 443
  User git
  IdentityFile ~/.ssh/id_ed25519_github
  IdentitiesOnly yes
  StrictHostKeyChecking accept-new
EOF
chmod 600 ~/.ssh/config
ssh -T git@github.com                 # 看到 Hi lzqkotony! 就成了
```

> 走 `ssh.github.com:443` 是为了绕开部分网络对 22 端口的拦截，纯 IPv6 环境同样适用。

### gh（发 Release 要用）

```bash
gh auth login --hostname github.com --git-protocol ssh --web --skip-ssh-key
```

会在终端打印一个设备码，浏览器打开 <https://github.com/login/device> 输入即可（**不会弹出任何窗口**）。
已有机器上登录过的话，也可以直接把 token 写进 `~/.config/gh/hosts.yml`：

```yaml
github.com:
    oauth_token: gho_xxxxxxxx
    git_protocol: ssh
    user: 你的用户名
```

## 三、构建与验证

```bash
mvn -B -ntp clean package      # 产物 target/Cogito-<版本>.jar
bash build.sh                  # 同上（等价封装）
bash build.sh -d /opt/paper/plugins   # 构建后直接拷到服务端
```

首次构建会从 Maven Central / repo.papermc.io / jitpack 拉依赖，约两三分钟；之后是秒级。

> ⚠️ 同一份代码在 Windows 与 Linux 构建出的 jar **SHA256 不同**（jar 里含时间戳），
> 所以校验一律以"发布那一次构建 + 它附带的 .sha256"为准。

## 四、日常一轮的发布流程

```bash
bash tools/ship.sh --version 0.3.0-BETA --message "异想体镇压产出"
```

它会依次：导出聊天记录 → 改 pom 版本号并构建 → 提交推送 main → 打 tag → （加 `--release` 时）发 Release。
加 `--dry-run` 可以先演练。

## 五、WSL 特有的两个坑

1. **`$HOME` 与 Windows 不通用**：脚本默认在 `~/.codex/sessions/` 找会话文件，WSL 里这是 Linux 家目录。
   要导出 Windows 那边的会话，显式传路径：

   ```bash
   bash tools/ship.sh --version 0.3.0-BETA --message "..." \
     --session /mnt/c/Users/<Windows用户名>/.codex/sessions/2026/09/15/rollout-*.jsonl
   ```

   如果 Codex 就跑在 WSL 里（推荐），`~/.codex/sessions/` 自然就是对的，不用管这一条。

2. **别在 `/mnt/c` 下构建**：9p 文件系统慢且行尾/权限容易出问题，仓库放 `~/` 下。

## 六、本机现状（2026-09-16）

这台机器的 Arch WSL 已经配好：JDK 21.0.12.1、Maven 3.9.16、Node v26.8.2、gh 2.101.0、OpenSSH 10.5p1，
仓库在 `~/cogito-plugins`，SSH 与 gh 均已认证，`mvn clean package` 与 `bash tools/ship.sh --dry-run` 都验证通过。
