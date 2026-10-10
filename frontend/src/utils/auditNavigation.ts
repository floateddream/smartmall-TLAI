import { readPositiveQueryId, readTraceId } from './routeQuery.js'

export function auditCorrelationNavigation(
  destination: 'agent-audits' | 'ai-call-logs',
  traceId: unknown,
  permitted: boolean,
) {
  const safeTraceId = readTraceId(traceId)
  return permitted && safeTraceId
    ? { name: destination, query: { traceId: safeTraceId } }
    : null
}

export function auditConversationNavigation(conversationId: unknown, permitted: boolean) {
  const safeId = readPositiveQueryId(typeof conversationId === 'number'
    ? String(conversationId) : conversationId)
  return permitted && safeId !== null
    ? { name: 'assistant' as const, query: { conversationId: String(safeId) } }
    : null
}
