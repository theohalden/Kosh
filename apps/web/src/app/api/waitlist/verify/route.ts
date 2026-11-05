import { NextResponse } from "next/server";
import { verify } from "@/lib/waitlist/service";
import { siteUrl } from "@/lib/waitlist/email";

export const dynamic = "force-dynamic";

/** Email links are GET. Verification is idempotent and has no destructive effect, so GET is acceptable here. */
export async function GET(req: Request) {
  const token = new URL(req.url).searchParams.get("token") ?? "";
  let result = "error";
  try {
    result = await verify(token);
  } catch (err) {
    console.error("[waitlist] verify failed:", err instanceof Error ? err.message : err);
  }
  return NextResponse.redirect(`${siteUrl()}/confirmed?r=${result}`, 303);
}
