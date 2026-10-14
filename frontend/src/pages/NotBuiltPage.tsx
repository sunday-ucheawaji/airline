import { Link } from 'react-router'

export function NotBuiltPage() {
  return (
    <section aria-labelledby="not-built-title" className="mx-auto max-w-6xl px-5 py-20 sm:px-8">
      <h1 id="not-built-title" className="font-display text-5xl">
        This page isn’t built yet
      </h1>
      <p className="mt-4 max-w-[52ch] text-muted-foreground">
        You can still search for flights or find an existing booking from the home page.
      </p>
      <Link to="/" className="mt-6 inline-block rounded-sm font-medium text-chart-blue underline underline-offset-4">
        Go to the home page
      </Link>
    </section>
  )
}
