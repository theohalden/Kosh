import { json, readJson, sameOrigin } from "@/lib/http";
import { unsubscribe } from "@/lib/waitlist/service";

export const dynamic = "force-dynamic";

/** Accepts {token} JSON from our page, or an RFC 8058 one-click post with ?token= in the URL. */
export async function POST(req: Request) {
  let token = new URL(req.url).searchParams.get("token") ?? "";
  if (!token) {
    if (!sameOrigin(req)) return json({ error: "forbidden" }, 403);
    const raw = await readJson(req);
    if (!raw.ok) return json({ error: "bad_request", message: raw.message }, raw.status);
    token = typeof (raw.body as { token?: unknown })?.token === "string" ? (raw.body as { token: string }).token : "";
  }
  try {
    const r = await unsubscribe(token);
    if (r === "ok" || r === "not_found") return json({ status: "unsubscribed" });
    return json({ error: r, message: r === "expired" ? "This link has expired." : "This link isn’t valid." }, 400);
  } catch (err) {
    console.error("[waitlist] unsubscribe failed:", err instanceof Error ? err.message : err);
    return json({ error: "server", message: "Something went wrong. Please try again." }, 500);
  }
}
