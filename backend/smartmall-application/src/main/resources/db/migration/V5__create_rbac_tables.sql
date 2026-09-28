CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(300),
    built_in BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sys_role_code CHECK (code ~ '^[A-Z][A-Z0-9_]{1,63}$')
);

CREATE TABLE sys_permission (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    module VARCHAR(40) NOT NULL,
    description VARCHAR(300),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sys_permission_code CHECK (code ~ '^[a-z][a-z0-9]*:[a-z][a-z0-9_]*$')
);

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES sys_role (id) ON DELETE RESTRICT,
    assigned_by BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_sys_user_role_role_id ON sys_user_role (role_id);

CREATE TABLE sys_role_permission (
    role_id BIGINT NOT NULL REFERENCES sys_role (id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES sys_permission (id) ON DELETE RESTRICT,
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX idx_sys_role_permission_permission_id ON sys_role_permission (permission_id);

CREATE TABLE sys_permission_audit (
    id BIGSERIAL PRIMARY KEY,
    operator_id BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
    operator_name VARCHAR(32) NOT NULL,
    target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('USER', 'ROLE')),
    target_id BIGINT NOT NULL,
    target_name VARCHAR(80) NOT NULL,
    action VARCHAR(40) NOT NULL CHECK (action IN ('USER_CREATED', 'USER_ROLES_CHANGED', 'USER_ENABLED_CHANGED', 'ROLE_CREATED', 'ROLE_UPDATED', 'ROLE_PERMISSIONS_CHANGED', 'ROLE_ENABLED_CHANGED')),
    before_value VARCHAR(1000),
    after_value VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_permission_audit_target ON sys_permission_audit (target_type, target_id, created_at DESC);
CREATE INDEX idx_sys_permission_audit_created_at ON sys_permission_audit (created_at DESC);

INSERT INTO sys_role (code, name, description, built_in) VALUES
    ('ADMIN', '系统管理员', '拥有全部系统权限，内置角色不可禁用', TRUE),
    ('OPERATOR', '运营人员', '负责商品、客户、订单和售后日常操作', TRUE);

INSERT INTO sys_permission (code, name, module, description) VALUES
    ('dashboard:view', '查看仪表盘', '总览', '访问运营仪表盘'),
    ('product:view', '查看商品', '商品', '查看商品列表和详情'),
    ('product:manage', '管理商品', '商品', '创建和修改商品'),
    ('customer:view', '查看客户', '客户', '查看客户列表和详情'),
    ('customer:manage', '管理客户', '客户', '创建和修改客户'),
    ('order:view', '查看订单', '订单', '查看订单列表和详情'),
    ('order:create', '创建订单', '订单', '创建新订单并扣减库存'),
    ('order:pay', '确认支付', '订单', '执行模拟支付'),
    ('order:cancel', '取消订单', '订单', '取消待支付订单并恢复库存'),
    ('refund:view', '查看退款', '退款', '查看退款申请和详情'),
    ('refund:apply', '申请退款', '退款', '为已支付订单提交退款'),
    ('refund:review', '审核退款', '退款', '通过或驳回退款申请'),
    ('ticket:view', '查看工单', '售后工单', '查看售后工单和处理记录'),
    ('ticket:create', '创建工单', '售后工单', '基于订单创建售后工单'),
    ('ticket:reply', '回复工单', '售后工单', '添加售后沟通记录'),
    ('ticket:status', '推进工单', '售后工单', '推进售后工单状态'),
    ('system:user_view', '查看用户', '系统设置', '查看后台用户和角色'),
    ('system:user_manage', '管理用户', '系统设置', '创建、启停用户并分配角色'),
    ('system:rbac_manage', '管理角色权限', '系统设置', '创建角色并配置权限'),
    ('system:model_manage', '管理模型配置', '系统设置', '维护 AI 模型配置');

-- ADMIN 固定拥有全部权限；OPERATOR 保留迁移前已有的日常业务能力。
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role CROSS JOIN sys_permission permission
WHERE role.code = 'ADMIN';

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role CROSS JOIN sys_permission permission
WHERE role.code = 'OPERATOR'
  AND permission.code IN (
      'dashboard:view', 'product:view', 'product:manage', 'customer:view', 'customer:manage',
      'order:view', 'order:create', 'order:pay', 'order:cancel',
      'refund:view', 'refund:apply',
      'ticket:view', 'ticket:create', 'ticket:reply', 'ticket:status'
  );

-- 先迁移每个用户的旧角色，再删除单角色字段；不会删除任何账号。
INSERT INTO sys_user_role (user_id, role_id)
SELECT app_user.id, sys_role.id
FROM app_user
JOIN sys_role ON sys_role.code = app_user.role;

ALTER TABLE app_user DROP CONSTRAINT ck_app_user_role;
ALTER TABLE app_user DROP COLUMN role;
