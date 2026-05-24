# 现代化企业级 Spring Batch 实验场 (Batch-Lab)

本工程是一个用于深度演练 Spring Batch、文件处理、MyBatis 数据库操作以及 Shell/Cron 调度的综合型现代化实验场。
它严格遵循企业级 Java 后端架构，支持多 Job 组装与独立运行，完美适配长期扩展需求。

## 🎯 架构体系

```text
batch-lab
│
├── README.md                # 本文档
├── pom.xml                  # Maven 依赖管理 (Java 21, Spring Boot 3, Batch 5, MyBatis)
│
├── docs/                    # 深度文档
├── docker/                  # 容器化环境
│   └── compose.yml          # MySQL 一键启动配置 (docker compose up -d)
│
├── logs/                    # 业务运行与框架日志存放点
│
├── data/                    # 批处理本地数据交换目录
│   ├── input/               # 输入文件 (如: sample.csv)
│   ├── output/              # 成功处理后的输出 (如: result.csv, export.txt)
│   ├── temp/                # 临时处理目录
│   ├── archive/             # 处理完成后的历史文件存放点 (带时间戳世代管理)
│   └── error/               # 错误/脏数据文件收集点
│
└── src/main/
    ├── java/com/example/batchlab/
    │   ├── config/          # 框架级配置
    │   ├── batch/           # Spring Batch 核心组件 (企业级最关键层)
    │   │   ├── job/         # Job 组装配置 (定义 Job 包含哪些 Step)
    │   │   ├── step/        # Step 组装配置 (定义 Step 包含哪些 Reader/Writer/Processor 或 Tasklet)
    │   │   ├── tasklet/     # 单步任务实现 (如：操作文件移动)
    │   │   ├── reader/      # 数据读取层 (CSV FlatFile, MyBatis Cursor)
    │   │   ├── processor/   # 数据处理与清洗层 (业务逻辑的核心)
    │   │   └── writer/      # 数据输出层 (CSV, Fixed-Length, MyBatis Batch Insert)
    │   ├── domain/          # 领域模型 (DTO, Entity)
    │   ├── mapper/          # MyBatis Mapper 接口 (持久层)
    │   └── exception/       # 异常定义
    └── resources/
        ├── application.yml        # 全局配置 (开启了 MyBatis 和 SQL Schema 自动初始化)
        ├── application-local.yml  # 本地配置 (默认 H2 内存数据库)
        ├── application-mysql.yml  # MySQL 配置 (需 Docker 启动 MySQL)
        ├── mapper/                # MyBatis XML 映射文件
        └── sql/                   # 数据库建表脚本 (schema.sql)
```

## 🚀 5 大硬核 Batch 演练模型 (Input/Output 详解)

为了让您能够练习到最精髓的企业级逻辑，本工程内置了 5 种截然不同的 Batch Job。

### 1. `csvProcessJob` (纯文件清洗流转)
*   **用途**: 对传入的文件数据进行业务校验、清洗和转换。
*   **动作 (Action)**: 读取输入，改变状态为 `PROCESSED`，然后直接写出新文件。
*   **输入 (Input)**: `data/input/sample.csv`
*   **输出 (Output)**: `data/output/result.csv`

### 2. `fileArchiveJob` (单步工具流)
*   **用途**: 防止源数据被第二天重复处理的"世代归档"机制。
*   **动作 (Action)**: 扫描输入目录，找到所有的 CSV 文件，追加当前时间戳后缀（例：`sample_20260517120000.csv`），然后执行系统底层移动。
*   **输入 (Input)**: `data/input/sample.csv`
*   **输出 (Output)**: `data/archive/sample_时间戳.csv`
*   **⚠️ 源文件保护**: 默认配置下，**移动**会删除原始 input 文件（真实的"世代归档"语义）。但本地 `local` profile 已默认开启 **复制保留**（`app.batch.archive.keep-source: true`），所以演练时反复运行 **不会** 把 `sample.csv` 跑没了。详见下方「源文件保护机制」。

### 3. `csvToDbJob` (批量高性能入库)
*   **用途**: 将合作方传来的海量文本数据快速刷入我们的数据库中。
*   **动作 (Action)**: 解析 CSV 映射为 DTO，转换为 Entity，使用 MyBatis 框架每 10 条执行一次批量 `INSERT`，状态记为 `IMPORTED`。
*   **输入 (Input)**: `data/input/sample.csv`
*   **输出 (Output)**: 数据库表 `transaction_info` 新增记录。

### 4. `dbToFixedJob` (数据库抽出固定长文件)
*   **用途**: 按照老旧系统的严苛要求，从数据库抽数据并生成严格占位格式的文本（日本常说的固定长 Format）。
*   **动作 (Action)**: 利用 MyBatis 游标 (Cursor) 按行扫描千万级表不爆内存。将数字补0，字符串补空格，抽出成 `.txt`。
*   **输入 (Input)**: 数据库表 `transaction_info`
*   **输出 (Output)**: `data/output/export.txt`

