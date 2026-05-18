# 数据库设计与切换指南

## 1. 系统用到的所有表

### 1.1 业务表：`transaction_info`

这是我们自己定义的业务数据表，所有 Batch Job 的数据流转都围绕这张表。

```sql
CREATE TABLE IF NOT EXISTS transaction_info (
    id BIGINT PRIMARY KEY,        -- 主键ID (对应 CSV 文件的第一列)
    name VARCHAR(255) NOT NULL,   -- 名称 (对应 CSV 文件的第二列)
    amount DECIMAL(10,2) NOT NULL, -- 金额 (对应 CSV 文件的第三列)
    status VARCHAR(50) NOT NULL   -- 状态 (由 Batch 动态赋值: IMPORTED, EXPORTED 等)
);
```

**这张表是怎么来的？**
- 建表 SQL 位于 `src/main/resources/sql/schema.sql`。
- 配置了 `spring.sql.init.mode=always`，所以每次启动时 Spring Boot 会自动执行这个 SQL 文件。
- H2 模式下：表创建在内存里，程序退出后数据消失。
- MySQL 模式下：表创建在物理磁盘上，数据永久保存。

### 1.2 Spring Batch 框架表 (共 6 张，框架自动管理)

Spring Batch 自己还需要一组元数据表来记录"哪个 Job 跑过了、跑成功了还是失败了"。这些表由框架自动创建和维护，您完全不需要手动操作。

| 表名 | 用途 |
|------|------|
| `BATCH_JOB_INSTANCE` | 每个 Job 的唯一实例记录 |
| `BATCH_JOB_EXECUTION` | 每次执行的开始时间、结束时间、状态 |
| `BATCH_JOB_EXECUTION_PARAMS` | 本次执行时传入的参数 |
| `BATCH_STEP_EXECUTION` | 每个 Step 的执行明细（读了几条、写了几条、跳过了几条） |
| `BATCH_STEP_EXECUTION_CONTEXT` | Step 级别的上下文信息（用于断点续跑） |
| `BATCH_JOB_EXECUTION_CONTEXT` | Job 级别的上下文信息 |

---

## 2. 如何切换到 MySQL？

### 第一步：用 Docker 一键启动 MySQL

确保您的 Mac 上已经安装了 Docker Desktop。然后在项目根目录下执行：

```bash
cd docker
docker compose up -d
```

这会自动帮您：
1. 下载 MySQL 8.0 镜像
2. 创建数据库 `batchlab_db`
3. 创建用户 `batch_user`，密码 `batch_pass`
4. 自动执行 `schema.sql`，在 MySQL 中建好 `transaction_info` 表

启动成功后，可以验证一下连接：
```bash
docker exec -it batchlab-mysql mysql -u batch_user -pbatch_pass batchlab_db -e "SHOW TABLES;"
```

### 第二步：用 MySQL 运行 Batch Job

只需要在启动脚本后面加上第二个参数 `mysql`：

```bash
./scripts/run-local.sh csvToDbJob mysql
```

**这背后发生了什么？**
- 脚本把 `--spring.profiles.active` 从默认的 `local` 切换成了 `mysql`
- Spring Boot 就去读 `application-mysql.yml` 而不是 `application-local.yml`
- 数据就写到物理 MySQL 里了，即使程序退出数据也不会丢

### 第三步：想改密码或端口？

| 要改什么 | 改哪里 |
|---------|--------|
| MySQL 用户名/密码 | `docker/compose.yml` 里的 `MYSQL_USER` / `MYSQL_PASSWORD` |
| Java 连接信息 | `src/main/resources/application-mysql.yml` 里的 `username` / `password` |
| 端口号 | `docker/compose.yml` 里的 `ports` 部分 |

> **重要：两边保持一致！** Docker 那边改了密码，Java 配置这边也要同步修改。

### 停止 MySQL

```bash
cd docker
docker compose down
```

加 `-v` 参数会同时删除数据卷（清空所有数据）：
```bash
docker compose down -v
```
