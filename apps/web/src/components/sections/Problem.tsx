import { Reveal } from "../Reveal";

const DIARY: { d: string; t: string }[] = [
  { d: "1 Nov", t: "Income arrives. For a moment, everything feels simple." },
  { d: "2 Nov", t: "Rent leaves one account. Everyday purchases leave another." },
  { d: "9 Nov", t: "You open your bank app. It shows a balance, not a story." },
  { d: "14 Nov", t: "A loan payment is due. Which account was it coming from?" },
  { d: "19 Nov", t: "You open the spreadsheet. Last updated: August." },
  { d: "30 Nov", t: "Month-end. Plenty of numbers. Still no clear sense of where you stand." },
];

export function Problem() {
  return (
    <section className="on-ink bg-ink text-paper" aria-labelledby="problem-h">
      <div className="shell section">
        <div className="grid gap-16 lg:grid-cols-[0.9fr_1.1fr] lg:gap-24">
          <div className="lg:sticky lg:top-28 lg:self-start">
            <Reveal><span className="eyebrow !text-paper/60">A month, as most of us live it</span></Reveal>
            <Reveal delay={80}>
              <h2 id="problem-h" className="h2 mt-6">Your money is everywhere. <em className="text-coral-soft">Your picture of it isn’t.</em></h2>
            </Reveal>
            <Reveal delay={160}>
              <p className="lead mt-8 !text-paper/70">An account for bills. Another for everyday spending. Investments somewhere else. Each one shows a number. None shows the whole picture.</p>
            </Reveal>
          </div>

          <ol className="relative border-l border-paper/20" aria-label="A month of money, day by day">
            {DIARY.map((e, i) => (
              <li key={e.d} className="pb-12 pl-8 last:pb-0 sm:pl-12">
                <Reveal delay={60}>
                  <span className="absolute -left-[5px] mt-3 h-[9px] w-[9px] rounded-full bg-ink ring-1 ring-paper/50" aria-hidden="true" />
                  <time className="text-sm font-bold tracking-wide text-coral-soft">{e.d}</time>
                  <p className={`display mt-2 text-[clamp(1.6rem,3vw,2.5rem)] leading-[1.1] tracking-[-0.02em] ${i === DIARY.length - 1 ? "text-paper" : "text-paper/85"}`}>{e.t}</p>
                </Reveal>
              </li>
            ))}
          </ol>
        </div>

        <Reveal className="mt-24 grid gap-8 border-t border-paper/15 pt-14 md:grid-cols-[1.2fr_auto] md:items-end">
          <p className="display max-w-3xl text-[clamp(2rem,4.4vw,3.8rem)] leading-[1.02] tracking-[-0.03em]">
            You’re not bad with money. <span className="text-paper/55">Your money is just spread across too many places.</span>
          </p>
          <a href="#product" className="btn !border-paper/40 !text-paper hover:!bg-paper hover:!text-ink border-2">See it as one picture <span aria-hidden="true">↓</span></a>
        </Reveal>
      </div>
    </section>
  );
}
