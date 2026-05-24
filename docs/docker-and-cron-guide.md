# Docker 与定时任务 (Cron) 使用指南

## Part 1: Docker 到底干嘛用的？

### 为什么项目里有 Docker？

简单来说：**Docker 是用来一键帮您安装 MySQL 数据库的。**

我们项目默认用的是 H2 内存数据库（程序一关数据就没了）。但当您想用真正的 MySQL 数据库时，您有两个选择：

| 方案 | 操作 | 难度 |
|------|------|------|
| 手动安装 MySQL | 下载安装包 → 配置环境变量 → 设置密码 → 手动建库建表 | ⭐⭐⭐⭐ 麻烦 |
| **用 Docker** | 一行命令 `docker compose up -d` → 全部自动完成 | ⭐ 极简 |

### 前提条件：安装 Docker Desktop

1. 打开 https://www.docker.com/products/docker-desktop/
2. 下载 Mac 版 (Apple Silicon 芯片选 Apple Chip)
3. 安装并启动 Docker Desktop

验证安装成功：
```bash
docker --version
# 输出类似: Docker version 24.0.x, build xxxxxxx
```

### 使用方法

```bash
# 启动 MySQL (首次会自动下载镜像，约需 1-2 分钟)
cd docker
docker compose up -d

# 查看状态
docker compose ps
# 应该看到 batchlab-mysql 状态为 running

# 验证数据库可以连上
docker exec -it batchlab-mysql mysql -u batch_user -pbatch_pass batchlab_db -e "SHOW TABLES;"

# 停止 MySQL (数据保留)
docker compose down

# 停止并清空所有数据 (从零开始)
docker compose down -v
```

### 和我们的 Batch 怎么配合？

1. 启动 Docker MySQL: `cd docker && docker compose up -d`
2. 回到项目根目录运行 Batch: `./scripts/run-local.sh csvToDbJob mysql`
3. 这里第二个参数 `mysql` 告诉系统去读 `application-mysql.yml` 而不是 `application-local.yml`

---

## Part 2: 定时任务 (Cron) 在 MacBook 上怎么用？

### 什么是 Cron？

Cron 是 macOS 自带的定时任务调度工具。你可以设定"每天凌晨 1 点自动跑某个命令"，它就会在后台帮你执行。

### 核心问题：MacBook 锁屏/休眠后 Cron 还能跑吗？

**答案：不能。** 当 Mac 进入休眠状态后，cron 任务会被暂停。这是一个 macOS 的硬限制。

### 解决方案对比

| 方案 | 适用场景 | 难度 |
|------|---------|------|
| 方案 A: 阻止休眠 | 本地持续开发/测试 | ⭐ |
| 方案 B: launchd (推荐) | macOS 原生方案，唤醒后自动补跑 | ⭐⭐ |
| 方案 C: 服务器部署 | 生产级，永远在线 | ⭐⭐⭐⭐ |

---

### 方案 A: 阻止 Mac 休眠 (最简单，适合开发测试)

在终端执行：
```bash
# 阻止 Mac 休眠 (关掉这个终端窗口或按 Ctrl+C 就恢复正常)
caffeinate -s
```

然后在**另一个终端窗口**设置 cron 任务，Mac 就会保持清醒并按时执行。

---

### 方案 B: launchd (推荐，macOS 原生)

launchd 是 macOS 官方推荐的替代 cron 的方案。它有一个关键优势：**如果任务因为休眠错过了执行时间，Mac 醒来后会自动补跑！**

#### 设置步骤：

1. 创建一个 plist 配置文件：

```bash
nano ~/Library/LaunchAgents/com.batchlab.daily.plist
```

2. 粘贴以下内容 (已帮您写好)：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.batchlab.daily</string>

    <key>ProgramArguments</key>
    <array>
        <string>/Users/qujunsheng/Projects/batch-lab/scripts/run-job.sh</string>
        <string>dailyReportCompositeJob</string>
    </array>

    <key>StartCalendarInterval</key>
    <dict>
        <key>Hour</key>
        <integer>1</integer>
        <key>Minute</key>
        <integer>0</integer>
    </dict>

    <key>StandardOutPath</key>
    <string>/Users/qujunsheng/Projects/batch-lab/logs/launchd.log</string>
    <key>StandardErrorPath</key>
    <string>/Users/qujunsheng/Projects/batch-lab/logs/launchd-error.log</string>

    <key>EnvironmentVariables</key>
    <dict>
        <key>JAVA_HOME</key>
        <string>/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home</string>
        <key>PATH</key>
        <string>/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin</string>
    </dict>
</dict>
</plist>
```

3. 加载任务：
```bash
launchctl load ~/Library/LaunchAgents/com.batchlab.daily.plist
```

4. 查看任务状态：
```bash
launchctl list | grep batchlab
```

5. 如果想停止：
```bash
launchctl unload ~/Library/LaunchAgents/com.batchlab.daily.plist
```

---

### 方案 C: 部署到服务器 (生产级)

如果您需要 24/7 不间断运行，最终应该把 jar 包部署到一台永远在线的 Linux 服务器上，然后在服务器上用 cron 调度。这是企业真正的做法，但目前学习阶段不需要这么复杂。

---

### 传统 Cron 用法 (仅供参考，Mac 休眠后不会执行)

```bash
# 1. 赋予脚本执行权限（必做）
chmod +x scripts/*.sh

# 2. 编辑 crontab
crontab -e

# 3. 粘贴以下内容 (每天凌晨 1 点跑一次终极串联 Job，使用默认 local 配置，日志按日期切分)
# ⚠️ 注意: 在 crontab 中 % 必须用 \ 转义，写成 \%
0 1 * * * /Users/qujunsheng/Projects/batch-lab/scripts/run-job.sh dailyReportCompositeJob >> /Users/qujunsheng/Projects/batch-lab/logs/cron-\$(date +\%Y\%m\%d).log 2>&1

# 4. 保存退出后查看
crontab -l
```

Cron 格式说明：
```
┌───── 分钟 (0 - 59)
│ ┌───── 小时 (0 - 23)
│ │ ┌───── 日 (1 - 31)
│ │ │ ┌───── 月 (1 - 12)
│ │ │ │ ┌───── 星期几 (0 - 7, 0和7都是星期日)
│ │ │ │ │
* * * * * 要执行的命令
```

常用表达式：
```
0 1 * * *     每天凌晨 1:00
*/5 * * * *   每 5 分钟
0 9 * * 1-5   工作日每天上午 9:00
0 0 1 * *     每月 1 号凌晨
```

---

## Part 3: Git 仓库脚本可执行权限管理 ⭐️

在团队协作开发中，如果其他人 clone 您的项目，可能会因为操作系统的差异导致 shell 脚本失去 `x` (executable) 可执行权限，出现 `Permission denied` 报错。

为了彻底解决此问题，您可以直接让 Git 保持对该脚本的执行状态跟踪，在终端中执行：

```bash
# 让 Git 保持 run-job.sh 和 run-local.sh 始终可执行
git update-index --chmod=+x scripts/run-job.sh
git update-index --chmod=+x scripts/run-local.sh

# 提交改动
git commit -m "chore: keep scripts executable in git index"
```

这样，团队其他成员在任何地方 git clone 出来的项目，其脚本默认都自带 `x` 可执行权限，体验极佳。

