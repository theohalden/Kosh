export const INTERESTS = ["expenses", "salary_budget", "investments", "net_worth", "curious"] as const;
export type Interest = (typeof INTERESTS)[number];

export const INTEREST_LABELS: Record<Interest, string> = {
  expenses: "Tracking expenses automatically",
  salary_budget: "Income and budgets",
  investments: "Investments and wealth",
  net_worth: "Net worth",
  curious: "Just curious",
};

/** Bump when the consent wording in EarlyAccess.tsx changes; stored with every signup. */
export const CONSENT_VERSION = "2026-11-tinward-v2";

const EMAIL_RE = /^[a-z0-9.!#$%&'*+/=?^_`{|}~-]{1,64}@(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,24}$/;

/** Trim, Unicode-normalise (NFKC), lowercase. Does not guess provider-specific aliasing. */
export function normalizeEmail(raw: string): string {
  return raw.normalize("NFKC").trim().toLowerCase();
}

export function isValidEmail(normalized: string): boolean {
  if (normalized.length > 254 || normalized.includes("..")) return false;
  const [local] = normalized.split("@");
  if (!local || local.startsWith(".") || local.endsWith(".")) return false;
  return EMAIL_RE.test(normalized);
}

export type SignupInput = { email: string; emailNormalized: string; interest: Interest | null; marketingConsent: boolean; honeypot: string };
export type ParseResult = { ok: true; value: SignupInput } | { ok: false; field: "email" | "interest" | "body"; message: string };

export function parseSignupBody(body: unknown): ParseResult {
  if (typeof body !== "object" || body === null || Array.isArray(body)) return { ok: false, field: "body", message: "Invalid request." };
  const b = body as Record<string, unknown>;
  if (typeof b.email !== "string" || b.email.trim() === "") return { ok: false, field: "email", message: "Enter your email address." };
  const emailNormalized = normalizeEmail(b.email);
  if (!isValidEmail(emailNormalized)) return { ok: false, field: "email", message: "That doesn’t look like a valid email address." };
  let interest: Interest | null = null;
  if (b.interest !== undefined && b.interest !== null && b.interest !== "") {
    if (typeof b.interest !== "string" || !(INTERESTS as readonly string[]).includes(b.interest)) return { ok: false, field: "interest", message: "Choose one of the listed options." };
    interest = b.interest as Interest;
  }
  return {
    ok: true,
    value: {
      email: b.email.normalize("NFKC").trim(),
      emailNormalized,
      interest,
      marketingConsent: b.marketingConsent === true,
      honeypot: typeof b.website === "string" ? b.website : "",
    },
  };
}
