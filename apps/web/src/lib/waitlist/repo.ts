import { randomUUID } from "node:crypto";
import { createClient, type SupabaseClient } from "@supabase/supabase-js";
import type { Interest } from "./validation";

export type Status = "pending" | "verified" | "unsubscribed";

export type Signup = {
  id: string;
  email: string;
  emailNormalized: string;
  interest: Interest | null;
  marketingConsent: boolean;
  marketingConsentAt: string | null;
  consentVersion: string;
  status: Status;
  verifyTokenHash: string | null;
  verifyExpiresAt: string | null;
  verificationSentAt: string | null;
  verifiedAt: string | null;
  invitedAt: string | null;
  unsubscribedAt: string | null;
  createdAt: string;
};

export type NewSignup = Pick<Signup, "email" | "emailNormalized" | "interest" | "marketingConsent" | "consentVersion" | "verifyTokenHash" | "verifyExpiresAt">;
export type Patch = Partial<Omit<Signup, "id" | "createdAt">>;

export interface WaitlistRepo {
  findByEmail(emailNormalized: string): Promise<Signup | null>;
  findById(id: string): Promise<Signup | null>;
  findByVerifyHash(hash: string): Promise<Signup | null>;
  /** Insert; if the normalised email already exists returns that row with created=false. */
  insert(input: NewSignup): Promise<{ created: boolean; signup: Signup }>;
  update(id: string, patch: Patch): Promise<Signup | null>;
  delete(id: string): Promise<boolean>;
  listAll(): Promise<Signup[]>;
  listInvitable(limit: number): Promise<Signup[]>;
  /** true while the caller is within `limit` hits per `windowSeconds`. */
  hit(bucket: string, keyHash: string, windowSeconds: number, limit: number): Promise<boolean>;
}

/* ───────────────────────── in-memory (development & tests only) ───────────────────────── */

export class MemoryRepo implements WaitlistRepo {
  rows = new Map<string, Signup>();
  hits = new Map<string, number>();

  async findByEmail(n: string) { return [...this.rows.values()].find((r) => r.emailNormalized === n) ?? null; }
  async findById(id: string) { return this.rows.get(id) ?? null; }
  async findByVerifyHash(h: string) { return [...this.rows.values()].find((r) => r.verifyTokenHash === h) ?? null; }
  async insert(input: NewSignup) {
    const existing = await this.findByEmail(input.emailNormalized);
    if (existing) return { created: false, signup: existing };
    const now = new Date().toISOString();
    const signup: Signup = {
      id: randomUUID(), ...input,
      marketingConsentAt: input.marketingConsent ? now : null,
      status: "pending", verificationSentAt: null, verifiedAt: null, invitedAt: null, unsubscribedAt: null, createdAt: now,
    };
    this.rows.set(signup.id, signup);
    return { created: true, signup };
  }
  async update(id: string, patch: Patch) {
    const row = this.rows.get(id);
    if (!row) return null;
    const next = { ...row, ...patch };
    this.rows.set(id, next);
    return next;
  }
  async delete(id: string) { return this.rows.delete(id); }
  async listAll() { return [...this.rows.values()].sort((a, b) => a.createdAt.localeCompare(b.createdAt)); }
  async listInvitable(limit: number) {
    return (await this.listAll()).filter((r) => r.status === "verified" && !r.invitedAt).sort((a, b) => (a.verifiedAt ?? "").localeCompare(b.verifiedAt ?? "")).slice(0, limit);
  }
  async hit(bucket: string, keyHash: string, windowSeconds: number, limit: number) {
    const w = Math.floor(Date.now() / 1000 / windowSeconds);
    const k = `${bucket}:${keyHash}:${w}`;
    const n = (this.hits.get(k) ?? 0) + 1;
    this.hits.set(k, n);
    return n <= limit;
  }
}

/* ───────────────────────── Supabase / Postgres ───────────────────────── */

type Row = {
  id: string; email: string; email_normalized: string; interest: Interest | null; marketing_consent: boolean; marketing_consent_at: string | null;
  consent_version: string; status: Status; verify_token_hash: string | null; verify_expires_at: string | null; verification_sent_at: string | null;
  verified_at: string | null; invited_at: string | null; unsubscribed_at: string | null; created_at: string;
};

const fromRow = (r: Row): Signup => ({
  id: r.id, email: r.email, emailNormalized: r.email_normalized, interest: r.interest, marketingConsent: r.marketing_consent, marketingConsentAt: r.marketing_consent_at,
  consentVersion: r.consent_version, status: r.status, verifyTokenHash: r.verify_token_hash, verifyExpiresAt: r.verify_expires_at, verificationSentAt: r.verification_sent_at,
  verifiedAt: r.verified_at, invitedAt: r.invited_at, unsubscribedAt: r.unsubscribed_at, createdAt: r.created_at,
});

