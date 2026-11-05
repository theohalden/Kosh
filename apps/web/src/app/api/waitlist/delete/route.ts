import { clientIp, json, readJson, sameOrigin } from "@/lib/http";
import { requestDeletion } from "@/lib/waitlist/service";

export const dynamic = "force-dynamic";

export async function POST(req: Request) {
  if (!sameOrigin(req)) return json({ error: "forbidden" }, 403);
  const raw = await readJson(req);
  if (!raw.ok) return json({ error: "bad_request", message: raw.message }, raw.status);
  const email = typeof (raw.body as { email?: unknown })?.email === "string" ? (raw.body as { email: string }).email : "";
  try {
    const r = await requestDeletion(email, clientIp(req));
    if (r === "invalid") return json({ error: "validation", message: "Enter the email address you signed up with." }, 422);
    if (r === "rate_limited") return json({ error: "rate_limited", message: "Too many requests. Please try again later." }, 429, { "Retry-After": "3600" });
    // Same answer whether or not the address is on the list.
    return json({ status: "accepted" });
  } catch (err) {
    console.error("[waitlist] deletion request failed:", err instanceof Error ? err.message : err);
    return json({ error: "server", message: "Something went wrong. Please try again." }, 500);
  }
}
