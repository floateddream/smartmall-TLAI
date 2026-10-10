import test from 'node:test'
import assert from 'node:assert/strict'
import { auditConversationNavigation, auditCorrelationNavigation } from '../src/utils/auditNavigation.js'
import { sanitizeAgentNavigation } from '../src/utils/routeQuery.js'
import { decideRouteAccess } from '../src/utils/routePolicy.js'
import { LatestRequestGate, requestErrorMessage } from '../src/utils/requestState.js'

test('route policy separates guests, unauthenticated users, and missing permissions', () => {
  assert.deepEqual(decideRouteAccess({ guest: true }, '/login', true, () => false), { name: 'dashboard' })
  assert.equal(decideRouteAccess({ guest: true }, '/login', false, () => false), true)
  assert.deepEqual(decideRouteAccess({}, '/orders?page=2', false, () => false), {
    name: 'login', query: { redirect: '/orders?page=2' },
  })
  assert.deepEqual(decideRouteAccess({ permission: 'order:view' }, '/orders', true, () => false), {
    name: 'dashboard',
  })
  assert.equal(decideRouteAccess({ permission: 'order:view' }, '/orders', true, () => true), true)
  assert.deepEqual(decideRouteAccess({}, '//evil.example', false, () => false), {
    name: 'login', query: { redirect: '/dashboard' },
  })
})

test('agent navigation keeps only the allowlisted route parameters', () => {
  assert.deepEqual(sanitizeAgentNavigation({
    path: '/orders',
    query: { id: '12', keyword: ' SM2026 ', status: 'PAID', redirect: 'https://evil.example', sql: 'DROP TABLE' },
  }), { path: '/orders', query: { id: '12', keyword: 'SM2026', status: 'PAID' } })
  assert.deepEqual(sanitizeAgentNavigation({ path: '/tickets' }), { path: '/tickets', query: {} })
  assert.equal(sanitizeAgentNavigation({ path: 'https://evil.example', query: {} }), null)
  assert.deepEqual(sanitizeAgentNavigation({
    path: '/data-analysis',
    query: { startDate: '2020-01-01', endDate: '2020-12-31', limit: '50', granularity: 'DAY' },
  }), {
    path: '/data-analysis',
    query: { granularity: 'DAY', startDate: '2020-01-01', endDate: '2020-12-31', limit: '50' },
  })
})

test('audit links require a valid identifier and the destination permission', () => {
  const traceId = '123e4567-e89b-42d3-a456-426614174000'
  assert.deepEqual(auditCorrelationNavigation('agent-audits', traceId.toUpperCase(), true), {
    name: 'agent-audits', query: { traceId },
  })
  assert.equal(auditCorrelationNavigation('agent-audits', traceId, false), null)
  assert.equal(auditCorrelationNavigation('ai-call-logs', 'not-a-trace', true), null)
  assert.deepEqual(auditConversationNavigation(42, true), {
    name: 'assistant', query: { conversationId: '42' },
  })
  assert.equal(auditConversationNavigation(42, false), null)
  assert.equal(auditConversationNavigation(-1, true), null)
})

test('latest request gate ignores stale completions and normalizes error states', () => {
  const gate = new LatestRequestGate()
  const first = gate.begin()
  const second = gate.begin()
  assert.equal(gate.isCurrent(first), false)
  assert.equal(gate.isCurrent(second), true)
  gate.invalidate()
  assert.equal(gate.isCurrent(second), false)
  assert.equal(requestErrorMessage(new Error('network unavailable'), 'fallback'), 'network unavailable')
  assert.equal(requestErrorMessage('opaque failure', 'fallback'), 'fallback')
})
