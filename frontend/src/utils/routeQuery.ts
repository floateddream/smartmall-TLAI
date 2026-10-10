export function readQueryText(value: unknown, maxLength = 100) {
  if (typeof value !== 'string') return ''
  const normalized = value.trim()
  if (!normalized || normalized.length > maxLength || /[\u0000-\u001f\u007f]/.test(normalized)) return ''
  return normalized
}

export function readQueryEnum<T extends string>(value: unknown, allowed: readonly T[]): T | '' {
  const normalized = readQueryText(value, 40)
  return allowed.includes(normalized as T) ? normalized as T : ''
}

export function readPositiveQueryId(value: unknown) {
  if (typeof value !== 'string' || !/^[1-9]\d{0,15}$/.test(value)) return null
  const id = Number(value)
  return Number.isSafeInteger(id) && id > 0 ? id : null
}

export function readQueryInteger(value: unknown, min: number, max: number) {
  if (typeof value !== 'string' || !/^\d{1,3}$/.test(value)) return null
  const number = Number(value)
  return Number.isInteger(number) && number >= min && number <= max ? number : null
}

export function readTraceId(value: unknown) {
  if (typeof value !== 'string') return ''
  const normalized = value.trim().toLowerCase()
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(normalized)
    ? normalized : ''
}

export function readIsoDate(value: unknown) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return ''
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return ''
  const [year, month, day] = value.split('-').map(Number)
  return date.getFullYear() === year && date.getMonth() + 1 === month && date.getDate() === day ? value : ''
}

interface AgentNavigationLike {
  path: string
  query?: Record<string, string>
}

export function sanitizeAgentNavigation(navigation: AgentNavigationLike | null | undefined) {
  if (!navigation || typeof navigation.path !== 'string') return null
  const source = navigation.query && typeof navigation.query === 'object' && !Array.isArray(navigation.query)
    ? navigation.query : {}
  const query: Record<string, string> = {}
  const addText = (key: string, maxLength = 100) => {
    const value = readQueryText(source[key], maxLength)
    if (value) query[key] = value
  }
  const addEnum = <T extends string>(key: string, allowed: readonly T[]) => {
    const value = readQueryEnum(source[key], allowed)
    if (value) query[key] = value
  }
  const addId = () => {
    const id = readPositiveQueryId(source.id)
    if (id !== null) query.id = String(id)
  }

  switch (navigation.path) {
    case '/orders':
      addId(); addText('keyword');
      addEnum('status', ['PENDING_PAYMENT', 'PAID', 'CANCELLED', 'REFUNDING', 'REFUNDED'] as const)
      break
    case '/refunds':
      addId(); addText('keyword')
      addEnum('status', ['PENDING_REVIEW', 'COMPLETED', 'REJECTED'] as const)
      break
    case '/tickets':
      addId(); addText('keyword')
      addEnum('status', ['OPEN', 'PROCESSING', 'RESOLVED', 'CLOSED'] as const)
      addEnum('category', ['RETURN_EXCHANGE', 'LOGISTICS', 'PRODUCT_QUALITY', 'OTHER'] as const)
      break
    case '/knowledge':
      addText('keyword')
      addEnum('status', ['UPLOADED', 'PROCESSING', 'READY', 'FAILED'] as const)
      break
    case '/data-analysis': {
      addEnum('report', ['ORDER_TREND', 'REFUND_TREND', 'REFUND_PRODUCT', 'REFUND_REASON',
        'TICKET_TREND', 'TICKET_STATUS', 'TICKET_CATEGORY'] as const)
      addEnum('granularity', ['DAY', 'WEEK', 'MONTH'] as const)
      const startDate = readIsoDate(source.startDate)
      const endDate = readIsoDate(source.endDate)
      if (validDateRange(startDate, endDate)) Object.assign(query, { startDate, endDate })
      const limit = readQueryInteger(source.limit, 1, 50)
      if (limit !== null) query.limit = String(limit)
      break
    }
    default:
      return null
  }
  return { path: navigation.path, query }
}

export function validDateRange(startDate: string, endDate: string) {
  if (!startDate || !endDate) return false
  const start = new Date(`${startDate}T00:00:00`)
  const end = new Date(`${endDate}T00:00:00`)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const days = Math.floor((end.getTime() - start.getTime()) / 86400000) + 1
  return days >= 1 && days <= 366 && end.getTime() <= today.getTime()
}
