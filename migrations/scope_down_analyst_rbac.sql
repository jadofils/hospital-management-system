-- Scope down the Analyst role's blanket read grant so it no longer covers the
-- RBAC-management tables (users/roles/permissions/user_roles/role_permissions).
-- The original seed granted "every read permission except developer_tools",
-- which unintentionally let Analyst read who has which access — an
-- administrative concern, not a reporting one. Re-running the seed script alone
-- won't retract these already-granted rows (it only uses ON CONFLICT DO NOTHING),
-- so this migration removes them explicitly on already-provisioned databases.
DELETE FROM role_permissions rp
USING roles r, permissions p
WHERE rp.role_id = r.role_id
  AND rp.permission_id = p.permission_id
  AND r.role_name = 'Analyst'
  AND p.action = 'read'
  AND p.resource IN ('users', 'roles', 'permissions', 'user_roles', 'role_permissions');
