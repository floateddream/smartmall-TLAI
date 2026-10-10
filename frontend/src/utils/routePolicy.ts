export interface RoutePolicyMeta {
  guest?: unknown
  permission?: unknown
}

export type RoutePolicyDecision = true | {
  name: 'dashboard' | 'login'
  query?: { redirect: string }
}

export function decideRouteAccess(
  meta: RoutePolicyMeta,
  fullPath: string,
  authenticated: boolean,
  can: (permission: string) => boolean,
): RoutePolicyDecision {
  if (meta.guest) return authenticated ? { name: 'dashboard' } : true
  if (!authenticated) return { name: 'login', query: { redirect: safeRedirect(fullPath) } }
  if (typeof meta.permission === 'string' && !can(meta.permission)) return { name: 'dashboard' }
  return true
}

function safeRedirect(fullPath: string) {
  return fullPath.startsWith('/') && !fullPath.startsWith('//') ? fullPath : '/dashboard'
}
