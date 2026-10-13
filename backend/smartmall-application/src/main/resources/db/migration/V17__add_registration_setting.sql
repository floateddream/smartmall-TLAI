-- 系统级开关与角色权限分开保存；只有管理员可以修改，公开注册只读取该值。
CREATE TABLE sys_system_setting (
    setting_key VARCHAR(80) PRIMARY KEY,
    setting_value VARCHAR(500) NOT NULL,
    description VARCHAR(300),
    updated_by BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sys_system_setting_key CHECK (setting_key ~ '^[a-z][a-z0-9_.-]{1,79}$')
);

-- 升级时保持原有注册行为，管理员可在“用户与权限”页面关闭。
INSERT INTO sys_system_setting (setting_key, setting_value, description)
VALUES ('registration.enabled', 'true', '是否允许访客通过公开注册页面创建运营账号');
