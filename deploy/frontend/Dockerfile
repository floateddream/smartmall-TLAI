# CentOS 7 的 3.10 内核与较新的 Alpine 基础层存在 pnpm 写入兼容问题。
# 固定版本和摘要，保证演示服务器与其他部署环境使用同一构建基线。
FROM node:22.12.0-alpine3.20@sha256:027911463b296bdaf6df82b5ccf2c6b290fee725d5fba6513a037ed019400625 AS build
WORKDIR /app
# 旧 Node 镜像内置的 Corepack 不包含 pnpm 当前签名密钥，直接安装项目固定版本。
RUN npm install --global pnpm@10.34.5
COPY frontend/package.json frontend/pnpm-lock.yaml frontend/.npmrc ./
RUN pnpm install --frozen-lockfile
COPY frontend/ ./
RUN pnpm build

FROM nginx:1.27.3-alpine3.20@sha256:814a8e88df978ade80e584cc5b333144b9372a8e3c98872d07137dbf3b44d0e4
COPY deploy/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=5 \
  CMD wget -q -O - http://127.0.0.1/healthz | grep -q '^ok$' || exit 1
