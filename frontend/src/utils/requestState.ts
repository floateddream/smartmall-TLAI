export class LatestRequestGate {
  private sequence = 0

  begin() {
    return ++this.sequence
  }

  invalidate() {
    this.sequence++
  }

  isCurrent(requestId: number) {
    return requestId === this.sequence
  }
}

export function requestErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}
