# SmartMall TLAI 前端

当前已实现登录注册、后台导航、真实运营仪表盘、商品与客户管理、订单库存流程、全额退款审核、售后工单、AI 模型配置、AI 助手、知识库文档处理和 RAG 引用式问答。

## 本地启动

1. 在 IDEA 的 `smartmall-application` 模块启动 `SmartMallTlaiApplication`，并确认 `http://localhost:8080/actuator/health` 返回 `UP`。
2. 使用 Node.js 22 和 pnpm 10，在 `frontend` 目录执行：

   ```powershell
   D:\software\node-v22.23.2\npm_global\pnpm.cmd install
   D:\software\node-v22.23.2\npm_global\pnpm.cmd dev
   ```
& "D:\software\node-v22.23.2\node.exe" ".\node_modules\vite\bin\vite.js" --host 127.0.0.1
3. 打开 `http://127.0.0.1:5173`。Vite 会将 `/api` 和 `/actuator` 转发到本机后端 `127.0.0.1:8080`。

如终端优先使用旧版 Node，请先在当前 PowerShell 会话执行：

```powershell
$env:PATH='D:\software\node-v22.23.2;'+$env:PATH
```

## 认证流程

- 注册成功后返回登录页；登录支持用户名或邮箱。
- Access Token 存在本机浏览器 `localStorage`，刷新后通过 `/api/auth/me` 恢复用户。
- 401 清除 Token 并返回登录页；管理员菜单只对 `ADMIN` 展示，实际授权仍由后端负责。
- Token 过期后需重新登录；本阶段不做刷新 Token。
- AI 助手调用当前活动的 CHAT 模型；CHAT 与 EMBEDDING 分别激活，远程 API Key 只提交给后端加密保存。

仪表盘通过 `/api/dashboard/overview` 读取真实经营数据：本周订单与退款按上海时区统计，并与上周相同进度比较；趋势覆盖最近七天，待处理工单和近期订单来自实时数据库查询。服务状态来自真实健康检查。不要在前端配置数据库密码或 JWT 密钥。
