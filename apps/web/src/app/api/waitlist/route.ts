import { clientIp, json, readJson, sameOrigin } from "@/lib/http";
import { signup } from "@/lib/waitlist/service";
import { parseSignupBody } from "@/lib/waitlist/validation";

export const dynamic = "force-dynamic";

export async function POST(req: Request) {
  if (!sameOrigin(req)) return json({ error: "forbidden", message: "Cross-site request blocked." }, 403);
  const raw = await readJson(req);
  if (!raw.ok) return json({ error: "bad_request", message: raw.message }, raw.status);

  const parsed = parseSignupBody(raw.body);
  if (!parsed.ok) return json({ error: "validation", field: parsed.field, message: parsed.message }, 422);

  // Honeypot: bots fill the hidden field. Pretend success, store nothing, send nothing.
  if (parsed.value.honeypot) return json({ status: "check_email" });

  try {
    const result = await signup(parsed.value, clientIp(req));
    switch (result.status) {
      case "rate_limited":
        return json({ error: "rate_limited", message: "Too many attempts. Please try again in a little while." }, 429, { "Retry-After": "900" });
      case "email_failed":
        return json({ error: "email_failed", message: "We saved your details but couldn’t send the confirmation email. Please try again in a minute." }, 503);
      default:
        return json({ status: result.status });
    }
  } catch (err) {
    console.error("[waitlist] signup failed:", err instanceof Error ? err.message : err);
    return json({ error: "server", message: "Something went wrong on our side. Please try again shortly." }, 500);
  }
}
