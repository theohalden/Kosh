import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRepo, setRepo } from "@/lib/waitlist/repo";
import { setMailer, type Mail, type Mailer } from "@/lib/waitlist/email";
import { isValidEmail, normalizeEmail, parseSignupBody } from "@/lib/waitlist/validation";
import { readToken, signToken, sha256 } from "@/lib/waitlist/tokens";
import * as svc from "@/lib/waitlist/service";
import { POST as signupRoute } from "@/app/api/waitlist/route";
import { GET as verifyRoute } from "@/app/api/waitlist/verify/route";
import { POST as unsubRoute } from "@/app/api/waitlist/unsubscribe/route";
import { POST as deleteReqRoute } from "@/app/api/waitlist/delete/route";
import { POST as deleteConfirmRoute } from "@/app/api/waitlist/delete/confirm/route";
import { GET as exportRoute } from "@/app/api/admin/export/route";
import { POST as inviteRoute } from "@/app/api/admin/invite/route";

const ADMIN = "a".repeat(40);
let repo: MemoryRepo;
let sent: Mail[];
let failMail = false;

beforeEach(() => {
  repo = new MemoryRepo();
  sent = [];
  failMail = false;
  const mailer: Mailer = { async send(m) { if (failMail) throw new Error("boom"); sent.push(m); } };
  setRepo(repo);
  setMailer(mailer);
  vi.stubEnv("NEXT_PUBLIC_SITE_URL", "https://kosh.test");
  vi.stubEnv("ADMIN_API_TOKEN", ADMIN);
  vi.stubEnv("INVITE_URL", "https://play.example/beta");
  vi.stubEnv("TOKEN_SECRET", "t".repeat(40));
});

const post = (url: string, body: unknown, headers: Record<string, string> = {}) =>
  new Request(`https://kosh.test${url}`, { method: "POST", headers: { "content-type": "application/json", "x-forwarded-for": "1.2.3.4", ...headers }, body: JSON.stringify(body) });
const tokenFrom = (m: Mail) => /token=([\w-]+)/.exec(m.text)![1];

describe("validation", () => {
  it("normalises and validates emails", () => {
    expect(normalizeEmail("  Asha.K@Example.COM ")).toBe("asha.k@example.com");
    for (const ok of ["a@b.co", "first.last+tag@sub.example.in"]) expect(isValidEmail(ok)).toBe(true);
    for (const bad of ["", "a@b", "a@@b.com", "a b@c.com", ".a@b.com", "a..b@c.com", "a@b..com", "x".repeat(260) + "@a.com"]) expect(isValidEmail(bad)).toBe(false);
  });
  it("rejects bad bodies and unknown interests", () => {
    expect(parseSignupBody(null).ok).toBe(false);
    expect(parseSignupBody({ email: "a@b.co", interest: "stocks" }).ok).toBe(false);
    const ok = parseSignupBody({ email: "A@B.co", interest: "investments", marketingConsent: true });
    expect(ok.ok && ok.value.marketingConsent && ok.value.emailNormalized).toBe("a@b.co");
    const noConsent = parseSignupBody({ email: "a@b.co", marketingConsent: "yes" });
    expect(noConsent.ok && noConsent.value.marketingConsent).toBe(false); // only literal true counts
  });
});

describe("signed tokens", () => {
  const id = "123e4567-e89b-12d3-a456-426614174000";
  it("round-trips, detects tampering and expiry", () => {
    const t = signToken("delete", id, 60, 1_000_000);
    expect(readToken("delete", t, 1_000_000)).toEqual({ id });
    expect(readToken("unsubscribe", t, 1_000_000)).toEqual({ error: "invalid" }); // purpose-bound
    expect(readToken("delete", t.slice(0, -2) + "xx")).toEqual({ error: "invalid" });
    expect(readToken("delete", t, 1_000_000 + 61_000)).toEqual({ error: "expired" });
    expect(readToken("unsubscribe", signToken("unsubscribe", id), Date.now() + 9e11)).toEqual({ id }); // no expiry
  });
});

