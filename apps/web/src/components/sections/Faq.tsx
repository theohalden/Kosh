import { Reveal } from "../Reveal";
import { FAQS } from "@/content/site";

export function Faq() {
  return (
    <section id="faq" className="shell section" aria-labelledby="faq-h">
      <div className="grid gap-12 lg:grid-cols-[0.7fr_1.3fr] lg:gap-20">
        <div>
          <Reveal><span className="eyebrow">FAQ</span></Reveal>
          <Reveal delay={80}><h2 id="faq-h" className="h2 mt-5">Straight answers.</h2></Reveal>
          <Reveal delay={160}><p className="lead mt-6">Including the unglamorous ones: what&apos;s built, what&apos;s not, and what we don&apos;t know yet.</p></Reveal>
        </div>
        <div className="border-t-2 border-ink">
          {FAQS.map((f) => (
            <details key={f.q} className="faq group border-b border-line">
              <summary className="flex min-h-[4.25rem] cursor-pointer list-none items-center justify-between gap-6 py-4 text-left [&::-webkit-details-marker]:hidden">
                <span className="display text-[clamp(1.25rem,2.2vw,1.65rem)] font-semibold leading-tight tracking-normal">{f.q}</span>
                <span className="grid h-9 w-9 flex-none place-items-center rounded-full border-2 border-ink transition-transform duration-300 group-open:rotate-45" aria-hidden="true">
                  <svg width="14" height="14" viewBox="0 0 14 14"><path d="M7 1v12M1 7h12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" /></svg>
                </span>
              </summary>
              <p className="max-w-2xl pb-6 pr-14 text-[1.02rem] text-ink-2">{f.a}</p>
            </details>
          ))}
        </div>
      </div>
    </section>
  );
}
