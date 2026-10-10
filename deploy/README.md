# 生产部署指南

## 拓扑与边界

```text
公网 :80/:443 -> frontend(Nginx) -> backend:8080
                                      |-> postgres:5432
                                      |-> redis:6379
                                      `-> minio:9000
```

默认只发布前端 `80`；数据库、缓存、对象存储控制台和后端端口均不发布。`data` 网络设为 internal。默认内存上限约 2.9 GiB，适合 2 核 4 GiB 主机的演示/轻量负载，不代表压测后的生产容量。

## 首次部署

1. 安装 Docker Engine 与 Compose v2，准备至少 4 GiB 内存和持久磁盘。
2. 将 `.env.example` 复制为 `.env`，为数据库、Redis、MinIO、JWT 和 AI 主密钥分别生成独立随机值。不要使用示例占位值。
3. 确认 `MINIO_IMAGE` 指向组织已扫描并推送的固定镜像；默认值是项目约定的私有镜像名。
4. 先渲染并人工检查配置，再构建启动：

```bash
docker compose --env-file deploy/.env -f deploy/compose.yaml config
docker compose --env-file deploy/.env -f deploy/compose.yaml up -d --build
docker compose --env-file deploy/.env -f deploy/compose.yaml ps
```

Flyway 在后端启动时按 V1～V16 顺序迁移。不要修改已经发布的迁移，也不要在未确认数据库状态时执行 `repair`。

## 健康与日志

- 外部入口：`GET /healthz` 返回 `ok`。
- 后端容器：`/actuator/health/readiness`，仅容器网络内使用。
- Compose 为每个服务启用 10 MiB × 3 的日志轮转。
- `docker compose ... logs --since 10m backend` 用于排查；分享日志前先检查敏感信息。

## HTTPS

推荐在宿主机或云负载均衡器终止 TLS。若直接由容器 Nginx 终止，可基于 `nginx/https.conf.example`：替换 `__PUBLIC_HOST__`，只读挂载证书目录，并在 Compose 中发布 443。证书私钥不得加入镜像或 Git。启用 HTTPS 后同步把 `SMARTMALL_CORS_ALLOWED_ORIGINS` 设置为实际 `https://` Origin。

## 备份

备份脚本只创建带 UTC 时间戳的新目录，不覆盖旧备份；内容包括 PostgreSQL 逻辑备份和 MinIO 数据，Redis 仅作为缓存不备份：

```bash
sh deploy/scripts/backup.sh /srv/smartmall-backups
```

应将备份复制到独立存储，并按组织要求加密、限制访问、定期做恢复演练。容器卷和数据库同时在线复制只能满足轻量演示；严格一致性场景应使用数据库快照和对象存储版本化方案。

## 恢复（破坏性）

恢复会清理并重建当前数据库对象、覆盖同名 MinIO 对象。脚本缺少明确确认参数时必定退出：

```bash
sh deploy/scripts/restore.sh --confirm-overwrite /srv/smartmall-backups/20261010T010203Z
```

执行前应再做一次当前环境备份并安排停机窗口。恢复后检查容器健康、Flyway 版本、关键订单/知识库文档和登录权限。脚本不会删除 MinIO 中备份不存在的额外对象；如需完全镜像恢复，应在独立环境验证后由管理员另行处理。

## 升级与回滚

镜像标签必须固定，不要使用 `latest`。升级前备份并执行测试；升级后检查健康与 Flyway。数据库迁移通常不可自动回滚，应用镜像回滚前必须确认旧版本能读取新 schema。