const toColumns = (p: Patch): Record<string, unknown> => {
  const map: Record<string, string> = {
    email: "email", emailNormalized: "email_normalized", interest: "interest", marketingConsent: "marketing_consent", marketingConsentAt: "marketing_consent_at",
    consentVersion: "consent_version", status: "status", verifyTokenHash: "verify_token_hash", verifyExpiresAt: "verify_expires_at", verificationSentAt: "verification_sent_at",
    verifiedAt: "verified_at", invitedAt: "invited_at", unsubscribedAt: "unsubscribed_at",
  };
  return Object.fromEntries(Object.entries(p).map(([k, v]) => [map[k], v]));
};

export class SupabaseRepo implements WaitlistRepo {
  constructor(private db: SupabaseClient) {}
  private t() { return this.db.from("waitlist_signups"); }

  private async one(q: PromiseLike<{ data: Row | null; error: { message: string } | null }>) {
    const { data, error } = await q;
    if (error) throw new Error(`db: ${error.message}`);
    return data ? fromRow(data) : null;
  }
  findByEmail(n: string) { return this.one(this.t().select("*").eq("email_normalized", n).maybeSingle<Row>()); }
  findById(id: string) { return this.one(this.t().select("*").eq("id", id).maybeSingle<Row>()); }
  findByVerifyHash(h: string) { return this.one(this.t().select("*").eq("verify_token_hash", h).maybeSingle<Row>()); }

  async insert(input: NewSignup) {
    const now = new Date().toISOString();
    const { data, error } = await this.t()
      .insert({ email: input.email, email_normalized: input.emailNormalized, interest: input.interest, marketing_consent: input.marketingConsent,
        marketing_consent_at: input.marketingConsent ? now : null, consent_version: input.consentVersion, verify_token_hash: input.verifyTokenHash, verify_expires_at: input.verifyExpiresAt })
      .select("*").single<Row>();
    if (error) {
      if ((error as { code?: string }).code === "23505") {
        const existing = await this.findByEmail(input.emailNormalized);
        if (existing) return { created: false, signup: existing };
      }
      throw new Error(`db: ${error.message}`);
    }
    return { created: true, signup: fromRow(data) };
  }
  update(id: string, patch: Patch) { return this.one(this.t().update(toColumns(patch)).eq("id", id).select("*").maybeSingle<Row>()); }
  async delete(id: string) {
    const { data, error } = await this.t().delete().eq("id", id).select("id");
    if (error) throw new Error(`db: ${error.message}`);
    return (data?.length ?? 0) > 0;
  }
  async listAll() {
    const { data, error } = await this.t().select("*").order("created_at", { ascending: true }).returns<Row[]>();
    if (error) throw new Error(`db: ${error.message}`);
    return data.map(fromRow);
  }
  async listInvitable(limit: number) {
    const { data, error } = await this.t().select("*").eq("status", "verified").is("invited_at", null).order("verified_at", { ascending: true }).limit(limit).returns<Row[]>();
    if (error) throw new Error(`db: ${error.message}`);
    return data.map(fromRow);
  }
  async hit(bucket: string, keyHash: string, windowSeconds: number, limit: number) {
    const { data, error } = await this.db.rpc("rate_limit_hit", { p_bucket: bucket, p_key_hash: keyHash, p_window_seconds: windowSeconds, p_limit: limit });
    if (error) throw new Error(`db: ${error.message}`);
    return data === true;
  }
}

/* ───────────────────────── factory ───────────────────────── */

const g = globalThis as unknown as { __koshRepo?: WaitlistRepo };

export function getRepo(): WaitlistRepo {
  if (g.__koshRepo) return g.__koshRepo;
  const prod = process.env.NODE_ENV === "production";
  const driver = process.env.WAITLIST_DRIVER ?? (process.env.SUPABASE_URL ? "supabase" : "memory");
  if (driver === "memory") {
    if (prod) throw new Error("WAITLIST_DRIVER=memory is not allowed in production");
    g.__koshRepo = new MemoryRepo();
  } else {
    const url = process.env.SUPABASE_URL;
    const key = process.env.SUPABASE_SERVICE_ROLE_KEY;
    if (!url || !key) throw new Error("SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are required");
    g.__koshRepo = new SupabaseRepo(createClient(url, key, { auth: { persistSession: false, autoRefreshToken: false } }));
  }
  return g.__koshRepo;
}

/** Test seam. */
export function setRepo(repo: WaitlistRepo | undefined) { g.__koshRepo = repo; }
