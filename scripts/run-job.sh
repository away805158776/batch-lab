#!/bin/bash
# run-job.sh - 生产环境/打包后的启动脚本
# 前提：先执行 mvn clean package -DskipTests 打包
# 用法 (Scheme A - 位置参数): ./scripts/run-job.sh <JobName> [profile] [extra-args...]
# 用法 (Scheme B - 透传参数): ./scripts/run-job.sh --spring.batch.job.name=<JobName> [extra-args...]

# 开启严格的 Bash 错误处理模式
set -euo pipefail

# 切换到项目根目录
cd "$(dirname "$0")/.."

# 1. 自动适配并检查 JAVA_HOME
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"

if [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_EXEC="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA_EXEC="java"
else
    echo "================================================================"
    echo " ERROR: Java executable not found!"
    echo " Please set a valid JAVA_HOME or make sure 'java' is in PATH."
    echo "================================================================"
    exit 1
fi

# 2. 检查 JAR 文件是否存在
JAR_PATH="target/batch-lab-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo "================================================================"
    echo " ERROR: Executable jar file not found!"
    echo " Path: $JAR_PATH"
    echo " Please compile and package the project first by running:"
    echo "   mvn clean package -DskipTests"
    echo "================================================================"
    exit 1
fi

# 3. 兼容双重解析模式 (Scheme A & Scheme B)
IS_SCHEME_B=false
if [[ "${1:-}" == --* ]]; then
    # Scheme B: 传参透传模式 (Spring 原生格式直接传递)
    IS_SCHEME_B=true
    
    # 尝试从传参中提取 Job 名称，用于并发锁命名
    JOB_NAME=""
    for arg in "$@"; do
        if [[ "$arg" == --spring.batch.job.name=* ]]; then
            JOB_NAME="${arg#*=}"
        fi
    done
    
    # 如果没有找到具体 Job 名称，使用通用锁名称
    if [ -z "$JOB_NAME" ]; then
        JOB_NAME="spring-batch-generic"
    fi
else
    # Scheme A: 位置参数模式
    JOB_NAME="${1:-}"
    PROFILE="${2:-local}"
fi

# 检查 Job 名称是否指定
if [ -z "$JOB_NAME" ]; then
    echo "================================================================"
    echo " ERROR: Job name is required!"
    echo " Usage (Scheme A - Positional):"
    echo "   ./scripts/run-job.sh <JobName> [profile] [extra-args...]"
    echo " Usage (Scheme B - Transparent):"
    echo "   ./scripts/run-job.sh --spring.batch.job.name=<JobName> [extra-args...]"
    echo "================================================================"
    exit 1
fi

# Ensure logs directory exists
mkdir -p logs

# 4. 并发锁 (防重复执行保护)
LOCK_FILE="logs/${JOB_NAME}.pid"
if [ -f "$LOCK_FILE" ]; then
    OLD_PID=$(cat "$LOCK_FILE")
    # 安全判断旧 PID 进程是否仍在运行
    PROCESS_ALIVE=false
    kill -0 "$OLD_PID" 2>/dev/null && PROCESS_ALIVE=true || true
    
    if [ "$PROCESS_ALIVE" = true ]; then
        echo "================================================================"
        echo " ERROR: Job [$JOB_NAME] is already running! (PID: $OLD_PID)"
        echo " Execution aborted to prevent concurrent data conflicts."
        echo "================================================================"
        exit 1
    else
        # 旧进程已死亡，清除过期锁
        rm -f "$LOCK_FILE"
    fi
fi

# 写入当前进程 PID 锁
echo $$ > "$LOCK_FILE"
# 注册退出钩子，无论是正常退出还是异常退出，均清理 PID 锁
trap 'rm -f "$LOCK_FILE"' EXIT

# 5. 打印执行元数据
echo "================================================================"
echo " Batch Lab (Packaged Mode)"
echo " Time:       $(date '+%Y-%m-%d %H:%M:%S')"
echo " Job Name:   $JOB_NAME"
if [ "$IS_SCHEME_B" = true ]; then
    echo " Mode:       Scheme B (Transparent Arguments Passing)"
    echo " Arguments:  $*"
else
    echo " Mode:       Scheme A (Positional Arguments)"
    echo " Profile:    $PROFILE"
fi
echo " PID:        $$"
echo "================================================================"

# 6. 执行 Job
if [ "$IS_SCHEME_B" = true ]; then
    # Scheme B: 直接将所有参数透传给 Java
    "$JAVA_EXEC" -jar "$JAR_PATH" "$@"
else
    # Scheme A: 组合参数运行，并且支持后续追加的自定义参数
    # 使用 shift 移除前两个位置参数，剩下的参数通过 "$@" 透传
    shift 2 || shift 1 || true
    "$JAVA_EXEC" -jar "$JAR_PATH" \
        --spring.profiles.active="$PROFILE" \
        --spring.batch.job.name="$JOB_NAME" \
        "$@"
fi
