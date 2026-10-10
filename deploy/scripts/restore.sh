#!/usr/bin/env sh
set -eu

if [ "$#" -ne 2 ] || [ "$1" != "--confirm-overwrite" ]; then
  echo "恢复会覆盖当前 PostgreSQL 对象和同名 MinIO 对象。" >&2
  echo "用法：$0 --confirm-overwrite <备份目录>" >&2
  exit 2
fi

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_FILE="$DEPLOY_DIR/compose.yaml"
ENV_FILE="${SMARTMALL_ENV_FILE:-$DEPLOY_DIR/.env}"
SOURCE=$2

if [ ! -f "$ENV_FILE" ]; then
  echo "找不到环境文件：$ENV_FILE" >&2
  exit 1
fi
if [ ! -f "$SOURCE/postgres.sql" ] || [ ! -d "$SOURCE/minio" ]; then
  echo "备份目录不完整，必须包含 postgres.sql 和 minio/：$SOURCE" >&2
  exit 1
fi

echo "即将停止应用写入并从以下目录恢复：$SOURCE"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" stop backend
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d postgres minio
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T postgres \
  sh -c 'exec psql --set ON_ERROR_STOP=on --username="$POSTGRES_USER" --dbname="$POSTGRES_DB"' \
  < "$SOURCE/postgres.sql"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" cp "$SOURCE/minio/." minio:/data
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d backend frontend
echo "恢复命令已完成；请立即执行健康检查和抽样核验。"
