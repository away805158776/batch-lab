# Batch Job 执行流程深度解析

## 1. 从命令行到 Java 代码，到底发生了什么？

当您敲下这行命令时：
```bash
./scripts/run-local.sh csvToDbJob
```

整个执行链条是这样的：

```
┌───────────────────────────────────────────────────────────────┐
│ 第 1 层: Shell 脚本 (run-local.sh)                           │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ 接收参数 "csvToDbJob"                                   │   │
│ │ 拼接命令:                                               │   │
│ │ mvn spring-boot:run                                     │   │
│ │   -Dspring-boot.run.profiles=local                      │   │
│ │   -Dspring-boot.run.arguments=                          │   │
│ │     "--spring.batch.job.name=csvToDbJob"                 │   │
│ └─────────────────────────────────────────────────────────┘   │
│                          ↓                                    │
│ 第 2 层: Maven (mvn spring-boot:run)                         │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ 编译 Java 源代码 → 启动 Spring Boot 应用                │   │
│ │ 加载 application.yml + application-local.yml             │   │
│ │ 接收参数: spring.batch.job.name=csvToDbJob               │   │
│ └─────────────────────────────────────────────────────────┘   │
│                          ↓                                    │
│ 第 3 层: Spring Boot 启动                                    │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ 1. 扫描所有 @Configuration 类，创建所有的 Bean           │   │
│ │    - 5 个 Job Bean (csvProcessJob, csvToDbJob...)        │   │
│ │    - 对应的 Step, Reader, Processor, Writer Bean         │   │
│ │ 2. 初始化数据库连接池 (HikariCP)                         │   │
│ │ 3. 执行 schema.sql 建表                                  │   │
│ │ 4. 创建 Spring Batch 元数据表                            │   │
│ └─────────────────────────────────────────────────────────┘   │
│                          ↓                                    │
│ 第 4 层: JobLauncherApplicationRunner                        │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ Spring Boot 启动完成后自动触发                           │   │
│ │ 读取参数 spring.batch.job.name=csvToDbJob                │   │
│ │ 从 5 个 Job 中筛选出 csvToDbJob                          │   │
│ │ 调用 JobLauncher.run(csvToDbJob, parameters)             │   │
│ └─────────────────────────────────────────────────────────┘   │
│                          ↓                                    │
│ 第 5 层: Job 执行                                            │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ csvToDbJob 包含 1 个 Step: csvToDbStep                   │   │
│ │                                                         │   │
│ │ csvToDbStep 的 Chunk 处理循环:                           │   │
│ │ ┌─────────────────────────────────────────────────────┐ │   │
│ │ │ 循环开始 (chunk-size=10)                            │ │   │
│ │ │                                                     │ │   │
│ │ │ Reader: 从 CSV 读 10 条 → TransactionDto            │ │   │
│ │ │    ↓                                                │ │   │
│ │ │ Processor: Dto → Entity (status=IMPORTED)           │ │   │
│ │ │    ↓                                                │ │   │
│ │ │ Writer: MyBatis 批量 INSERT 10 条到数据库            │ │   │
│ │ │    ↓                                                │ │   │
│ │ │ 提交事务 (commit)                                   │ │   │
│ │ │    ↓                                                │ │   │
│ │ │ 还有数据吗？→ 是 → 回到循环开始                      │ │   │
│ │ │              → 否 → Step 完成                        │ │   │
│ │ └─────────────────────────────────────────────────────┘ │   │
│ └─────────────────────────────────────────────────────────┘   │
│                          ↓                                    │
│ 第 6 层: 结束                                                │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ 更新 BATCH_JOB_EXECUTION 表: status=COMPLETED           │   │
│ │ 关闭数据库连接池                                         │   │
│ │ JVM 退出，进程结束                                       │   │
│ └─────────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────┘
```

## 2. Chunk 和 Tasklet 的区别

Spring Batch 的 Step 有两种执行模式：

### Chunk 模式 (处理大量数据)
用于 csvProcessJob、csvToDbJob、dbToFixedJob。

```
读10条 → 处理10条 → 写10条 → 提交事务
读10条 → 处理10条 → 写10条 → 提交事务
读 3条 → 处理 3条 → 写 3条 → 提交事务（最后一批不足10条）
→ Step 完成
```

**关键点**: 如果中途某一批的写入失败了，只有这 10 条会回滚，前面已提交的不受影响。这就是 Chunk 的事务保护机制。

### Tasklet 模式 (干一件事)
用于 fileArchiveJob。

```
执行一次 execute() 方法 → 返回 FINISHED → Step 完成
```

**关键点**: 没有循环，没有 Reader/Writer，适合"扫描目录移动文件"这类非数据流的操作。

## 3. dailyReportCompositeJob 的多步串联

这是最复杂的 Job，它包含 3 个 Step 按顺序执行：

```
dailyReportCompositeJob
│
├── Step 1: csvToDbStep (Chunk模式)
│   读 CSV → 转 Entity → 批量 INSERT 入库
│   → COMPLETED ✅
│         ↓
├── Step 2: dbToFixedStep (Chunk模式)
│   游标读数据库 → 改状态 → 写固定长文件 export.txt
│   → COMPLETED ✅
│         ↓
└── Step 3: fileArchiveStep (Tasklet模式)
    扫描 input 目录 → 移动到 archive 并加时间戳
    → COMPLETED ✅

整个 Job → COMPLETED ✅
```

**如果 Step 2 失败了会怎样？**
- Step 1 的数据已经入库了（已提交的事务不会回滚）
- Step 3 不会被执行（后续 Step 全部跳过）
- Job 状态变为 FAILED
- 下次重新运行时，Spring Batch 会从 Step 2 开始重试（断点续跑机制）