describe("signup → verify flow (service)", () => {
  it("creates a pending signup, emails a link, verifies once, then reports duplicates", async () => {
    const input = parseSignupBody({ email: "Asha@Example.com", interest: "salary_budget", marketingConsent: true });
    if (!input.ok) throw new Error("parse");
    expect(await svc.signup(input.value, "9.9.9.9")).toEqual({ status: "check_email" });
    expect(sent).toHaveLength(1);
    expect(sent[0].to).toBe("Asha@Example.com");
    const row = (await repo.findByEmail("asha@example.com"))!;
    expect(row).toMatchObject({ status: "pending", marketingConsent: true, interest: "salary_budget" });
    expect(row.marketingConsentAt).toBeTruthy();
    expect(row.consentVersion).toBeTruthy();

    const token = tokenFrom(sent[0]);
    expect(row.verifyTokenHash).toBe(sha256(token)); // only the hash is stored
    expect(JSON.stringify(row)).not.toContain(token);

    expect(await svc.verify(token)).toBe("verified");
    expect((await repo.findByEmail("asha@example.com"))!.status).toBe("verified");
    expect(await svc.verify(token)).toBe("invalid"); // single use
    expect(await svc.signup(input.value, "9.9.9.9")).toEqual({ status: "already_registered" });
    expect(sent).toHaveLength(1);
  });

  it("deduplicates on normalised email and throttles repeat sends", async () => {
    const a = parseSignupBody({ email: "dup@example.com" }); const b = parseSignupBody({ email: "  DUP@example.com " });
    if (!a.ok || !b.ok) throw new Error();
    await svc.signup(a.value, "1.1.1.1");
    expect(await svc.signup(b.value, "1.1.1.1")).toEqual({ status: "resent_recently" });
    expect(repo.rows.size).toBe(1);
    expect(sent).toHaveLength(1);
  });

  it("re-sends a fresh link after the cooldown and invalidates the old one", async () => {
    const a = parseSignupBody({ email: "later@example.com" });
    if (!a.ok) throw new Error();
    let t = 1_700_000_000_000;
    const deps = { repo, mailer: () => ({ send: async (m: Mail) => { sent.push(m); } }), now: () => t };
    await svc.signup(a.value, "1.1.1.1", deps);
    t += 61_000;
    expect(await svc.signup(a.value, "1.1.1.1", deps)).toEqual({ status: "check_email" });
    expect(sent).toHaveLength(2);
    expect(await svc.verify(tokenFrom(sent[0]), deps)).toBe("invalid");
    expect(await svc.verify(tokenFrom(sent[1]), deps)).toBe("verified");
  });

  it("expires verification links after 48h", async () => {
    const a = parseSignupBody({ email: "slow@example.com" });
    if (!a.ok) throw new Error();
    let t = 1_700_000_000_000;
    const deps = { repo, mailer: () => ({ send: async (m: Mail) => { sent.push(m); } }), now: () => t };
    await svc.signup(a.value, "1.1.1.1", deps);
    t += 49 * 3600 * 1000;
    expect(await svc.verify(tokenFrom(sent[0]), deps)).toBe("expired");
  });

  it("rate limits per IP", async () => {
    let last = "";
    for (let i = 0; i < 10; i++) {
      const r = parseSignupBody({ email: `u${i}@example.com` });
      if (!r.ok) throw new Error();
      last = (await svc.signup(r.value, "7.7.7.7")).status;
    }
    expect(last).toBe("rate_limited");
    expect(repo.rows.size).toBe(8);
  });

  it("keeps the row but reports email_failed when the mailer throws", async () => {
    failMail = true;
    const r = parseSignupBody({ email: "fail@example.com" });
    if (!r.ok) throw new Error();
    expect(await svc.signup(r.value, "2.2.2.2")).toEqual({ status: "email_failed" });
    expect(repo.rows.size).toBe(1);
  });
});

