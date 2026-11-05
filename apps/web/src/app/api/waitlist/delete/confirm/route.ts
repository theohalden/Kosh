import { json, readJson, sameOrigin } from "@/lib/http";
import { confirmDeletion } from "@/lib/waitlist/service";

export const dynamic = "force-dynamic";

export async function POST(req: Request) {
  if (!sameOrigin(req)) return json({ error: "forbidden" }, 403);
  const raw = await readJson(req);
  if (!raw.ok) return json({ error: "bad_request", message: raw.message }, raw.status);
  const token = typeof (raw.body as { token?: unknown })?.token === "string" ? (raw.body as { token: string }).token : "";
  try {
    const r = await confirmDeletion(token);
    if (r === "ok" || r === "not_found") return json({ status: "deleted" });
    return json({ error: r, message: r === "expired" ? "This link has expired. Request deletion again." : "This link isn’t valid." }, 400);
  } catch (err) {
    console.error("[waitlist] deletion failed:", err instanceof Error ? err.message : err);
    return json({ error: "server", message: "Something went wrong. Please try again." }, 500);
  }
}
