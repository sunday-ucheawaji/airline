import { useCallback, useEffect, useState } from 'react'

/** Seconds left before an action may be repeated, e.g. resending an email. */
export function useCooldown(seconds: number) {
  const [remaining, setRemaining] = useState(0)

  useEffect(() => {
    if (remaining <= 0) return
    const timer = setTimeout(() => setRemaining((left) => left - 1), 1000)
    return () => clearTimeout(timer)
  }, [remaining])

  const start = useCallback(() => setRemaining(seconds), [seconds])
  return { remaining, start, isCoolingDown: remaining > 0 }
}
