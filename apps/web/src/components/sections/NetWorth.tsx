"use client";

import { Reveal, PlannedChip } from "../Reveal";
import { useInView } from "@/lib/useInView";
import { netWorth, sum } from "@/content/sample";
import { money } from "@/lib/format";

const H = 340;

export function NetWorth() {
  const [ref, seen] = useInView<HTMLDivElement>(0.35);
  const A = sum(netWorth.assets);
  const L = sum(netWorth.liabilities);
  const N = A - L;
  const h = (v: number) => Math.round((v / A) * H);

  const stack = (items: typeof netWorth.assets, label: string, total: number, tone: string) => (
    <div className="flex flex-1 flex-col">
      <div className="flex w-full flex-col justify-end gap-[2px]" style={{ height: H }}>
        {items.map((s, i) => (
          <div key={s.label} className="relative w-full overflow-hidden" style={{ height: h(s.value), background: s.color, transform: `scaleY(${seen ? 1 : 0})`, transformOrigin: "bottom", transition: `transform 850ms var(--ease-out-quint) ${i * 80}ms` }}>
            {h(s.value) > 56 && <span className="absolute left-2.5 top-2 text-[0.72rem] font-bold leading-tight text-ink/80">{s.label}</span>}
          </div>
        ))}
      </div>
      <p className="mt-4 text-sm font-bold text-ink-2">{label}</p>
      <p className={`display num text-[clamp(1.5rem,3vw,2.4rem)] leading-tight tracking-[-0.02em] ${tone}`}>{money(total)}</p>
    </div>
  );

  return (
    <section className="bg-paper-2/60" aria-labelledby="nw-h">
      <div className="shell section">
        <div className="grid gap-14 lg:grid-cols-[0.8fr_1.2fr] lg:gap-24">
          <div>
            <div className="flex items-center gap-5"><Reveal><span className="eyebrow">Net worth</span></Reveal><PlannedChip /></div>
            <Reveal delay={80}><h2 id="nw-h" className="h2 mt-6">Everything you own, minus what you owe.</h2></Reveal>
            <Reveal delay={160}>
              <p className="lead mt-8">One number for where you stand today. And month after month, a quiet answer to the question underneath all the others: am I moving in the right direction?</p>
            </Reveal>
            <Reveal delay={220}><p className="mt-8 max-w-md border-t border-line pt-5 text-[0.95rem] text-ink-2">The planned view brings recorded account balances, asset values and liabilities together. A snapshot, only as current as your last update.</p></Reveal>
          </div>

          <div ref={ref}>
            <div className="flex items-end gap-3 sm:gap-8" role="img" aria-label={`Assets ${money(A)} minus liabilities ${money(L)} equals net worth ${money(N)}`}>
              {stack(netWorth.assets, "What you own", A, "text-green")}
              <span className="display mb-24 text-4xl text-ink-3 sm:text-5xl" aria-hidden="true">−</span>
              {stack(netWorth.liabilities, "What you owe", L, "text-[#a05a48]")}
              <span className="display mb-24 text-4xl text-ink-3 sm:text-5xl" aria-hidden="true">=</span>
              <div className="flex flex-1 flex-col">
                <div className="flex w-full flex-col justify-end" style={{ height: H }}>
                  <div className="w-full bg-ink" style={{ height: h(N), transform: `scaleY(${seen ? 1 : 0})`, transformOrigin: "bottom", transition: "transform 1000ms var(--ease-out-quint) 200ms" }} />
                </div>
                <p className="mt-4 text-sm font-bold text-ink-2">Net worth</p>
                <p className="display num text-[clamp(1.5rem,3vw,2.4rem)] leading-tight tracking-[-0.02em]">{money(N)}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
