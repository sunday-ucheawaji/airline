import { useState, type ReactNode } from 'react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { FormAlert } from '@/components/form/FormAlert'
import { toApiError } from '@/lib/api'
import { cn } from '@/lib/utils'
import type { FlightStatus, PageResult } from './api'
import { STATUS_LABELS } from './format'

const STATUS_TONE: Record<FlightStatus, string> = {
  SCHEDULED: 'bg-chart-blue',
  DELAYED: 'bg-amber-500',
  BOARDING: 'bg-chart-blue',
  DEPARTED: 'bg-muted-foreground',
  IN_AIR: 'bg-muted-foreground',
  LANDED: 'bg-muted-foreground',
  ARRIVED: 'bg-muted-foreground',
  DIVERTED: 'bg-amber-500',
  CANCELLED: 'bg-destructive',
  COMPLETED: 'bg-border',
}

export function StatusLabel({ status }: { status: FlightStatus }) {
  return (
    <span className="inline-flex items-center gap-2 whitespace-nowrap">
      <span aria-hidden className={cn('size-2 rounded-full', STATUS_TONE[status])} />
      {STATUS_LABELS[status]}
    </span>
  )
}

/** A section's heading row: what it is, how many, and the action to add one (when the user may). */
export function SectionHeader({ title, description, action }: { title: string; description: ReactNode; action?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h2 className="font-display text-4xl">{title}</h2>
        <p className="mt-2 max-w-[62ch] text-muted-foreground">{description}</p>
      </div>
      {action}
    </div>
  )
}

/** Loading, failure and empty states for a list, so every panel says what's happening the same way. */
export function ListState({
  isLoading,
  error,
  isEmpty,
  empty,
  onRetry,
  children,
}: {
  isLoading: boolean
  error: unknown
  isEmpty: boolean
  empty: ReactNode
  onRetry: () => void
  children: ReactNode
}) {
  if (isLoading) {
    return (
      <p role="status" className="py-10 text-muted-foreground">
        Loading…
      </p>
    )
  }
  if (error) {
    return (
      <div className="grid justify-items-start gap-3 py-6">
        <FormAlert tone="error" title="This list couldn’t be loaded" focusOnMount={false}>
          {toApiError(error).message}
        </FormAlert>
        <Button variant="outline" onClick={onRetry}>
          Try again
        </Button>
      </div>
    )
  }
  if (isEmpty) return <div className="rounded-lg bg-card p-8 text-muted-foreground shadow-[0_0_0_1px_var(--rule)]">{empty}</div>
  return <>{children}</>
}

export function Pager<T>({ result, onPage }: { result: PageResult<T> | undefined; onPage: (page: number) => void }) {
  if (!result || result.totalPages <= 1) return null
  return (
    <nav aria-label="Pages" className="mt-4 flex items-center gap-3 text-sm">
      <Button variant="outline" size="sm" disabled={result.page === 0} onClick={() => onPage(result.page - 1)}>
        Previous
      </Button>
      <span className="text-muted-foreground">
        Page {result.page + 1} of {result.totalPages}
      </span>
      <Button
        variant="outline"
        size="sm"
        disabled={result.page + 1 >= result.totalPages}
        onClick={() => onPage(result.page + 1)}
      >
        Next
      </Button>
    </nav>
  )
}

/** Shared table styling: a ruled card, a quiet header row, rows that never overflow the page. */
export function DataTable({ caption, children }: { caption: string; children: ReactNode }) {
  return (
    <div className="overflow-x-auto rounded-lg bg-card shadow-[0_0_0_1px_var(--rule)]">
      <table className="w-full min-w-[44rem] text-left text-sm">
        <caption className="sr-only">{caption}</caption>
        {children}
      </table>
    </div>
  )
}

export const th = 'px-4 py-3 font-medium text-muted-foreground'
export const td = 'border-t px-4 py-3 align-top'

type ConfirmDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description: ReactNode
  confirmLabel: string
  pendingLabel: string
  destructive?: boolean
  onConfirm: () => Promise<unknown>
}

/** Asks before an action that can't be undone, and shows the server's reason in place if it's refused. */
export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  pendingLabel,
  destructive,
  onConfirm,
}: ConfirmDialogProps) {
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const changeOpen = (next: boolean) => {
    if (!next) setError(null)
    onOpenChange(next)
  }

  const confirm = async () => {
    setPending(true)
    setError(null)
    try {
      await onConfirm()
      onOpenChange(false)
    } catch (failure) {
      setError(toApiError(failure).message)
    } finally {
      setPending(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={changeOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription asChild>
            <div className="grid gap-2">{description}</div>
          </DialogDescription>
        </DialogHeader>
        {error && (
          <FormAlert tone="error" title="That didn’t work">
            {error}
          </FormAlert>
        )}
        <DialogFooter>
          <Button variant="outline" onClick={() => changeOpen(false)} disabled={pending}>
            Keep it
          </Button>
          <Button variant={destructive ? 'destructive' : 'default'} onClick={confirm} disabled={pending} aria-busy={pending}>
            {pending ? pendingLabel : confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

/** The labelled select every staff form uses, with its error under it. */
export function SelectField({
  id,
  label,
  error,
  hint,
  children,
  ...props
}: React.ComponentProps<'select'> & { id: string; label: string; error?: string; hint?: ReactNode }) {
  const describedBy = [error && `${id}-error`, hint && `${id}-hint`].filter(Boolean).join(' ') || undefined
  return (
    <div className="grid gap-2">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      <select
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className="h-11 w-full rounded-md border border-input bg-card px-3 text-base outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 aria-invalid:border-destructive md:text-sm"
        {...props}
      >
        {children}
      </select>
      {error && (
        <p id={`${id}-error`} className="text-sm text-destructive">
          {error}
        </p>
      )}
      {hint && (
        <p id={`${id}-hint`} className="text-sm text-muted-foreground">
          {hint}
        </p>
      )}
    </div>
  )
}
