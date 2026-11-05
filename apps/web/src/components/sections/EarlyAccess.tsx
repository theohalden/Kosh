"use client";

import { useId, useRef, useState, type FormEvent } from "react";
import { INTERESTS, INTEREST_LABELS } from "@/lib/waitlist/validation";

type State =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "success"; variant: "check_email" | "resent_recently" }
  | { kind: "duplicate" }
  | { kind: "error"; message: string };

const EMAIL_HINT = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

export function EarlyAccess() {
  const id = useId();
  const [state, setState] = useState<State>({ kind: "idle" });
  const [email, setEmail] = useState("");
  const [interest, setInterest] = useState("");
  const [consent, setConsent] = useState(false);
  const [fieldError, setFieldError] = useState<string | null>(null);
  const emailRef = useRef<HTMLInputElement>(null);
  const hp = useRef<HTMLInputElement>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (state.kind === "loading") return;
    const trimmed = email.trim();
    if (!trimmed) { setFieldError("Enter your email address."); emailRef.current?.focus(); return; }
    if (!EMAIL_HINT.test(trimmed)) { setFieldError("That doesn’t look like a valid email address."); emailRef.current?.focus(); return; }
    setFieldError(null);
    setState({ kind: "loading" });
    try {
      const res = await fetch("/api/waitlist", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: trimmed, interest: interest || undefined, marketingConsent: consent, website: hp.current?.value ?? "" }),
      });
      const data = (await res.json().catch(() => ({}))) as { status?: string; error?: string; field?: string; message?: string };
      if (res.ok) {
        if (data.status === "already_registered") setState({ kind: "duplicate" });
        else setState({ kind: "success", variant: data.status === "resent_recently" ? "resent_recently" : "check_email" });
        return;
      }
      if (res.status === 422 && data.field === "email") { setFieldError(data.message ?? "Check your email address."); setState({ kind: "idle" }); emailRef.current?.focus(); return; }
      setState({ kind: "error", message: data.message ?? "Something went wrong. Please try again." });
    } catch {
      setState({ kind: "error", message: "We couldn’t reach the server. Check your connection and try again." });
    }
  }

  const loading = state.kind === "loading";
  const done = state.kind === "success" || state.kind === "duplicate";

  return (
    <section id="early-access" className="on-ink relative isolate overflow-hidden bg-ink text-paper" aria-labelledby="ea-h">
      <div className="shell section">
        <div className="grid items-center gap-12 lg:grid-cols-[1fr_1fr] lg:gap-20">
          <div>
            <span className="eyebrow !text-paper/60">Early access</span>
            <h2 id="ea-h" className="h2 mt-6">Be here for the clearer picture.</h2>
            <p className="mt-8 max-w-md text-[1.12rem] leading-relaxed text-paper/75">Help shape a more thoughtful way to understand your money. Join the list for an invitation when the Android beta is ready. No launch promises. No payment details.</p>
            <ul className="mt-8 grid gap-3 text-[0.98rem] text-paper/80">
              {["A confirmation email, then your invitation. Product news only if you opt in.", "We never ask for bank details, a phone number or your messages here", "Unsubscribe or delete your details whenever you like"].map((t) => (
                <li key={t} className="flex gap-3"><svg width="18" height="18" viewBox="0 0 18 18" className="mt-1 flex-none" aria-hidden="true"><path d="M3 9.5 7 13.5l8-9" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" /></svg>{t}</li>
              ))}
            </ul>
          </div>

          <div className="rounded-[var(--radius-panel)] bg-paper p-6 text-ink sm:p-9">
            {done ? (
              <div role="status" aria-live="polite" className="py-4 text-center sm:py-8">
                <div className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-mint text-green" aria-hidden="true">
                  <svg width="30" height="30" viewBox="0 0 30 30"><path d="M6 15.5 12.5 22 24 8.5" fill="none" stroke="currentColor" strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round" style={{ strokeDasharray: 40, strokeDashoffset: 40, animation: "draw 0.7s 0.1s var(--ease-out-quint) forwards" }} /></svg>
                </div>
                {state.kind === "duplicate" ? (
                  <>
                    <h3 className="mt-6 text-3xl">You&apos;re already on the list.</h3>
                    <p className="mt-3 text-ink-2">That address is confirmed, so there&apos;s nothing more to do. We&apos;ll email you when the beta opens.</p>
                  </>
                ) : (
                  <>
                    <h3 className="mt-6 text-3xl">Check your inbox.</h3>
                    <p className="mt-3 text-ink-2">{state.kind === "success" && state.variant === "resent_recently" ? "We sent a confirmation email a moment ago. Give it a minute, and check spam or promotions." : "We sent a link to confirm your address. Your place on the list isn’t held until you click it."}</p>
                    <p className="mt-2 text-sm text-ink-3">The link works for 48 hours.</p>
                  </>
                )}
                <button type="button" className="mt-6 min-h-11 font-bold underline underline-offset-4" onClick={() => { setState({ kind: "idle" }); setEmail(""); setConsent(false); setInterest(""); }}>Use a different email</button>
              </div>
            ) : (
              <form onSubmit={onSubmit} noValidate aria-describedby={`${id}-fine`}>
                <h3 className="text-[2rem]">Join the early-access list</h3>
                <div className="mt-6">
                  <label htmlFor={`${id}-email`} className="block text-sm font-extrabold">Email address <span className="font-medium text-ink-2">(required)</span></label>
                  <input
                    ref={emailRef} id={`${id}-email`} name="email" type="email" inputMode="email" autoComplete="email" required
                    value={email} onChange={(e) => setEmail(e.target.value)} disabled={loading}
                    aria-invalid={fieldError ? true : undefined} aria-describedby={fieldError ? `${id}-err` : undefined}
                    placeholder="you@example.com"
                    className={`mt-2 min-h-14 w-full rounded-2xl border-2 bg-white px-4 text-[1.05rem] placeholder:text-ink-3/70 ${fieldError ? "border-coral-deep" : "border-line focus:border-ink"}`}
                  />
                  {fieldError && <p id={`${id}-err`} role="alert" className="mt-2 text-sm font-bold text-coral-deep">{fieldError}</p>}
                </div>

                <div className="mt-5">
                  <label htmlFor={`${id}-interest`} className="block text-sm font-extrabold">What interests you most? <span className="font-medium text-ink-2">(optional)</span></label>
                  <div className="relative mt-2">
                    <select id={`${id}-interest`} name="interest" value={interest} onChange={(e) => setInterest(e.target.value)} disabled={loading} className="min-h-14 w-full appearance-none rounded-2xl border-2 border-line bg-white px-4 pr-11 text-[1.05rem] focus:border-ink">
                      <option value="">Choose one…</option>
                      {INTERESTS.map((k) => <option key={k} value={k}>{INTEREST_LABELS[k]}</option>)}
                    </select>
                    <svg className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2" width="14" height="14" viewBox="0 0 14 14" aria-hidden="true"><path d="m2 5 5 5 5-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>
                  </div>
                </div>

                {/* honeypot – invisible to people and assistive tech */}
                <div aria-hidden="true" className="absolute -left-[9999px] h-0 w-0 overflow-hidden"><label>Website<input ref={hp} type="text" name="website" tabIndex={-1} autoComplete="off" /></label></div>

                <label className="mt-6 flex cursor-pointer items-start gap-3">
                  <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} disabled={loading} className="mt-1 h-6 w-6 flex-none accent-[#12292c]" />
                  <span className="text-[0.92rem] leading-snug text-ink-2">Also send me occasional Tinward product news. <span className="text-ink-3">(Optional. You&apos;ll get your invitation either way.)</span></span>
                </label>

                <button type="submit" disabled={loading} className="btn btn-primary mt-7 w-full">
                  {loading ? (<><svg className="animate-spin" width="20" height="20" viewBox="0 0 20 20" aria-hidden="true"><circle cx="10" cy="10" r="8" fill="none" stroke="currentColor" strokeOpacity=".3" strokeWidth="3" /><path d="M10 2a8 8 0 0 1 8 8" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" /></svg>Joining…</>) : (<>Join early access <span aria-hidden="true">↗</span></>)}
                </button>

                <div aria-live="assertive" className="min-h-0">
                  {state.kind === "error" && <p role="alert" className="mt-4 rounded-xl bg-coral-soft/60 px-4 py-3 text-[0.95rem] font-bold text-[#6b1d10]">{state.message}</p>}
                </div>
                <p id={`${id}-fine`} className="mt-5 text-xs leading-relaxed text-ink-3">
                  We store your email, your optional answer and your consent choice with a timestamp, only to run early access. See the <a href="/privacy" className="underline">privacy policy</a>. No payment is requested.
                </p>
              </form>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
