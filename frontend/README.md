# SmartMall TLAI 前端

当前已实现登录注册、后台导航、真实运营仪表盘、商品与客户管理、订单库存流程、全额退款审核、售后工单、AI 模型配置、AI 助手、知识库文档处理和 RAG 引用式问答。

## 本地启动

1. 准备 Node.js 22.12+ 和 pnpm 10；推荐使用 fnm、nvm、nvm-windows 或 Volta 管理 Node.js 版本。
2. 启动后端，并确认 `http://127.0.0.1:8080/actuator/health` 返回 `UP`。
3. 从仓库根目录执行：

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm dev
```

4. 打开 `http://127.0.0.1:5173`。Vite 会将 `/api` 和 `/actuator` 转发到本机后端 `127.0.0.1:8080`。

如需构建生产静态资源：

```bash
pnpm test
pnpm build
```

## 认证流程

- 注册成功后返回登录页；登录支持用户名或邮箱。
- Access Token 存在本机浏览器 `localStorage`，刷新后通过 `/api/auth/me` 恢复用户。
- 401 清除 Token 并返回登录页；管理员菜单只对 `ADMIN` 展示，实际授权仍由后端负责。
- Token 过期后需要重新登录，当前实现不提供刷新 Token。
- AI 助手调用当前活动的 CHAT 模型；CHAT 与 EMBEDDING 分别激活，远程 API Key 只提交给后端加密保存。

仪表盘通过 `/api/dashboard/overview` 读取真实经营数据：本周订单与退款按上海时区统计，并与上周相同进度比较；趋势覆盖最近七天，待处理工单和近期订单来自实时数据库查询。服务状态来自真实健康检查。不要在前端配置数据库密码或 JWT 密钥。
