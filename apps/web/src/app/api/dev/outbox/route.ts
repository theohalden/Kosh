import { json } from "@/lib/http";
import { outbox } from "@/lib/waitlist/email";

export const dynamic = "force-dynamic";

/** Development-only view of the in-memory email outbox. 404 in production. */
export async function GET() {
  if (process.env.NODE_ENV === "production") return json({ error: "Not found" }, 404);
  return json(outbox());
}
