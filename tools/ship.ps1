<#
    Cogito 一键发布流程

    做五件事：
      1. 导出本轮聊天记录到 chat/
      2. 改 pom.xml 版本号并构建（mvn clean package）
      3. git add / commit / push 到 main
      4. 打标签 v<版本> 并推送
      5. 发 GitHub Release（jar + sha256 + 说明），需要 -Release

    用法：
      # 只做 1~4（不发 Release）
      powershell -ExecutionPolicy Bypass -File tools\ship.ps1 -Version 0.3.0-BETA -Message "异想体镇压产出"

      # 完整发布，并只导出某个时间点之后的聊天记录
      powershell -ExecutionPolicy Bypass -File tools\ship.ps1 `
        -Version 0.3.0-BETA -Message "异想体镇压产出" `
        -Since 2026-09-16T02:00:00Z -NotesFile ..\notes-0.3.0-BETA.md -Release

      # 只想看看会做什么
      powershell -ExecutionPolicy Bypass -File tools\ship.ps1 -Version 0.3.0-BETA -Message "试跑" -DryRun

    提醒：本脚本不会弹出任何窗口；聊天记录里的原始 jsonl 不会被提交（只提交转好的 md）。
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Version,
    [Parameter(Mandatory = $true)][string]$Message,
    [string]$Session,        # 会话 jsonl，默认取最近修改的那个
    [string]$Since,          # 只导出该时间点之后（UTC，如 2026-09-16T02:00:00Z）
    [string]$NotesFile,      # Release 说明 md
    [string]$TranscriptName, # 记录文件名，默认 transcript-<日期>-<版本>.md
    [switch]$Release,
    [switch]$SkipTranscript,
    [switch]$SkipBuild,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

function Step($text) { Write-Host "==> $text" -ForegroundColor Cyan }
function Info($text) { Write-Host "    $text" -ForegroundColor Gray }

$transcriptName = if ($TranscriptName) { $TranscriptName } else { "transcript-$(Get-Date -Format 'yyyy-MM-dd')-$Version.md" }
$transcriptPath = Join-Path $repo "chat\$transcriptName"
$jarPath = Join-Path $repo "target\Cogito-$Version.jar"

# ---------------------------------------------------------------- 1. 聊天记录
Step "① 导出聊天记录"
if ($SkipTranscript) {
    Info "已跳过（-SkipTranscript）"
} else {
    if (-not $Session) {
        $sessionDir = Join-Path $env:USERPROFILE '.codex\sessions'
        $Session = Get-ChildItem $sessionDir -Recurse -Filter 'rollout-*.jsonl' -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1 -ExpandProperty FullName
    }
    if (-not $Session) { throw "没找到会话文件，请用 -Session 指定" }
    Info "会话文件：$Session"

    $nodeArgs = @('tools/export-transcript.js', $Session, $transcriptPath, '--title', "Cogito 项目 · $Version 会话记录")
    if ($Since) { $nodeArgs += @('--since', $Since) }
    if ($DryRun) {
        Info "[dry-run] node $($nodeArgs -join ' ')"
    } else {
        & node @nodeArgs
        Info "已写出 $transcriptName"
    }
}

# ------------------------------------------------------------------ 2. 版本号
Step "② 更新 pom.xml 版本号并构建"
$pom = Join-Path $repo 'pom.xml'
$pomText = [System.IO.File]::ReadAllText($pom)
$newPom = [regex]::Replace($pomText, '(<artifactId>cogito</artifactId>\s*<version>)[^<]+(</version>)', "`${1}$Version`${2}")
if ($newPom -eq $pomText) {
    Info "版本号已经是 $Version（或没匹配到），跳过改写"
} elseif ($DryRun) {
    Info "[dry-run] 把 pom.xml 版本改成 $Version"
} else {
    [System.IO.File]::WriteAllText($pom, $newPom, (New-Object System.Text.UTF8Encoding($false)))
    Info "pom.xml 版本 -> $Version"
}

if ($SkipBuild) {
    Info "已跳过构建（-SkipBuild）"
} elseif ($DryRun) {
    Info "[dry-run] mvn -B -ntp clean package"
} else {
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        throw "找不到 mvn，请先装 Maven（或先运行开发机上的环境激活脚本）"
    }
    & mvn -B -ntp clean package
    if ($LASTEXITCODE -ne 0) { throw "构建失败（退出码 $LASTEXITCODE）" }
    if (-not (Test-Path $jarPath)) { throw "没有找到构建产物：$jarPath" }
    $sha = (Get-FileHash $jarPath -Algorithm SHA256).Hash.ToLower()
    "$sha *Cogito-$Version.jar" | Set-Content -Encoding ASCII (Join-Path $repo "dist\Cogito-$Version.jar.sha256") -ErrorAction SilentlyContinue
    Info "产物：target\Cogito-$Version.jar（$((Get-Item $jarPath).Length) 字节）"
    Info "SHA256：$sha"
}

# --------------------------------------------------------------- 3. 提交推送
Step "③ 提交并推送到 main"
$changes = git status --porcelain
if (-not $changes) {
    Info "没有改动需要提交"
} elseif ($DryRun) {
    Info "[dry-run] git add -A && git commit -m ... && git push origin main"
} else {
    git add -A
    git commit -q -m "$Message"
    git push origin main
    Info "已推送：$(git log --oneline -1)"
}

# ------------------------------------------------------------------ 4. 标签
Step "④ 打标签 v$Version"
if ($DryRun) {
    Info "[dry-run] git tag -a v$Version && git push origin v$Version"
} elseif (git tag -l "v$Version") {
    Info "标签 v$Version 已存在，跳过"
} else {
    git tag -a "v$Version" -m "Cogito $Version"
    git push origin "v$Version"
    Info "标签已推送"
}

# -------------------------------------------------------------- 5. Release
if (-not $Release) {
    Step "⑤ 跳过 Release（未加 -Release）"
} else {
    Step "⑤ 发 GitHub Release"
    $assets = @($jarPath)
    $shaFile = Join-Path $repo "dist\Cogito-$Version.jar.sha256"
    if ($DryRun) {
        Info "[dry-run] $gh release create v$Version <jar> <sha256> --title 'Cogito $Version' --notes-file $NotesFile --prerelease"
    } else {
        $gh = (Get-Command gh -ErrorAction SilentlyContinue).Source
        if (-not $gh) {
            $fallback = Join-Path $env:USERPROFILE 'Documents\Codex\2026-09-15\g\tools\gh\bin\gh.exe'
            if (Test-Path $fallback) { $gh = $fallback }
        }
        if (-not $gh) { throw "找不到 gh，请安装 GitHub CLI 或改成本地路径" }

        if (-not (Test-Path $shaFile)) {
            $sha = (Get-FileHash $jarPath -Algorithm SHA256).Hash.ToLower()
            "$sha *Cogito-$Version.jar" | Set-Content -Encoding ASCII $shaFile
        }
        $assets += $shaFile
        $releaseArgs = @('release', 'create', "v$Version") + $assets + @('--title', "Cogito $Version", '--prerelease')
        if ($NotesFile) { $releaseArgs += @('--notes-file', $NotesFile) }
        & $gh @releaseArgs
    }
}

Write-Host ''
Write-Host "完成：Cogito $Version" -ForegroundColor Green
if ($DryRun) { Write-Host '（这是 dry-run，什么都没改）' -ForegroundColor Yellow }
