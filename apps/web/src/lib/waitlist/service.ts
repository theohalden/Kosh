import { getRepo, type Signup, type WaitlistRepo } from "./repo";
import { deletionEmail, getMailer, invitationEmail, siteUrl, verificationEmail, type Mailer } from "./email";
import { keyHash, newToken, readToken, sha256, signToken } from "./tokens";
import { CONSENT_VERSION, normalizeEmail, isValidEmail, type SignupInput } from "./validation";

const VERIFY_TTL_MS = 48 * 3600 * 1000;
const RESEND_COOLDOWN_MS = 60 * 1000;
const DELETE_TTL_S = 24 * 3600;

export type Deps = { repo: WaitlistRepo; mailer: () => Mailer; now: () => number };
const defaults = (): Deps => ({ repo: getRepo(), mailer: getMailer, now: Date.now });

export type SignupResult =
  | { status: "check_email" }            // new (or still-unverified) address, verification mail sent
  | { status: "already_registered" }     // verified already
  | { status: "resent_recently" }        // pending, but a mail went out < 60 s ago
  | { status: "rate_limited" }
  | { status: "email_failed" };          // saved, but the email couldn't be sent

export async function signup(input: SignupInput, ip: string, deps: Deps = defaults()): Promise<SignupResult> {
  const { repo, now } = deps;
  if (!(await repo.hit("signup:ip", keyHash(ip), 3600, 8))) return { status: "rate_limited" };

  const token = newToken();
  const expires = new Date(now() + VERIFY_TTL_MS).toISOString();
  let { created, signup: row } = await repo.insert({
    email: input.email, emailNormalized: input.emailNormalized, interest: input.interest, marketingConsent: input.marketingConsent,
    consentVersion: CONSENT_VERSION, verifyTokenHash: sha256(token), verifyExpiresAt: expires,
  });

  if (!created) {
    if (row.status === "verified") return { status: "already_registered" };
    if (row.status === "unsubscribed") {
      // Re-joining is an explicit new request: reset to pending and re-verify.
      row = (await repo.update(row.id, { status: "pending", unsubscribedAt: null, verifyTokenHash: sha256(token), verifyExpiresAt: expires, marketingConsent: input.marketingConsent, marketingConsentAt: input.marketingConsent ? new Date(now()).toISOString() : null, interest: input.interest ?? row.interest, consentVersion: CONSENT_VERSION })) ?? row;
    } else {
      const last = row.verificationSentAt ? Date.parse(row.verificationSentAt) : 0;
      if (now() - last < RESEND_COOLDOWN_MS) return { status: "resent_recently" };
      if (!(await repo.hit("signup:email", keyHash(input.emailNormalized), 3600, 3))) return { status: "rate_limited" };
      row = (await repo.update(row.id, { verifyTokenHash: sha256(token), verifyExpiresAt: expires })) ?? row;
    }
  }

  try {
    await deps.mailer().send(verificationEmail(row.email, `${siteUrl()}/api/waitlist/verify?token=${token}`));
    await repo.update(row.id, { verificationSentAt: new Date(now()).toISOString() });
    return { status: "check_email" };
  } catch (err) {
    console.error("[waitlist] verification email failed:", err instanceof Error ? err.message : err);
    return { status: "email_failed" };
  }
}

export type VerifyResult = "verified" | "already_verified" | "expired" | "invalid";

export async function verify(token: string, deps: Deps = defaults()): Promise<VerifyResult> {
  if (!token || token.length > 128) return "invalid";
  const row = await deps.repo.findByVerifyHash(sha256(token));
  if (!row) return "invalid";
  if (row.verifyExpiresAt && Date.parse(row.verifyExpiresAt) < deps.now()) return "expired";
  if (row.status === "verified") return "already_verified";
  await deps.repo.update(row.id, { status: "verified", verifiedAt: new Date(deps.now()).toISOString(), verifyTokenHash: null, verifyExpiresAt: null });
  return "verified";
}

export type TokenOutcome = "ok" | "invalid" | "expired" | "not_found";

export async function unsubscribe(token: string, deps: Deps = defaults()): Promise<TokenOutcome> {
  const t = readToken("unsubscribe", token, deps.now());
  if ("error" in t) return t.error;
  const row = await deps.repo.findById(t.id);
  if (!row) return "not_found";
  if (row.status !== "unsubscribed") await deps.repo.update(row.id, { status: "unsubscribed", unsubscribedAt: new Date(deps.now()).toISOString(), marketingConsent: false, marketingConsentAt: null });
  return "ok";
}

/** Always resolves the same way for the caller so the form can't be used to probe who is on the list. */
export async function requestDeletion(rawEmail: string, ip: string, deps: Deps = defaults()): Promise<"accepted" | "rate_limited" | "invalid"> {
  const email = normalizeEmail(rawEmail);
  if (!isValidEmail(email)) return "invalid";
  if (!(await deps.repo.hit("delete:ip", keyHash(ip), 3600, 5))) return "rate_limited";
  const row = await deps.repo.findByEmail(email);
  if (row && (await deps.repo.hit("delete:email", keyHash(email), 3600, 2))) {
    try {
      const token = signToken("delete", row.id, DELETE_TTL_S, deps.now());
      await deps.mailer().send(deletionEmail(row.email, `${siteUrl()}/delete?token=${encodeURIComponent(token)}`));
    } catch (err) { console.error("[waitlist] deletion email failed:", err instanceof Error ? err.message : err); }
  }
  return "accepted";
}

export async function confirmDeletion(token: string, deps: Deps = defaults()): Promise<TokenOutcome> {
  const t = readToken("delete", token, deps.now());
  if ("error" in t) return t.error;
  return (await deps.repo.delete(t.id)) ? "ok" : "not_found";
}

export async function invite(limit: number, deps: Deps = defaults()): Promise<{ sent: number; failed: number }> {
  const inviteUrl = process.env.INVITE_URL;
  if (!inviteUrl) throw new Error("INVITE_URL is not configured");
  const rows = await deps.repo.listInvitable(Math.min(Math.max(limit, 1), 200));
  let sent = 0, failed = 0;
  for (const r of rows) {
    try {
      const unsub = `${siteUrl()}/unsubscribe?token=${encodeURIComponent(signToken("unsubscribe", r.id))}`;
      await deps.mailer().send(invitationEmail(r.email, inviteUrl, unsub));
      await deps.repo.update(r.id, { invitedAt: new Date(deps.now()).toISOString() });
      sent++;
    } catch (err) { failed++; console.error("[waitlist] invite failed:", err instanceof Error ? err.message : err); }
  }
  return { sent, failed };
}

const csvCell = (v: unknown) => {
  let s = v === null || v === undefined ? "" : String(v);
  if (/^[=+\-@\t\r]/.test(s)) s = `'${s}`;          // neutralise spreadsheet formula injection
  return /[",\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
};

export async function exportCsv(deps: Deps = defaults()): Promise<string> {
  const rows: Signup[] = await deps.repo.listAll();
  const head = ["id", "email", "interest", "status", "marketing_consent", "marketing_consent_at", "consent_version", "created_at", "verified_at", "invited_at", "unsubscribed_at"];
  const lines = rows.map((r) => [r.id, r.email, r.interest, r.status, r.marketingConsent, r.marketingConsentAt, r.consentVersion, r.createdAt, r.verifiedAt, r.invitedAt, r.unsubscribedAt].map(csvCell).join(","));
  return [head.join(","), ...lines].join("\n") + "\n";
}