describe("unsubscribe & deletion", () => {
  async function verified(email: string) {
    const r = parseSignupBody({ email, marketingConsent: true });
    if (!r.ok) throw new Error();
    await svc.signup(r.value, "3.3.3.3");
    await svc.verify(tokenFrom(sent.at(-1)!));
    return (await repo.findByEmail(email))!;
  }
  it("unsubscribes and clears marketing consent", async () => {
    const row = await verified("u@example.com");
    expect(await svc.unsubscribe(signToken("unsubscribe", row.id))).toBe("ok");
    expect(await repo.findById(row.id)).toMatchObject({ status: "unsubscribed", marketingConsent: false, marketingConsentAt: null });
    expect(await svc.unsubscribe("garbage")).toBe("invalid");
  });
  it("deletion requires emailed confirmation and gives no existence oracle", async () => {
    const row = await verified("gone@example.com");
    const before = sent.length;
    expect(await svc.requestDeletion("nobody@example.com", "4.4.4.4")).toBe("accepted");
    expect(sent.length).toBe(before); // nothing sent for unknown address, same answer
    expect(await svc.requestDeletion("GONE@example.com", "4.4.4.4")).toBe("accepted");
    expect(sent.length).toBe(before + 1);
    const link = /token=([^\s]+)/.exec(sent.at(-1)!.text)![1];
    expect(await repo.findById(row.id)).not.toBeNull(); // still there until confirmed
    expect(await svc.confirmDeletion(decodeURIComponent(link))).toBe("ok");
    expect(await repo.findById(row.id)).toBeNull();
    expect(await svc.confirmDeletion(signToken("unsubscribe", row.id))).toBe("invalid"); // wrong purpose
  });
});

describe("HTTP routes", () => {
  it("POST /api/waitlist: 200 on success, 422 on bad email, 415 on wrong content-type, 403 cross-origin", async () => {
    const ok = await signupRoute(post("/api/waitlist", { email: "route@example.com", interest: "net_worth" }));
    expect(ok.status).toBe(200);
    expect(await ok.json()).toEqual({ status: "check_email" });
    const bad = await signupRoute(post("/api/waitlist", { email: "nope" }));
    expect(bad.status).toBe(422);
    expect(await bad.json()).toMatchObject({ field: "email" });
    const ct = await signupRoute(new Request("https://kosh.test/api/waitlist", { method: "POST", headers: { "content-type": "text/plain" }, body: "email=a@b.co" }));
    expect(ct.status).toBe(415);
    const xs = await signupRoute(post("/api/waitlist", { email: "x@example.com" }, { origin: "https://evil.example" }));
    expect(xs.status).toBe(403);
    const same = await signupRoute(post("/api/waitlist", { email: "same@example.com" }, { origin: "https://kosh.test" }));
    expect(same.status).toBe(200);
  });
  it("honeypot returns success but stores and sends nothing", async () => {
    const r = await signupRoute(post("/api/waitlist", { email: "bot@example.com", website: "http://spam" }));
    expect(r.status).toBe(200);
    expect(repo.rows.size).toBe(0);
    expect(sent).toHaveLength(0);
  });
  it("returns 429 with Retry-After when limited", async () => {
    let res!: Response;
    for (let i = 0; i < 9; i++) res = await signupRoute(post("/api/waitlist", { email: `r${i}@example.com` }));
    expect(res.status).toBe(429);
    expect(res.headers.get("retry-after")).toBeTruthy();
  });
  it("returns 503 when the email can't be sent", async () => {
    failMail = true;
    const r = await signupRoute(post("/api/waitlist", { email: "mailfail@example.com" }));
    expect(r.status).toBe(503);
  });
  it("verify redirects to /confirmed with the outcome", async () => {
    await signupRoute(post("/api/waitlist", { email: "v@example.com" }));
    const res = await verifyRoute(new Request(`https://kosh.test/api/waitlist/verify?token=${tokenFrom(sent[0])}`));
    expect(res.status).toBe(303);
    expect(res.headers.get("location")).toBe("https://kosh.test/confirmed?r=verified");
    const bad = await verifyRoute(new Request("https://kosh.test/api/waitlist/verify?token=nope"));
    expect(bad.headers.get("location")).toBe("https://kosh.test/confirmed?r=invalid");
  });
  it("unsubscribe + deletion endpoints", async () => {
    await signupRoute(post("/api/waitlist", { email: "e2e@example.com" }));
    const id = (await repo.findByEmail("e2e@example.com"))!.id;
    expect((await unsubRoute(post("/api/waitlist/unsubscribe", { token: signToken("unsubscribe", id) }))).status).toBe(200);
    expect((await unsubRoute(post("/api/waitlist/unsubscribe", { token: "bad" }))).status).toBe(400);
    // RFC 8058 one-click form post
    const oc = await unsubRoute(new Request(`https://kosh.test/api/waitlist/unsubscribe?token=${signToken("unsubscribe", id)}`, { method: "POST", body: "List-Unsubscribe=One-Click", headers: { "content-type": "application/x-www-form-urlencoded" } }));
    expect(oc.status).toBe(200);
    expect((await deleteReqRoute(post("/api/waitlist/delete", { email: "e2e@example.com" }))).status).toBe(200);
    const link = decodeURIComponent(/token=([^\s]+)/.exec(sent.at(-1)!.text)![1]);
    expect((await deleteConfirmRoute(post("/api/waitlist/delete/confirm", { token: link }))).status).toBe(200);
    expect(await repo.findById(id)).toBeNull();
  });
});

