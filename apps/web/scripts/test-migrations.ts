/**
 * Applies the real SQL migration to an in-process PostgreSQL (PGlite) and checks the constraints,
 * trigger, RLS flags and the atomic rate-limit function the Supabase driver depends on.
 * Run: npm run test:db
 */
import { PGlite } from "@electric-sql/pglite";
import { readFileSync, readdirSync } from "node:fs";
import path from "node:path";
import assert from "node:assert/strict";

const dir = path.join(process.cwd(), "supabase", "migrations");
const db = new PGlite();
// Supabase provides these roles; create stand-ins so the REVOKE branch is exercised.
await db.exec("create role anon nologin; create role authenticated nologin;");
for (const f of readdirSync(dir).filter((x) => x.endsWith(".sql")).sort()) await db.exec(readFileSync(path.join(dir, f), "utf8"));
// Idempotent re-run
for (const f of readdirSync(dir).filter((x) => x.endsWith(".sql")).sort()) await db.exec(readFileSync(path.join(dir, f), "utf8"));

const ins = (email: string, extra = "") =>
  db.query(`insert into waitlist_signups (email, email_normalized, consent_version ${extra ? "," + extra.split("|")[0] : ""}) values ($1, $1, 'v1' ${extra ? "," + extra.split("|")[1] : ""})`, [email]);

await ins("a@example.com");
await assert.rejects(ins("a@example.com"), /duplicate key|unique/i, "normalised email must be unique");
await assert.rejects(ins("b@example.com", "interest|'stocks'"), /check/i, "interest enum enforced");
await assert.rejects(ins("c@example.com", "status|'weird'"), /check/i, "status enum enforced");
await assert.rejects(ins("d@example.com", "marketing_consent|true"), /consent_has_timestamp|check/i, "consent needs a timestamp");
await ins("e@example.com", "marketing_consent,marketing_consent_at|true,now()");

const before = (await db.query<{ updated_at: string }>("select updated_at from waitlist_signups where email='a@example.com'")).rows[0].updated_at;
await new Promise((r) => setTimeout(r, 15));
await db.query("update waitlist_signups set status='verified' where email='a@example.com'");
const after = (await db.query<{ updated_at: string }>("select updated_at from waitlist_signups where email='a@example.com'")).rows[0].updated_at;
assert.ok(new Date(after) > new Date(before), "updated_at trigger fires");

const rls = await db.query<{ relname: string; relrowsecurity: boolean }>("select relname, relrowsecurity from pg_class where relname in ('waitlist_signups','rate_limit_hits')");
assert.equal(rls.rows.length, 2);
assert.ok(rls.rows.every((r) => r.relrowsecurity), "RLS enabled on both tables");
const pol = await db.query("select * from pg_policies where tablename in ('waitlist_signups','rate_limit_hits')");
assert.equal(pol.rows.length, 0, "no policies: browser roles get nothing");
const grants = await db.query("select grantee from information_schema.role_table_grants where table_name in ('waitlist_signups','rate_limit_hits') and grantee in ('anon','authenticated')");
assert.equal(grants.rows.length, 0, "anon/authenticated have no table privileges");

const results: boolean[] = [];
for (let i = 0; i < 5; i++) results.push((await db.query<{ rate_limit_hit: boolean }>("select rate_limit_hit('t','k',3600,3)")).rows[0].rate_limit_hit);
assert.deepEqual(results, [true, true, true, false, false], "limit of 3 per window");
assert.equal((await db.query<{ rate_limit_hit: boolean }>("select rate_limit_hit('t','other',3600,3)")).rows[0].rate_limit_hit, true, "keys are independent");

console.log("migration OK: constraints, trigger, RLS, grants, rate-limit function verified on PostgreSQL (PGlite)");
await db.close();
