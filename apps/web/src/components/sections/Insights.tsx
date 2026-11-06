"use client";

import { Reveal, LiveChip } from "../Reveal";
import { useInView } from "@/lib/useInView";
import { overview } from "@/content/sample";
import { money } from "@/lib/format";
import { useMarket } from "@/lib/market";

export function Insights() {
  useMarket();
  const [ref, seen] = useInView<HTMLDivElement>(0.25);
  const max = Math.max(...overview.categories.map((c) => c.value));
  return (
    <section className="border-t border-line" aria-labelledby="insights-h">
      <div className="shell section">
        <div className="grid gap-14 lg:grid-cols-[0.9fr_1.1fr] lg:gap-24">
          <div>
            <Reveal><span className="eyebrow">Understand your habits</span></Reveal>
            <Reveal delay={80}><h2 id="insights-h" className="h2 mt-6">The story behind your spending.</h2></Reveal>
            <Reveal delay={160}>
              <p className="lead mt-8">A long list of transactions only tells you so much. What matters is seeing the bigger picture: which categories take up the most space, how your spending changes, and where there may be room to do things differently.</p>
            </Reveal>
            <Reveal delay={220}><p className="mt-8 max-w-md border-t border-line pt-5 text-[0.95rem] text-ink-2">Make sense of the numbers, without having to become an expert in them.</p></Reveal>
            <Reveal delay={260}><div className="mt-8"><LiveChip>Category spending in the app today</LiveChip></div></Reveal>
          </div>

          <div ref={ref} className="paper-card p-6 sm:p-9">
            <div className="flex items-end justify-between gap-4">
              <div><p className="text-sm font-bold text-ink-2">Spent in {overview.month}</p><p className="display num mt-1 text-[clamp(2.6rem,5vw,4rem)] leading-none tracking-[-0.03em]">{money(overview.spent)}</p></div>
              <p className="pb-1 text-sm text-ink-3">of {money(overview.income)} income</p>
            </div>
            <ul className="mt-8 grid gap-5" aria-label="Spending by category, illustrative">
              {overview.categories.map((c, i) => (
                <li key={c.label} className="insight-row">
                  <div className="flex items-baseline justify-between text-[0.98rem]"><span className="font-semibold">{c.label}</span><span className="num font-bold">{money(c.value)}</span></div>
                  <div className="mt-2 h-2 overflow-hidden rounded-full bg-paper-3">
                    <i className="block h-full rounded-full" style={{ width: seen ? `${(c.value / max) * 100}%` : "0%", background: c.color, transition: `width 800ms var(--ease-out-quint) ${i * 70}ms` }} />
                  </div>
                </li>
              ))}
            </ul>
            <p className="mt-6 text-xs text-ink-3">Illustrative sample figures, not customer data.</p>
          </div>
        </div>
      </div>
    </section>
  );
}
