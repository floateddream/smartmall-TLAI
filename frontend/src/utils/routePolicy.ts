export interface RoutePolicyMeta {
  public?: unknown
  guest?: unknown
  permission?: unknown
}

export type RoutePolicyDecision = true | {
  name: 'dashboard' | 'entry-not-found'
  query?: { redirect: string }
}

export function decideRouteAccess(
  meta: RoutePolicyMeta,
  _fullPath: string,
  authenticated: boolean,
  can: (permission: string) => boolean,
): RoutePolicyDecision {
  if (meta.public) return true
  if (meta.guest) return authenticated ? { name: 'dashboard' } : true
  // 未登录时不通过后台路径暴露登录入口；登录页只接受完整的固定地址。
  if (!authenticated) return { name: 'entry-not-found' }
  if (typeof meta.permission === 'string' && !can(meta.permission)) return { name: 'dashboard' }
  return true
}
