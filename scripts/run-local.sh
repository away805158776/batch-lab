#!/bin/bash
# run-local.sh - 本地开发启动脚本
# 用法: ./scripts/run-local.sh <JobName> [profile] [extra-args...]
# 示例:
#   ./scripts/run-local.sh csvProcessJob          # 默认用 H2 内存数据库
#   ./scripts/run-local.sh csvToDbJob mysql        # 连接 MySQL 数据库

# 开启严格的 Bash 错误处理
set -euo pipefail

# 切换到项目根目录
cd "$(dirname "$0")/.."

# 检查 maven 环境变量
if ! command -v mvn >/dev/null 2>&1; then
    echo "================================================================"
    echo " ERROR: Maven (mvn) command not found!"
    echo " Please make sure Maven is installed and added to your PATH."
    echo "================================================================"
    exit 1
fi

JOB_NAME=${1:-""}
PROFILE=${2:-"local"}

if [ -n "$JOB_NAME" ]; then
    echo "================================================================"
    echo " Batch Lab 启动中 (本地开发模式)..."
    echo " Job:     $JOB_NAME"
    echo " Profile: $PROFILE"
    echo "================================================================"
    
    # 提取除了前两个位置参数之外的所有参数并透传给 spring boot
    shift 2 || shift 1 || true
    mvn spring-boot:run \
        -Dspring-boot.run.profiles="$PROFILE" \
        -Dspring-boot.run.arguments="--spring.batch.job.name=$JOB_NAME $*"
else
    echo "================================================================"
    echo " 用法: ./scripts/run-local.sh <JobName> [profile] [extra-args...]"
    echo ""
    echo " 可用的 Job:"
    echo "   csvProcessJob            - 纯文件清洗流转"
    echo "   fileArchiveJob           - 文件世代归档"
    echo "   csvToDbJob               - CSV 批量入库"
    echo "   dbToFixedJob             - 数据库抽出定长文件"
    echo "   dailyReportCompositeJob  - 多步联动终极 Job"
    echo ""
    echo " 可用的 Profile (可选，默认 local):"
    echo "   local  - H2 内存数据库 (默认，无需额外安装)"
    echo "   mysql  - MySQL 数据库 (需先 docker compose up -d)"
    echo ""
    echo " 示例:"
    echo "   ./scripts/run-local.sh csvProcessJob"
    echo "   ./scripts/run-local.sh csvToDbJob mysql"
    echo "================================================================"
fi
