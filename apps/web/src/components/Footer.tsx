import { MotionPreference } from "./MotionPreference";
import { Logo } from "./Logo";
import { REPO_URL } from "@/content/site";

export function Footer() {
  return (
    <footer className="on-ink bg-ink text-paper">
      <div className="shell py-16">
        <div className="grid gap-12 md:grid-cols-[1.3fr_1fr_1fr]">
          <div>
            <Logo />
            <p className="display mt-5 max-w-xs text-2xl font-medium leading-tight text-paper/90">Your money. In perspective.</p>
            <p className="mt-4 max-w-xs text-sm text-paper/60">A clearer picture of your financial life. Early development. Early Android APK available.</p>
          </div>
          <nav aria-label="Footer">
            <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-paper/50">Explore</p>
            <ul className="mt-4 grid gap-1">
              {[["Download APK", "/#download"], ["Product", "/#product"], ["Features", "/#features"], ["How it works", "/#how"], ["Pricing", "/#pricing"], ["FAQ", "/#faq"], ["Early access", "/#early-access"]].map(([l, h]) => (
                <li key={h}><a href={h} className="inline-flex min-h-10 items-center text-paper/85 no-underline hover:text-coral-soft">{l}</a></li>
              ))}
            </ul>
          </nav>
          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-paper/50">Project</p>
            <ul className="mt-4 grid gap-1">
              <li><a href={REPO_URL} target="_blank" rel="noopener noreferrer" className="inline-flex min-h-10 items-center gap-1 text-paper/85 no-underline hover:text-coral-soft">Source on GitHub <span aria-hidden="true">↗</span><span className="sr-only">(opens in a new tab)</span></a></li>
              <li><a href={`${REPO_URL}/issues`} target="_blank" rel="noopener noreferrer" className="inline-flex min-h-10 items-center gap-1 text-paper/85 no-underline hover:text-coral-soft">Questions &amp; issues <span aria-hidden="true">↗</span><span className="sr-only">(opens in a new tab)</span></a></li>
              <li><a href="/privacy" className="inline-flex min-h-10 items-center text-paper/85 no-underline hover:text-coral-soft">Privacy policy</a></li>
              <li><a href="/terms" className="inline-flex min-h-10 items-center text-paper/85 no-underline hover:text-coral-soft">Terms</a></li>
            </ul>
          </div>
        </div>
        <div className="mt-14 flex flex-col gap-3 border-t border-paper/15 pt-6 text-sm text-paper/55 sm:flex-row sm:justify-between">
          <p>© 2026 Tinward. The Android app is open source under the MIT license.</p>
          <div className="flex flex-col gap-2 sm:items-end"><p>Figures on this site are illustrative sample data.</p><MotionPreference /></div>
        </div>
      </div>
    </footer>
  );
}
