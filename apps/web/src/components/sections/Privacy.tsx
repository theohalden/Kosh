import { Reveal } from "../Reveal";
import { REPO_URL } from "@/content/site";

const FACTS = [
  { k: "It stays on your phone", v: "Your transactions, the original message text and the categories you’ve chosen live in a database on your device." },
  { k: "It can’t phone home", v: "The current app doesn’t ask for internet permission, and its code has no finance-data upload and no analytics." },
  { k: "No account to make", v: "You don’t sign up, and there’s no Tinward server holding your money data." },
  { k: "No copy in device backups", v: "Android’s app backup is switched off for Tinward, so the database isn’t copied into backups." },
];
const PERMS = [
  { name: "READ_SMS", why: "Lets Tinward read your existing bank messages once, to build your history." },
  { name: "RECEIVE_SMS", why: "Lets Tinward notice new transaction messages as they arrive." },
];
const NOT_YET = [
  "Tinward doesn’t encrypt its own database yet. Your phone’s screen lock and device encryption are still doing that work.",
  "Export, backup, restore and in-app deletion aren’t built. Uninstalling Tinward removes its local data.",
  "Message parsing is still maturing. Check totals against your bank statement until it’s proven.",
];

export function Privacy() {
  return (
    <section id="privacy" className="on-ink bg-ink text-paper" aria-labelledby="privacy-h">
      <div className="shell section">
        <div className="grid gap-14 lg:grid-cols-[0.85fr_1.15fr] lg:gap-24">
          <div className="lg:sticky lg:top-28 lg:self-start">
            <Reveal><span className="eyebrow !text-paper/60">Privacy and trust</span></Reveal>
            <Reveal delay={80}><h2 id="privacy-h" className="h2 mt-6">Your money stays on your phone.</h2></Reveal>
            <Reveal delay={160}><p className="lead mt-8 !text-paper/70">Tinward is local-first, and you don’t have to take our word for it: the app is open source, so every claim here points at real code.</p></Reveal>
            <Reveal delay={220}>
              <a href={`${REPO_URL}/blob/main/apps/android/app/src/main/AndroidManifest.xml`} target="_blank" rel="noopener noreferrer" className="mt-8 inline-flex min-h-11 items-center gap-2 border-b border-paper/50 pb-1 font-bold text-paper no-underline hover:border-paper">
                Read the permissions on GitHub <span aria-hidden="true">↗</span><span className="sr-only">(opens in a new tab)</span>
              </a>
            </Reveal>
          </div>

          <div>
            <Reveal>
              <dl className="divide-y divide-paper/15 border-y border-paper/15">
                {FACTS.map((f) => (
                  <div key={f.k} className="grid gap-2 py-6 sm:grid-cols-[14rem_1fr] sm:gap-8">
                    <dt className="display text-[1.55rem] leading-tight">{f.k}</dt>
                    <dd className="text-[0.98rem] text-paper/65">{f.v}</dd>
                  </div>
                ))}
              </dl>
            </Reveal>

            <Reveal delay={80}>
              <h3 className="mt-14 text-[1.9rem]">The two permissions, and why</h3>
              <ul className="mt-5 grid gap-6 sm:grid-cols-2">
                {PERMS.map((p) => (
                  <li key={p.name} className="border-t border-paper/25 pt-4">
                    <code className="text-sm font-bold text-saffron">{p.name}</code>
                    <p className="mt-2 text-[0.95rem] text-paper/65">{p.why}</p>
                  </li>
                ))}
              </ul>
              <p className="mt-4 text-sm text-paper/50">Statement import uses Android’s file picker and needs no SMS access at all.</p>
            </Reveal>

            <Reveal delay={140}>
              <h3 className="mt-14 text-[1.9rem]">What we haven’t built yet</h3>
              <ul className="mt-5 grid gap-4 text-[0.97rem] text-paper/75">
                {NOT_YET.map((x) => <li key={x} className="flex gap-4"><span className="mt-3 h-px w-4 flex-none bg-saffron" aria-hidden="true" />{x}</li>)}
              </ul>
              <p className="mt-6 text-sm text-paper/45">The app downloads Manrope through Google Play services, with a system-font fallback. That’s a font request, not financial data.</p>
            </Reveal>
          </div>
        </div>
      </div>
    </section>
  );
}