### 5. `dailyReportCompositeJob` (多步联动终极 Job)
*   **用途**: 每日最终跑批的总控任务，编排并串联前面多个独立的逻辑。
*   **动作 (Action)**: 自动按顺序执行：1(入库) -> 2(抽出定长报表) -> 3(归档原始文件)。任何一步失败立刻中断。
*   **输入 (Input)**: `data/input/sample.csv`
*   **输出 (Output)**: 数据库新增数据 + `data/output/export.txt` + `data/archive/sample_xxx.csv`。

---

## 💻 运行手册

> **Spring Boot 3 必读**：必须通过 `--spring.batch.job.name` 参数精确指定运行哪个 Job，否则会报错 `Job name must be specified in case of multiple jobs`。下方脚本已自动帮你拼接该参数，直接传 Job 名即可。

不带参数运行脚本即可看到帮助菜单：
```bash
./scripts/run-local.sh
```

### 使用 H2 内存数据库 (默认，无需任何安装)

```bash
./scripts/run-local.sh csvProcessJob           # 文件清洗
./scripts/run-local.sh fileArchiveJob          # 文件归档
./scripts/run-local.sh csvToDbJob              # CSV 入库
./scripts/run-local.sh dbToFixedJob            # 数据库抽出定长文件
./scripts/run-local.sh dailyReportCompositeJob # 终极多步联动
```
*(注意：`dbToFixedJob` 抽数据前，请先跑 `csvToDbJob` 确保库里有数据。跑完后检查 `data/output/export.txt`。)*

### 使用 MySQL 数据库 (需先启动 Docker)

```bash
# 1. 启动 MySQL
cd docker && docker compose up -d && cd ..

# 2. 运行 (第二个参数传 mysql)
./scripts/run-local.sh csvToDbJob mysql
```

---

## 🛡️ 源文件保护机制 (避免 input 跑一次就没了)

`fileArchiveJob` / `dailyReportCompositeJob` 的归档步骤由 `FileArchiveTasklet` 实现，行为由配置项 `app.batch.archive.keep-source` 控制：

| 配置值 | 行为 | 适用场景 |
|--------|------|----------|
| `true` (本地 local 默认) | **复制**到 archive，原始 `data/input/sample.csv` **保留** | 学习演练，反复跑不丢文件 |
| `false` (全局默认 / 生产) | **移动**到 archive，原始文件**被删除** | 真实世代归档，防止次日重复处理 |

想体验真实的"移动归档"语义，可临时覆盖：
```bash
./scripts/run-local.sh fileArchiveJob local --app.batch.archive.keep-source=false
```

---

## 🗄️ 数据库连接与查看 (H2 内存数据库)

目前我们在 `application-local.yml` 中默认使用的是 **H2 内存数据库**，它无需您安装任何外部软件，极其适合跑批演练。系统每次启动都会通过 `resources/sql/schema.sql` 自动在内存中建好 `transaction_info` 表。

**如何查看数据库里的数据？**
1. 启动任何一个 Batch Job 时（它会占用一小段时间才退出），趁进程未退出时操作。
2. 打开浏览器访问自带控制台：`http://localhost:8080/h2-console`
3. 在页面中填写配置：
   - **JDBC URL**: `jdbc:h2:mem:batchdb`
   - **User Name**: `SA`
   - **Password**: *(留空)*
4. 点击 `Connect` 即可看到 `transaction_info` 物理表，并可执行 `SELECT * FROM transaction_info` 查询批量入库的数据！

> **更多深度的架构设计解释，请移步查阅 [docs/architecture-design.md](docs/architecture-design.md)。**

---

## ⏰ 定时调度 (Cron)

完整的定时任务配置指南请查阅：[scripts/cron/crontab-setup.txt](scripts/cron/crontab-setup.txt)，其中包含 **秒级测试调度** 的做法（cron 最小粒度是分钟，秒级需用循环脚本模拟）。

---

## 📚 深度文档索引

| 文档 | 内容 |
|------|------|
| [docs/batch-execution-flow.md](docs/batch-execution-flow.md) | **命令行到 Java 代码的完整执行链** — 从敲命令到 JVM 退出的 6 层调用详解 |
| [docs/database-guide.md](docs/database-guide.md) | **数据库表结构与 MySQL 切换** — 建表 SQL 说明、H2 → MySQL 一键切换步骤 |
| [docs/docker-and-cron-guide.md](docs/docker-and-cron-guide.md) | **Docker 与定时任务** — Docker 是什么、Mac 休眠下如何用 launchd 替代 cron |
| [docs/architecture-design.md](docs/architecture-design.md) | **架构设计原理** — 为什么这么分包、MyBatis 和 Spring Batch 怎么配合 |
