import type { Metadata } from "next";
import { StatusShell } from "@/components/StatusShell";

export const metadata: Metadata = { title: "Email confirmation", robots: { index: false } };

const COPY: Record<string, { h: string; p: string; ok: boolean }> = {
  verified: { h: "You’re confirmed.", p: "Your email is verified and you’re on the Tinward early-access list. We’ll email when the Android beta opens. Product news is optional.", ok: true },
  already_verified: { h: "Already confirmed.", p: "This address was verified earlier. You’re on the list; nothing more to do.", ok: true },
  expired: { h: "That link has expired.", p: "Confirmation links last 48 hours. Sign up again with the same email and we’ll send a fresh one.", ok: false },
  invalid: { h: "That link isn’t valid.", p: "It may have been used already or copied incompletely. Try signing up again to get a new link.", ok: false },
};

export default async function Confirmed({ searchParams }: { searchParams: Promise<{ r?: string }> }) {
  const { r = "invalid" } = await searchParams;
  const c = COPY[r] ?? { h: "Something went wrong.", p: "We couldn’t confirm that link. Please try again in a moment.", ok: false };
  return (
    <StatusShell>
      <div role="status" className="paper-card p-8 text-center sm:p-12">
        <div className={`mx-auto grid h-16 w-16 place-items-center rounded-full ${c.ok ? "bg-mint text-green" : "bg-coral-soft text-coral-deep"}`} aria-hidden="true">
          <svg width="30" height="30" viewBox="0 0 30 30">{c.ok ? <path d="M6 15.5 12.5 22 24 8.5" fill="none" stroke="currentColor" strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round" /> : <path d="M9 9l12 12M21 9 9 21" fill="none" stroke="currentColor" strokeWidth="3.2" strokeLinecap="round" />}</svg>
        </div>
        <h1 className="mt-6 text-4xl">{c.h}</h1>
        <p className="mx-auto mt-4 max-w-md text-ink-2">{c.p}</p>
        <a href={c.ok ? "/" : "/#early-access"} className="btn btn-primary mt-8">{c.ok ? "Back to Tinward" : "Join early access"}</a>
      </div>
    </StatusShell>
  );
}
