# SmartMall TLAI 前端

当前已实现登录注册、后台导航、商品与客户管理、订单库存流程、全额退款审核和售后工单。仪表盘仍使用演示经营数据，AI 助手、知识库和模型配置暂为占位页。

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

仪表盘的经营指标、趋势和近期订单是演示数据，业务管理页面读取真实后端接口，服务状态来自真实健康检查。不要在前端配置数据库密码或 JWT 密钥。
