# Docker Compose 部署指南

> 状态说明：仓库已经提供完整部署模板，但维护者尚未在全新服务器上完成端到端实机部署。本指南当前用于部署准备和验证，不应直接视为已经过生产验证的方案。首次公开发布前需要完成本文末尾的实机验收清单。

## 拓扑与边界

```text
公网 :80/:443 -> frontend(Nginx) -> backend:8080
                                      |-> postgres:5432
                                      |-> redis:6379
                                      `-> minio:9000
```

默认只发布前端 `80`；数据库、缓存、对象存储控制台和后端端口均不发布。`data` 网络设为 internal。默认内存上限约 2.9 GiB，目标环境为 2 核 4 GiB 的演示或轻量负载服务器，但在完成实机验收和压测前不作容量承诺。

## 两种部署方式

仓库提供两套 Compose，按服务器现状选择其一，不要同时启动：

| 文件 | 使用场景 |
| --- | --- |
| `deploy/compose.yaml` | 全新服务器，由 Compose 创建 PostgreSQL、Redis、MinIO、后端和前端。当前仍需部署者提供可用的固定 MinIO/MC 镜像。 |
| `deploy/compose.server.yaml` | 服务器已经运行 PostgreSQL、Redis、MinIO 和 Ollama，只构建后端与前端并加入现有 Docker 网络。 |

复用已有基础设施前，先用 `docker network ls` 和 `docker inspect` 确认容器网络。复制环境变量模板并只在服务器填写真实凭据：

```bash
cp deploy/server.env.example deploy/.env
docker compose --env-file deploy/.env -f deploy/compose.server.yaml config --quiet
docker compose --env-file deploy/.env -f deploy/compose.server.yaml up -d --build
docker compose --env-file deploy/.env -f deploy/compose.server.yaml ps
```

这套配置默认通过容器名连接 `smartmall-postgres`、`smartmall-redis` 和 `smartmall-minio`，并连接外部网络 `infra_default`、`minio_default`、`ollama_default`。名称不一致时只修改 `deploy/.env` 中的网络变量，不要把服务器专用名称或密码写入 Compose。Ollama 模型地址应在后台配置为 `http://smartmall-ollama:11434/v1`。已有数据库存在管理员时保持 `SMARTMALL_BOOTSTRAP_ADMIN_ENABLED=false`。

### 服务器前端一键更新

首次完成服务器部署后，前端代码更新可以在项目根目录执行：

```bash
sh deploy/scripts/update-frontend.sh
```

脚本会拉取当前 Git 分支、检查 Compose 配置、重新构建前端镜像、替换前端容器并等待健康检查通过。服务器存在未提交的已跟踪文件时，脚本会在拉取前停止；如果已经手动执行过 `git pull`，可以跳过拉取：

```bash
sh deploy/scripts/update-frontend.sh --skip-pull
```

该脚本只更新前端，不重建后端，也不会修改 `deploy/.env` 或基础设施容器。

### 服务器整套应用一键更新

后端代码、数据库迁移或前后端同时变更时，在项目根目录执行：

```bash
sh deploy/scripts/update-app.sh
```

脚本会依次拉取代码、检查 Compose 配置、构建后端与前端镜像、替换两个应用容器并等待健康检查通过。它只操作 `backend` 和 `frontend`，不会重启 PostgreSQL、Redis、MinIO 或 Ollama，也不会修改 `deploy/.env`。如果已经手动拉取过代码，可执行：

```bash
sh deploy/scripts/update-app.sh --skip-pull
```

含数据库迁移的版本应先自行完成数据库备份。新后端首次启动时会由 Flyway 自动执行尚未应用的迁移；迁移失败时后端健康检查不会通过，脚本会打印最近的后端日志并退出。

## 首次部署

1. 安装 Docker Engine 与 Compose v2，准备至少 4 GiB 内存和持久磁盘。
2. 将 `.env.example` 复制为 `.env`，为数据库、Redis、MinIO、JWT 和 AI 主密钥分别生成独立随机值。不要使用示例占位值。
3. 确认 `MINIO_IMAGE` 和 `MINIO_MC_IMAGE` 指向当前服务器能够拉取的固定镜像。示例默认值是维护者本地约定的镜像名，其他部署者不能直接依赖；MinIO 公共镜像或源码构建方案完成验证后再更新默认值。
4. 先渲染并人工检查配置，再构建启动：

```bash
docker compose --env-file deploy/.env -f deploy/compose.yaml config
docker compose --env-file deploy/.env -f deploy/compose.yaml up -d --build
docker compose --env-file deploy/.env -f deploy/compose.yaml ps
```

Flyway 在后端启动时按 V1～V17 顺序迁移。不要修改已经发布的迁移，也不要在未确认数据库状态时执行 `repair`。

## 健康与日志

- 外部入口：`GET /healthz` 返回 `ok`。
- 后端容器：`/actuator/health/readiness`，仅容器网络内使用。
- Compose 为每个服务启用 10 MiB × 3 的日志轮转。
- `docker compose ... logs --since 10m backend` 用于排查；分享日志前先检查敏感信息。

## CentOS 7 构建兼容性

CentOS 7 使用 3.10 内核，较新的 Alpine 基础镜像可能在 pnpm 安装完成后触发 `EPERM: operation not permitted, write`。前端 Dockerfile 因此固定使用 Node 22.12.0 + Alpine 3.20 和 Nginx 1.27.3 + Alpine 3.20，并同时固定镜像摘要。旧 Node 镜像内置的 Corepack 不包含当前 pnpm 签名密钥，构建阶段通过 npm 安装项目固定的 `pnpm@10.34.5`，不要通过关闭签名校验规避错误。不要仅为了追新版本把基础镜像改回 `node:22-alpine` 或 `nginx:alpine`；升级前应在目标内核上重新完成镜像构建和容器健康检查。长期部署仍建议迁移到受支持的新 Linux 发行版和 4.x 以上内核。

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

## 公开发布前实机验收

在宣布支持一键部署前，至少在一台没有项目历史环境的 Linux 服务器完成：

1. 从公开仓库全新克隆，不复用维护者已有镜像、容器、网络或数据卷。
2. 仅按照 `.env.example` 和本文准备配置，验证所有镜像都能获得或构建。
3. 执行 `docker compose ... up -d --build`，确认 PostgreSQL、Redis、MinIO、后端和前端全部健康。
4. 验证首次管理员创建、登录、模型配置、文件上传、知识库处理和 AI 对话。
5. 重启服务器，确认数据卷和服务自动恢复。
6. 完成一次备份与恢复演练，并记录实际内存、磁盘和启动耗时。
7. 根据验收结果更新镜像来源、最低配置和故障排查说明。
