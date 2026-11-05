"use client";
import Link from "next/link";

import { useState } from "react";

/** One explicit button press performs the action, so mail scanners that prefetch links can't trigger it. */
export function TokenAction({ endpoint, token, idleTitle, idleBody, button, doneTitle, doneBody, danger = false }: {
  endpoint: string; token: string; idleTitle: string; idleBody: string; button: string; doneTitle: string; doneBody: string; danger?: boolean;
}) {
  const [s, setS] = useState<"idle" | "loading" | "done" | "error">("idle");
  const [msg, setMsg] = useState("");
  async function go() {
    setS("loading");
    try {
      const r = await fetch(endpoint, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ token }) });
      const d = (await r.json().catch(() => ({}))) as { message?: string };
      if (r.ok) setS("done");
      else { setMsg(d.message ?? "Something went wrong."); setS("error"); }
    } catch { setMsg("We couldn’t reach the server. Please try again."); setS("error"); }
  }
  if (!token) return <div role="alert" className="paper-card p-8"><h1 className="text-3xl">This link isn’t valid.</h1><p className="mt-3 text-ink-2">Open the link from your email again, or request a new one.</p></div>;
  if (s === "done") return <div role="status" className="paper-card p-8 text-center sm:p-12"><h1 className="text-4xl">{doneTitle}</h1><p className="mx-auto mt-4 max-w-md text-ink-2">{doneBody}</p><Link href="/" className="btn btn-ghost mt-8">Back to Tinward</Link></div>;
  return (
    <div className="paper-card p-8 sm:p-12">
      <h1 className="text-4xl">{idleTitle}</h1>
      <p className="mt-4 max-w-md text-ink-2">{idleBody}</p>
      <button onClick={go} disabled={s === "loading"} className={`btn mt-8 ${danger ? "bg-ink text-paper hover:bg-coral-deep" : "btn-primary"}`}>{s === "loading" ? "Working…" : button}</button>
      {s === "error" && <p role="alert" className="mt-4 font-bold text-coral-deep">{msg}</p>}
    </div>
  );
}
