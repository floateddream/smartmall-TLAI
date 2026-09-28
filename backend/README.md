# SmartMall TLAI 后端模块

后端是一个统一部署的模块化单体。Maven 模块用于约束职责和依赖，不代表每个模块都是独立服务。

```text
smartmall-common          统一响应、分页、业务异常和错误码
smartmall-business        用户、商品、客户、订单、退款、工单领域对象、业务服务、Repository 接口
smartmall-infrastructure  MyBatis 实体、Mapper、Repository 实现；后续 Redis/消息实现也放这里
smartmall-security        Spring Security、JWT、登录和注册用例
smartmall-web             Controller、HTTP 请求/响应 DTO、全局异常处理
smartmall-job             定时任务边界，当前没有实际任务
smartmall-application     唯一启动入口、配置、Flyway 和启动初始化
```

依赖只允许由外向内：

```text
application -> web / security / infrastructure / job
web         -> security / business / common
security    -> business / common
infrastructure -> business / common
business    -> common
```

入口类位于：

```text
smartmall-application/src/main/java/com/smartmall/tlai/SmartMallTlaiApplication.java
```

IDEA 重新加载 `backend/pom.xml` 后，应看到 7 个 Maven 子模块。启动配置的模块 classpath 要选择 `smartmall-application`。本地私密配置放在：

```text
smartmall-application/src/main/resources/application-local.yml
```

Flyway 脚本也归启动模块统一装载，原有迁移版本和数据库表保持不变。
