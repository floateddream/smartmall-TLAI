# RBAC 用户与权限说明

## 1. 这次改造解决什么问题

原系统在 `app_user.role` 中直接保存一个角色，接口也通过 `ADMIN` 判断权限。这种方式只能给用户分配一个角色，新增角色时还要修改 Java 枚举和大量判断。

现在改为标准 RBAC：

```text
用户 app_user
  ↓ 多对多
角色 sys_role
  ↓ 多对多
权限 sys_permission
```

用户可以拥有多个角色，角色可以拥有多个权限。后端接口只校验权限编码，不再写死某个角色。

## 2. 数据库表

迁移脚本：`backend/smartmall-application/src/main/resources/db/migration/V5__create_rbac_tables.sql`

| 表 | 用途 |
|---|---|
| `sys_role` | 角色定义，包括内置角色和自定义角色 |
| `sys_permission` | 系统固定的权限点 |
| `sys_user_role` | 用户和角色的多对多关系 |
| `sys_role_permission` | 角色和权限的多对多关系 |
| `sys_permission_audit` | 用户、角色、权限变更审计记录 |

V5 会先把原 `app_user.role` 数据迁移到 `sys_user_role`，确认迁移完成后才删除旧字段，不删除用户。

内置角色：

- `ADMIN`：拥有全部权限，不能停用，也不能移除权限。
- `OPERATOR`：保留商品、客户、订单、退款申请和工单处理等日常运营权限，但没有退款审核和系统权限。

## 3. 权限如何生效

以审核退款为例：

```java
@PreAuthorize("hasAuthority('refund:review')")
```

请求链路：

```text
前端携带 JWT
  ↓
JwtAuthenticationFilter 解析用户名
  ↓
从数据库重新读取用户启用状态、角色和权限
  ↓
把角色转换为 ROLE_ADMIN 等 Authority
把权限转换为 refund:review 等 Authority
  ↓
@PreAuthorize 校验接口权限
```

JWT 中的角色列表只用于记录登录时的信息，真正授权以数据库本次查询结果为准。因此账号被停用或权限被收回后，旧 JWT 在下一次请求时也不能继续使用原权限。

## 4. 后端分层

```text
smartmall-web
  AccessControlController：管理接口和请求 DTO

smartmall-business
  UserAccountService：用户创建、启停、角色分配及保护规则
  AccessControlService：角色、权限矩阵和审计用例
  Repository 接口：声明持久化能力

smartmall-infrastructure
  MybatisUserAccountRepository：用户及角色权限批量联查
  MybatisRoleRepository：角色和权限关系持久化
  MybatisPermissionAuditRepository：审计记录持久化

smartmall-security
  AppUserPrincipal：保存当前请求的角色和权限
  SecurityConfig：认证入口配置
```

用户分页采用“先分页查询用户 ID，再批量查询角色权限”的方式，不会对每个用户单独查询角色，避免 N+1 问题。

## 5. 管理接口

| 方法 | 地址 | 权限 | 用途 |
|---|---|---|---|
| `GET` | `/api/admin/users` | `system:user_view` | 分页查询用户 |
| `POST` | `/api/admin/users` | `system:user_manage` | 创建后台用户 |
| `PUT` | `/api/admin/users/{id}/roles` | `system:user_manage` | 重新分配角色 |
| `PATCH` | `/api/admin/users/{id}/enabled` | `system:user_manage` | 启用或停用用户 |
| `GET` | `/api/admin/roles` | 用户查看或 RBAC 管理 | 查询角色 |
| `POST` | `/api/admin/roles` | `system:rbac_manage` | 创建角色 |
| `PUT` | `/api/admin/roles/{id}` | `system:rbac_manage` | 编辑角色名称和说明 |
| `PUT` | `/api/admin/roles/{id}/permissions` | `system:rbac_manage` | 替换角色权限 |
| `PATCH` | `/api/admin/roles/{id}/enabled` | `system:rbac_manage` | 启用或停用角色 |
| `GET` | `/api/admin/permissions` | `system:rbac_manage` | 查询权限矩阵 |
| `GET` | `/api/admin/audits` | `system:rbac_manage` | 查询操作审计 |

## 6. 前端菜单与按钮

前端 `auth.can('权限编码')` 用于导航体验：

```ts
auth.can('system:user_view')
auth.can('refund:review')
```

- 没有查看权限时，对应菜单和路由不可进入。
- 没有管理权限时，新建、编辑、审核等按钮不显示。
- 前端隐藏不是安全边界，最终仍由后端 `@PreAuthorize` 拦截。

用户与权限页面地址：

```text
http://127.0.0.1:5173/access-control
```

## 7. 首次创建管理员

现有 `floatdream` 账号会继续保持 `OPERATOR`，迁移不会自动把它升级为管理员。第一次使用权限页面前，需要创建一个独立管理员账号。

在本地 `application-local.yml` 中临时配置：

```yaml
smartmall:
  bootstrap-admin:
    enabled: true
    username: admin_user
    email: admin@example.com
    password: 请填写至少8位且包含字母和数字的密码
```

启动后端后，如果数据库中还没有启用的 `ADMIN`，系统会创建该账号。创建成功后把 `enabled` 改回 `false`，避免把初始化功能长期打开。

随后使用新管理员登录，在“系统设置 -> 用户与权限”中管理其他用户。`floatdream` 是否增加角色由你在页面中手动决定。

## 8. 安全保护规则

- 不能修改当前登录账号自己的角色和启用状态。
- 系统必须保留至少一个启用的 `ADMIN` 用户。
- 内置 `ADMIN` 角色不能停用，也不能减少权限。
- 用户至少要分配一个已启用角色。
- 角色和用户不做物理删除，只做启停。
- 每次用户、角色、权限变更都写入审计表。
- 关键管理员数量检查使用 PostgreSQL 事务级 advisory lock，避免并发操作同时移除最后一个管理员。
