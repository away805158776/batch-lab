# 核心架构与设计说明

## 1. 为什么采用这样的包结构？

Spring Batch 工程随着业务的复杂度上升，很容易变成一堆杂乱无章的配置文件。本工程采用了**严格职责分离**的企业级标准分层架构：

*   **`batch/job`**: 这是最高层的编排者。它不写任何业务逻辑，只负责把不同的 `Step` 拼装起来。
*   **`batch/step`**: 它是执行单元。它负责组装 `Reader`、`Processor` 和 `Writer`，并设定事务的 `chunk` 大小（比如一次读写 10 条）。
*   **`batch/reader` & `writer`**: 负责解决“数据从哪来、到哪去”的问题（CSV、数据库、定长文件）。我们把它们单独抽离成配置类，实现了 I/O 层和业务层的解耦。
*   **`batch/processor`**: 核心业务处理引擎。这是真正写 `if/else` 的地方。数据转换、校验、计算都在这里进行。
*   **`batch/tasklet`**: 当我们的任务不是“处理大量数据流”，而是“干一件单独的事”（比如：清空一张表、移动一个文件、发一封邮件）时，我们使用 Tasklet。

## 2. 数据库集成设计 (MyBatis)

本工程没有使用 JPA（Hibernate），而是使用了在金融系统及日本项目中占据绝对统治地位的 **MyBatis**，因为它：
1. 允许你手写高难度的复杂 SQL。
2. 在海量数据的批处理写入中，MyBatis 的批量 Insert 性能远优于 JPA。

### 数据库实体映射：
1. **Entity**: `domain/entity/TransactionEntity.java` (映射物理表 `transaction_info`)
2. **Mapper Interface**: `mapper/TransactionMapper.java` (定义 Java 方法)
3. **Mapper XML**: `resources/mapper/TransactionMapper.xml` (定义实际执行的 SQL 语句)

每次 `run-local.sh` 启动时，Spring Boot 会自动执行 `resources/sql/schema.sql`，在 H2 内存数据库中创建这张表。

## 3. 多 Job 环境下的调度隔离

在真实的企业环境中，你可能有 100 个不同的 Batch Job 写在同一个工程里。Spring Boot 默认启动会把这 100 个 Job 全部跑一遍，这是灾难性的。

所以，我们必须在启动时通过参数 **强制指定** 本次只跑哪个 Job。我们在 `scripts/run-local.sh` 中帮您封装好了这一步，只要把名字传进去，它就会自动生成 `--spring.batch.job.name=xxx` 传给框架，从而实现完美隔离。
