import { createHash, createHmac, randomBytes, timingSafeEqual } from "node:crypto";

export const sha256 = (s: string) => createHash("sha256").update(s).digest("hex");

/** 256-bit URL-safe random token. Only its SHA-256 is stored. */
export const newToken = () => randomBytes(32).toString("base64url");

export function safeEqual(a: string, b: string): boolean {
  const ha = createHash("sha256").update(a).digest();
  const hb = createHash("sha256").update(b).digest();
  return timingSafeEqual(ha, hb);
}

function secret(): string {
  const s = process.env.TOKEN_SECRET;
  if (s && s.length >= 32) return s;
  if (process.env.NODE_ENV === "production") throw new Error("TOKEN_SECRET (>=32 chars) is required in production");
  return "dev-only-insecure-secret-dev-only-insecure-secret";
}

export type SignedPurpose = "unsubscribe" | "delete";

/** Stateless signed link token: `<id>.<expiryEpoch>.<hmac>`; unsubscribe tokens never expire (expiry = 0). */
export function signToken(purpose: SignedPurpose, id: string, ttlSeconds = 0, now = Date.now()): string {
  const exp = ttlSeconds > 0 ? Math.floor(now / 1000) + ttlSeconds : 0;
  const mac = createHmac("sha256", secret()).update(`${purpose}.${id}.${exp}`).digest("base64url");
  return `${id}.${exp}.${mac}`;
}

export function readToken(purpose: SignedPurpose, token: string, now = Date.now()): { id: string } | { error: "invalid" | "expired" } {
  const parts = token.split(".");
  if (parts.length !== 3) return { error: "invalid" };
  const [id, expStr, mac] = parts;
  if (!/^[0-9a-f-]{36}$/.test(id) || !/^\d+$/.test(expStr)) return { error: "invalid" };
  const expected = createHmac("sha256", secret()).update(`${purpose}.${id}.${expStr}`).digest("base64url");
  if (!safeEqual(mac, expected)) return { error: "invalid" };
  const exp = Number(expStr);
  if (exp !== 0 && exp < Math.floor(now / 1000)) return { error: "expired" };
  return { id };
}

/** Salted hash for rate-limit keys so raw IPs and emails are never stored. */
export const keyHash = (value: string) => createHmac("sha256", secret()).update(`rl.${value}`).digest("hex").slice(0, 32);
