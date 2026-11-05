# Tinward website

The Next.js marketing website and early-access backend live here, as the `@tinward/web` npm workspace. The native finance application is a separate project in `../android`.

## Local development

Run from the repository root:

```bash
npm ci
cp apps/web/.env.example apps/web/.env.local
npm run dev
```

Do not overwrite an existing `.env.local`. The server runs at http://localhost:3001. The root lockfile owns dependency versions; do not create an app-local lockfile or run a second package manager here.

- `npm run check:web` runs type checks, ESLint, Vitest, the local PostgreSQL migration checks and the production build.
- `npm run build:web` builds this app.
- `npm run start:web` serves a previously built production app. Configure production services before using signup in production.

## Application boundaries

- `src/content`: synthetic product figures, marketing copy and proposed plan definitions.
- `src/lib/format.ts`: locale-aware illustrative amount formatting, independent of billing prices.
- `src/components`: marketing sections and interactive concept screens.
- `src/app/api`: waitlist registration, verification, unsubscribe/deletion and optional admin routes.
- `supabase/migrations`: persistence constraints and rate-limit functions.
- `tests`: synthetic-data arithmetic and waitlist behavior.
- `scripts`: app-specific development and verification utilities. Browser scripts are optional utilities, not part of CI.

## Configuration

Local development defaults to `WAITLIST_DRIVER=memory`. Signups are lost when the development process restarts. With no Resend credentials, email is captured by the development outbox at `/api/dev/outbox`; nothing is sent externally. This endpoint is disabled in production.

For production, configure `WAITLIST_DRIVER=supabase`, `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `RESEND_API_KEY`, a verified `EMAIL_FROM`, a `TOKEN_SECRET` of at least 32 characters, and the real `NEXT_PUBLIC_SITE_URL`. Apply the SQL migrations to the intended Supabase project before accepting signups. All service credentials must stay server-side. `ADMIN_API_TOKEN` is optional and enables authenticated admin endpoints; `INVITE_URL` is needed before sending invitations.

Generate a token secret with `openssl rand -hex 32`. Do not copy local development secrets into production. `tinward.app` is a proposed domain, not a deployment setting established here.

Product screenshots are synthetic concepts. Current Android message parsing is regional; budgets, investments and net worth must remain clearly identified as planned. The website must not access real Android financial records.
