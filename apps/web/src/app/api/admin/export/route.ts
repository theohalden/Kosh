import { adminGuard } from "@/lib/http";
import { exportCsv } from "@/lib/waitlist/service";

export const dynamic = "force-dynamic";

export async function GET(req: Request) {
  const denied = adminGuard(req);
  if (denied) return denied;
  const csv = await exportCsv();
  return new Response(csv, {
    headers: { "Content-Type": "text/csv; charset=utf-8", "Content-Disposition": 'attachment; filename="kosh-waitlist.csv"', "Cache-Control": "no-store" },
  });
}
