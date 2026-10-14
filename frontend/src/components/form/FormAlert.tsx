import { useEffect, useRef, type ReactNode } from 'react'
import { CircleAlert, CircleCheck } from 'lucide-react'
import { cn } from '@/lib/utils'

type FormAlertProps = {
  tone: 'error' | 'success'
  title: string
  children?: ReactNode
  /** Move focus here when the alert appears, so keyboard and screen reader users land on what changed. */
  focusOnMount?: boolean
}

export function FormAlert({ tone, title, children, focusOnMount = true }: FormAlertProps) {
  const ref = useRef<HTMLDivElement>(null)
  const Icon = tone === 'error' ? CircleAlert : CircleCheck

  useEffect(() => {
    if (focusOnMount) ref.current?.focus()
  }, [focusOnMount, title])

  return (
    <div
      ref={ref}
      tabIndex={-1}
      role={tone === 'error' ? 'alert' : 'status'}
      className={cn(
        'flex gap-3 rounded-md border-l-[3px] bg-card p-4 shadow-[0_0_0_1px_var(--rule)] outline-none focus-visible:ring-2 focus-visible:ring-ring/50',
        tone === 'error' ? 'border-l-destructive' : 'border-l-chart-blue',
      )}
    >
      <Icon aria-hidden className={cn('mt-0.5 size-5 shrink-0', tone === 'error' ? 'text-destructive' : 'text-chart-blue')} />
      <div className="grid gap-1">
        <p className="font-semibold">{title}</p>
        {children && <div className="text-sm text-muted-foreground [&_a]:font-medium [&_a]:text-chart-blue [&_a]:underline [&_a]:underline-offset-4">{children}</div>}
      </div>
    </div>
  )
}
