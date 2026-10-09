INSERT INTO sys_permission (code, name, module, description)
VALUES ('analytics:view', '使用数据分析', '智能工作台', '提交受控统计查询并查看分析结果')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission ON permission.code = 'analytics:view'
WHERE role.code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;
