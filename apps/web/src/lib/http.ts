import { NextResponse } from "next/server";
import { siteUrl } from "./waitlist/email";
import { safeEqual } from "./waitlist/tokens";

export const json = (body: unknown, status = 200, headers?: Record<string, string>) =>
  NextResponse.json(body, { status, headers: { "Cache-Control": "no-store", ...headers } });

/** First hop of x-forwarded-for (set by Vercel/most proxies); falls back to a constant so limits still apply. */
export function clientIp(req: Request): string {
  const xff = req.headers.get("x-forwarded-for");
  return (xff?.split(",")[0]?.trim() || req.headers.get("x-real-ip") || "unknown").slice(0, 64);
}

/** Reject cross-site browser POSTs. Requests without Origin (curl, server-to-server) are allowed; they carry no cookies to abuse. */
export function sameOrigin(req: Request): boolean {
  const origin = req.headers.get("origin");
  if (!origin) return true;
  try {
    const o = new URL(origin).host;
    return o === new URL(siteUrl()).host || o === req.headers.get("host") || o === req.headers.get("x-forwarded-host");
  } catch {
    return false;
  }
}

export async function readJson(req: Request, maxBytes = 4096): Promise<{ ok: true; body: unknown } | { ok: false; status: number; message: string }> {
  if (!(req.headers.get("content-type") ?? "").toLowerCase().startsWith("application/json")) return { ok: false, status: 415, message: "Content-Type must be application/json." };
  const text = await req.text();
  if (text.length > maxBytes) return { ok: false, status: 413, message: "Request too large." };
  try {
    return { ok: true, body: JSON.parse(text) };
  } catch {
    return { ok: false, status: 400, message: "Invalid JSON." };
  }
}

/** Admin bearer auth. Returns null when OK, or the response to send. A missing ADMIN_API_TOKEN disables the endpoints (404). */
export function adminGuard(req: Request): Response | null {
  const expected = process.env.ADMIN_API_TOKEN;
  if (!expected || expected.length < 32) return json({ error: "Not found" }, 404);
  const given = (req.headers.get("authorization") ?? "").replace(/^Bearer\s+/i, "");
  if (!given || !safeEqual(given, expected)) return json({ error: "Unauthorized" }, 401, { "WWW-Authenticate": "Bearer" });
  return null;
}
