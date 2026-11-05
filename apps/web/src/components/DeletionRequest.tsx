"use client";

import { useState, type FormEvent } from "react";

export function DeletionRequest() {
  const [email, setEmail] = useState("");
  const [s, setS] = useState<"idle" | "loading" | "done" | "error">("idle");
  const [msg, setMsg] = useState("");
  async function submit(e: FormEvent) {
    e.preventDefault();
    setS("loading");
    try {
      const r = await fetch("/api/waitlist/delete", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email }) });
      const d = (await r.json().catch(() => ({}))) as { message?: string };
      if (r.ok) setS("done"); else { setMsg(d.message ?? "Something went wrong."); setS("error"); }
    } catch { setMsg("We couldn’t reach the server. Please try again."); setS("error"); }
  }
  if (s === "done") return <p role="status" className="rounded-2xl bg-mint/60 p-5 font-bold">If that address is on the list, we’ve emailed a confirmation link. It works for 24 hours and nothing is deleted until you click it.</p>;
  return (
    <form onSubmit={submit} className="grid gap-3 sm:grid-cols-[1fr_auto]">
      <label className="sr-only" htmlFor="del-email">Email address you signed up with</label>
      <input id="del-email" type="email" required autoComplete="email" placeholder="you@example.com" value={email} onChange={(e) => setEmail(e.target.value)} className="min-h-14 rounded-2xl border-2 border-line bg-white px-4 focus:border-ink" />
      <button className="btn bg-ink text-paper hover:bg-coral-deep" disabled={s === "loading"}>{s === "loading" ? "Sending…" : "Email me a deletion link"}</button>
      {s === "error" && <p role="alert" className="font-bold text-coral-deep sm:col-span-2">{msg}</p>}
    </form>
  );
}
