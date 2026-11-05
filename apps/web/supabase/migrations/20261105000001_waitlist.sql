-- Kosh early-access waitlist.
-- Only the server (Supabase service role) ever touches these tables: RLS is on with NO policies,
-- and all privileges are revoked from the anon/authenticated roles that the browser could use.

create table if not exists public.waitlist_signups (
  id                   uuid primary key default gen_random_uuid(),
  email                text        not null check (char_length(email) between 3 and 254),
  email_normalized     text        not null check (char_length(email_normalized) between 3 and 254),
  interest             text        check (interest in ('expenses','salary_budget','investments','net_worth','curious')),
  marketing_consent    boolean     not null default false,
  marketing_consent_at timestamptz,
  consent_version      text        not null,
  status               text        not null default 'pending' check (status in ('pending','verified','unsubscribed')),
  verify_token_hash    text,
  verify_expires_at    timestamptz,
  verification_sent_at timestamptz,
  verified_at          timestamptz,
  invited_at           timestamptz,
  unsubscribed_at      timestamptz,
  created_at           timestamptz not null default now(),
  updated_at           timestamptz not null default now(),
  constraint consent_has_timestamp check (marketing_consent = false or marketing_consent_at is not null)
);

create unique index if not exists waitlist_signups_email_normalized_key on public.waitlist_signups (email_normalized);
create unique index if not exists waitlist_signups_verify_token_key on public.waitlist_signups (verify_token_hash) where verify_token_hash is not null;
create index if not exists waitlist_signups_invitable_idx on public.waitlist_signups (verified_at) where status = 'verified' and invited_at is null;

create or replace function public.touch_updated_at() returns trigger language plpgsql as $$
begin new.updated_at = now(); return new; end $$;

drop trigger if exists waitlist_signups_touch on public.waitlist_signups;
create trigger waitlist_signups_touch before update on public.waitlist_signups
  for each row execute function public.touch_updated_at();

-- Fixed-window rate limiting. Keys are salted hashes of IPs / emails, never raw values.
create table if not exists public.rate_limit_hits (
  bucket       text        not null,
  key_hash     text        not null,
  window_start timestamptz not null,
  hits         integer     not null default 0,
  primary key (bucket, key_hash, window_start)
);

-- Atomically count a hit; returns true while the caller is within the limit.
create or replace function public.rate_limit_hit(p_bucket text, p_key_hash text, p_window_seconds integer, p_limit integer)
returns boolean language plpgsql as $$
declare
  w timestamptz := to_timestamp(floor(extract(epoch from now()) / p_window_seconds) * p_window_seconds);
  n integer;
begin
  insert into public.rate_limit_hits (bucket, key_hash, window_start, hits)
  values (p_bucket, p_key_hash, w, 1)
  on conflict (bucket, key_hash, window_start) do update set hits = public.rate_limit_hits.hits + 1
  returning hits into n;
  -- opportunistic cleanup keeps the table tiny
  if random() < 0.02 then
    delete from public.rate_limit_hits where window_start < now() - interval '2 days';
  end if;
  return n <= p_limit;
end $$;

alter table public.waitlist_signups enable row level security;
alter table public.rate_limit_hits  enable row level security;

do $$
begin
  if exists (select 1 from pg_roles where rolname = 'anon') then
    revoke all on public.waitlist_signups, public.rate_limit_hits from anon;
    revoke execute on function public.rate_limit_hit(text, text, integer, integer) from anon;
  end if;
  if exists (select 1 from pg_roles where rolname = 'authenticated') then
    revoke all on public.waitlist_signups, public.rate_limit_hits from authenticated;
    revoke execute on function public.rate_limit_hit(text, text, integer, integer) from authenticated;
  end if;
end $$;
revoke execute on function public.rate_limit_hit(text, text, integer, integer) from public;
