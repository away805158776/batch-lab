import os

readme_path = "README.md"
with open(readme_path, "r", encoding="utf-8") as f:
    content = f.read()

target = "## ⏰ 定时调度 (Cron)\n\n完整的定时任务配置指南请查阅：[scripts/cron/crontab-setup.txt](scripts/cron/crontab-setup.txt)。"

replacement = """## ⏰ 定时调度与运行保护 (Cron & Locks)

为了保证生产环境的稳定与效率，本项目支持了脚本并发锁（防重复运行）、环境自动化预检、双解析参数兼容（Scheme A 位置参数与 Scheme B 原生 Spring 参数）。

### 1. 脚本赋权与 Git 保持执行权限
在首次使用或拉取代码后，建议执行脚本赋予执行权限：
```bash
chmod +x scripts/*.sh
```
为了防止团队中其他人 clone 代码后权限丢失，可以直接在 Git 索引中强制开启执行跟踪：
```bash
git update-index --chmod=+x scripts/run-job.sh
git update-index --chmod=+x scripts/run-local.sh
```

### 2. 双重参数解析运行示例
优化后的 `run-job.sh` 同时支持以下两种解析方式：
* **Scheme A (经典位置参数 - 推荐)**：
  ```bash
  # ./scripts/run-job.sh <JobName> [profile] [其他自定义参数]
  ./scripts/run-job.sh csvProcessJob local
  ```
* **Scheme B (Spring 原生透传)**：
  ```bash
  # ./scripts/run-job.sh --spring.batch.job.name=<JobName> [其他参数]
  ./scripts/run-job.sh --spring.batch.job.name=csvProcessJob
  ```

### 3. 定时调度与日志日期分片
完整的定时任务配置指南及 Mac 专属 launchd 调度指引，请查阅以下深度文档：
* **配置指南**：[scripts/cron/crontab-setup.txt](scripts/cron/crontab-setup.txt)
* **Mac 专属 launchd 调度与锁说明**：[docs/docker-and-cron-guide.md](docs/docker-and-cron-guide.md)"""

if target in content:
    content = content.replace(target, replacement)
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(content)
    print("SUCCESS: README.md updated successfully.")
else:
    print("WARNING: Target string not found in README.md.")
