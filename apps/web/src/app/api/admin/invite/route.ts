import { adminGuard, json, readJson } from "@/lib/http";
import { invite } from "@/lib/waitlist/service";

export const dynamic = "force-dynamic";

/** POST {"limit": 25}: invites the longest-waiting verified, still-subscribed, not-yet-invited people. */
export async function POST(req: Request) {
  const denied = adminGuard(req);
  if (denied) return denied;
  const raw = await readJson(req);
  const limit = raw.ok && typeof (raw.body as { limit?: unknown })?.limit === "number" ? Math.floor((raw.body as { limit: number }).limit) : 0;
  if (limit < 1 || limit > 200) return json({ error: "validation", message: "limit must be an integer from 1 to 200." }, 422);
  try {
    return json(await invite(limit));
  } catch (err) {
    console.error("[admin] invite failed:", err instanceof Error ? err.message : err);
    return json({ error: "server", message: err instanceof Error ? err.message : "failed" }, 500);
  }
}
