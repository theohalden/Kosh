import { Reveal } from "../Reveal";

const DIARY: { d: string; t: string }[] = [
  { d: "In", t: "It comes in." },
  { d: "Out", t: "It goes out." },
  { d: "Aside", t: "Some gets saved." },
  { d: "Away", t: "Some disappears into the everyday." },
  { d: "Lost", t: "And somewhere between your accounts, recurring bills, investments and expenses, the bigger picture gets lost." },
];

export function Problem() {
  return (
    <section className="on-ink bg-ink text-paper" aria-labelledby="problem-h">
      <div className="shell section">
        <div className="grid gap-16 lg:grid-cols-[0.9fr_1.1fr] lg:gap-24">
          <div className="lg:sticky lg:top-28 lg:self-start">
            <Reveal><span className="eyebrow !text-paper/60">Life is complicated enough</span></Reveal>
            <Reveal delay={80}>
              <h2 id="problem-h" className="h2 mt-6">Your money shouldn’t be <em className="text-coral-soft">another mystery.</em></h2>
            </Reveal>
            <Reveal delay={160}>
              <p className="lead mt-8 !text-paper/70">You shouldn’t need another spreadsheet, a complicated dashboard, or hours of bookkeeping just to understand your own finances.</p>
            </Reveal>
          </div>

          <ol className="relative border-l border-paper/20" aria-label="Where the bigger picture gets lost">
            {DIARY.map((e, i) => (
              <li key={e.d} className="relative pb-12 pl-8 last:pb-0 sm:pl-12">
                <span className="timeline-dot" aria-hidden="true" />
                <Reveal delay={60}>
                  
                  <time className="text-sm font-bold tracking-wide text-coral-soft">{e.d}</time>
                  <p className={`display mt-2 text-[clamp(1.6rem,3vw,2.5rem)] leading-[1.1] tracking-[-0.02em] ${i === DIARY.length - 1 ? "text-paper" : "text-paper/85"}`}>{e.t}</p>
                </Reveal>
              </li>
            ))}
          </ol>
        </div>

        <Reveal className="mt-24 grid gap-8 border-t border-paper/15 pt-14 md:grid-cols-[1.2fr_auto] md:items-end">
          <p className="display max-w-3xl text-[clamp(2rem,4.4vw,3.8rem)] leading-[1.02] tracking-[-0.03em]">
            Knowing where you stand <span className="text-paper/55">should feel simple.</span>
          </p>
          <a href="#product" className="btn !border-paper/40 !text-paper hover:!bg-paper hover:!text-ink border-2">See how Tinward helps <span aria-hidden="true">↓</span></a>
        </Reveal>
      </div>
    </section>
  );
}
