#!/usr/bin/env sh
set -eu

# SmartMall TLAI 服务器日志查看脚本。
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE="${SMARTMALL_ENV_FILE:-$PROJECT_DIR/deploy/.env}"
COMPOSE_FILE="$PROJECT_DIR/deploy/compose.server.yaml"
TARGET="${1:-backend}"

usage() {
  cat <<'EOF'
用法：
  sh deploy/scripts/logs.sh [backend|frontend|all]

示例：
  sh deploy/scripts/logs.sh           # 实时查看后端日志
  sh deploy/scripts/logs.sh frontend  # 实时查看前端日志
  sh deploy/scripts/logs.sh all       # 实时查看前后端日志

按 Ctrl+C 退出日志查看，不会停止容器。
EOF
}

case "$TARGET" in
  backend|frontend|all) ;;
  -h|--help) usage; exit 0 ;;
  *) echo "不支持的日志目标：$TARGET" >&2; usage >&2; exit 2 ;;
esac

command -v docker >/dev/null 2>&1 || { echo "未找到 docker 命令。" >&2; exit 1; }

if [ ! -f "$ENV_FILE" ]; then
  echo "找不到服务器配置文件：$ENV_FILE" >&2
  exit 1
fi

cd "$PROJECT_DIR"

if [ "$TARGET" = "all" ]; then
  exec docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs -f --tail=200
fi

exec docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs -f --tail=200 "$TARGET"
