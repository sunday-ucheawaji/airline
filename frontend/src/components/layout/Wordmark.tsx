import { Link } from 'react-router'
import { cn } from '@/lib/utils'

export function Wordmark({ className }: { className?: string }) {
  return (
    <Link to="/" className={cn('inline-flex items-center gap-2 rounded-md', className)}>
      <svg aria-hidden viewBox="0 0 32 20" className="h-4 w-auto text-chart-blue">
        <path d="M2 18 L26 3" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />
        <path d="M2 12 L19 1.5" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" opacity="0.45" />
        <circle cx="28.5" cy="2" r="2" fill="currentColor" />
      </svg>
      <span className="font-display text-2xl">Contrail</span>
    </Link>
  )
}