describe("admin endpoints", () => {
  const auth = (extra: Record<string, string> = {}) => ({ authorization: `Bearer ${ADMIN}`, ...extra });
  it("are closed without the bearer token, and disabled when unconfigured", async () => {
    expect((await exportRoute(new Request("https://kosh.test/api/admin/export"))).status).toBe(401);
    expect((await exportRoute(new Request("https://kosh.test/api/admin/export", { headers: { authorization: "Bearer wrong" } }))).status).toBe(401);
    vi.stubEnv("ADMIN_API_TOKEN", "");
    expect((await exportRoute(new Request("https://kosh.test/api/admin/export", { headers: auth() }))).status).toBe(404);
  });
  it("export CSV neutralises formula injection and omits secrets", async () => {
    const r = parseSignupBody({ email: "=cmd@example.com" });
    // "=cmd@example.com" is a syntactically valid local-part; ensure the cell is neutralised on the way out.
    if (r.ok) await svc.signup(r.value, "5.5.5.5");
    const ok = parseSignupBody({ email: "fine@example.com", marketingConsent: true });
    if (!ok.ok) throw new Error();
    await svc.signup(ok.value, "5.5.5.5");
    const res = await exportRoute(new Request("https://kosh.test/api/admin/export", { headers: auth() }));
    expect(res.status).toBe(200);
    const csv = await res.text();
    expect(csv.split("\n")[0]).toContain("email");
    expect(csv).toContain("fine@example.com");
    expect(csv).not.toMatch(/(^|,)=cmd/m);
    expect(csv).not.toContain("verify_token");
  });
  it("invite only reaches verified, subscribed, not-yet-invited people, oldest first, once", async () => {
    for (const e of ["a@example.com", "b@example.com", "c@example.com"]) {
      const r = parseSignupBody({ email: e }); if (!r.ok) throw new Error();
      await svc.signup(r.value, "6.6.6.6");
      if (e !== "c@example.com") await svc.verify(tokenFrom(sent.at(-1)!)); // c stays pending
    }
    const bId = (await repo.findByEmail("b@example.com"))!.id;
    await svc.unsubscribe(signToken("unsubscribe", bId));
    sent.length = 0;
    const res = await inviteRoute(post("/api/admin/invite", { limit: 10 }, auth()));
    expect(await res.json()).toEqual({ sent: 1, failed: 0 });
    expect(sent.map((m) => m.to)).toEqual(["a@example.com"]);
    expect(sent[0].text).toContain("https://play.example/beta");
    expect(sent[0].headers?.["List-Unsubscribe"]).toContain("/unsubscribe?token=");
    expect((await (await inviteRoute(post("/api/admin/invite", { limit: 10 }, auth()))).json())).toEqual({ sent: 0, failed: 0 });
    expect((await inviteRoute(post("/api/admin/invite", { limit: 0 }, auth()))).status).toBe(422);
  });
});
