# frontend

React + Vite + TypeScript + Tailwind CSS v4 + shadcn/ui (Radix base).

```bash
npm install
npm run dev      # http://localhost:5173
npm run build
npm run lint
```

The browser only talks to the API gateway (`localhost:5000`). In dev, Vite proxies `/api` to it, so calls use relative `/api/...` paths (`VITE_API_BASE_URL`, see `.env.example`). The gateway's CORS already allows `localhost:5173` for direct calls.

Add components with the shadcn CLI, e.g. `npx shadcn@latest add dialog`.
