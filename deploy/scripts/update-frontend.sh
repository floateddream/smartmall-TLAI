#!/usr/bin/env sh
set -eu

# SmartMall TLAI 服务器前端一键更新脚本。
# 默认先拉取当前分支，再构建并替换前端容器；传入 --skip-pull 可跳过拉取。
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE="${SMARTMALL_ENV_FILE:-$PROJECT_DIR/deploy/.env}"
COMPOSE_FILE="$PROJECT_DIR/deploy/compose.server.yaml"
SKIP_PULL=false

usage() {
  cat <<'EOF'
用法：
  sh deploy/scripts/update-frontend.sh [--skip-pull]

选项：
  --skip-pull  不执行 git pull，直接使用服务器当前代码构建
  -h, --help   显示帮助
EOF
}

case "${1:-}" in
  "") ;;
  --skip-pull) SKIP_PULL=true ;;
  -h|--help) usage; exit 0 ;;
  *) echo "不支持的参数：$1" >&2; usage >&2; exit 2 ;;
esac

command -v git >/dev/null 2>&1 || { echo "未找到 git 命令。" >&2; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "未找到 docker 命令。" >&2; exit 1; }

if [ ! -f "$ENV_FILE" ]; then
  echo "找不到服务器配置文件：$ENV_FILE" >&2
  exit 1
fi
if [ ! -f "$COMPOSE_FILE" ]; then
  echo "找不到 Compose 文件：$COMPOSE_FILE" >&2
  exit 1
fi

cd "$PROJECT_DIR"

if [ "$SKIP_PULL" = false ]; then
  # 服务器上的 deploy/.env 已被 Git 忽略，不会触发这里的工作区检查。
  if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "检测到未提交的已跟踪文件，已停止更新，避免覆盖服务器改动。" >&2
    echo "请先执行 git status 检查，或确认当前代码可用后加 --skip-pull。" >&2
    exit 1
  fi

  echo "[1/5] 拉取当前分支最新代码"
  git pull --ff-only
else
  echo "[1/5] 已跳过代码拉取"
fi

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

echo "[2/5] 检查 Compose 配置"
compose config --quiet

echo "[3/5] 构建前端镜像"
compose build frontend

echo "[4/5] 更新前端容器"
compose up -d --no-deps --no-build frontend

echo "[5/5] 等待前端容器健康"
CONTAINER_ID=$(compose ps -q frontend)
if [ -z "$CONTAINER_ID" ]; then
  echo "没有找到前端容器，最近日志如下：" >&2
  compose logs --tail=100 frontend >&2
  exit 1
fi

ATTEMPT=0
while [ "$ATTEMPT" -lt 30 ]; do
  HEALTH=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$CONTAINER_ID")
  if [ "$HEALTH" = "healthy" ] || [ "$HEALTH" = "running" ]; then
    echo "前端容器状态：$HEALTH"
    compose ps frontend
    echo "前端更新完成。"
    exit 0
  fi
  if [ "$HEALTH" = "unhealthy" ] || [ "$HEALTH" = "exited" ] || [ "$HEALTH" = "dead" ]; then
    echo "前端容器状态异常：$HEALTH，最近日志如下：" >&2
    compose logs --tail=100 frontend >&2
    exit 1
  fi
  ATTEMPT=$((ATTEMPT + 1))
  sleep 2
done

echo "等待前端健康状态超时，当前状态与最近日志如下：" >&2
compose ps frontend >&2
compose logs --tail=100 frontend >&2
exit 1
