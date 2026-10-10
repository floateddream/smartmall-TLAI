#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_FILE="$DEPLOY_DIR/compose.yaml"
ENV_FILE="${SMARTMALL_ENV_FILE:-$DEPLOY_DIR/.env}"
BACKUP_ROOT="${1:-$DEPLOY_DIR/backups}"
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
DESTINATION="$BACKUP_ROOT/$STAMP"

if [ ! -f "$ENV_FILE" ]; then
  echo "找不到环境文件：$ENV_FILE" >&2
  exit 1
fi
if [ -e "$DESTINATION" ]; then
  echo "备份目标已存在，拒绝覆盖：$DESTINATION" >&2
  exit 1
fi

mkdir -p "$DESTINATION/minio"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T postgres \
  sh -c 'exec pg_dump --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --clean --if-exists --no-owner --no-privileges' \
  > "$DESTINATION/postgres.sql"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" cp minio:/data/. "$DESTINATION/minio"

cat > "$DESTINATION/manifest.txt" <<EOF
createdAtUtc=$STAMP
contents=postgresql,minio
redis=excluded-cache-only
restoreRequires=--confirm-overwrite
EOF

echo "备份已新增：$DESTINATION"
