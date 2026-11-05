"use client";

import { useState } from "react";
import { Reveal, PlannedChip } from "../Reveal";
import { holdings, sum } from "@/content/sample";
import { radioKeys } from "@/lib/radioKeys";
import { money } from "@/lib/format";

const HISTORY = [0.6, 0.7, 0.78, 0.9, 1];
const MONTHS = ["Jul", "Aug", "Sep", "Oct", "Nov"];

export function Wealth() {
  const [sel, setSel] = useState("mf");
  const cur = holdings.find((h) => h.id === sel)!;
  const total = sum(holdings);
  const max = Math.max(...holdings.map((h) => h.value));
  return (
    <section className="border-t border-line" aria-labelledby="wealth-h">
      <div className="shell section">
        <div className="grid gap-14 lg:grid-cols-[0.9fr_1.1fr] lg:gap-24">
          <div>
            <div className="flex items-center gap-5"><Reveal><span className="eyebrow">Investments</span></Reveal><PlannedChip /></div>
            <Reveal delay={80}><h2 id="wealth-h" className="h2 mt-6">Your investments shouldn’t live in another spreadsheet.</h2></Reveal>
            <Reveal delay={160}>
              <p className="lead mt-8">Stocks, funds, retirement accounts, property or savings. The planned wealth view gives each a place. Add a holding, update its value when you check it, and keep the bigger picture close.</p>
            </Reveal>
            <Reveal delay={220}>
              <p className="mt-8 max-w-md border-t border-line pt-5 text-[0.95rem] text-ink-2"><strong className="text-ink">Manual by design.</strong> The planned tracker uses values you enter. Broker connections and live prices are not implemented. You decide what belongs in your picture.</p>
            </Reveal>
          </div>

          <Reveal delay={120}>
            <div className="paper-card overflow-hidden">
              <div className="border-b border-line px-6 py-5 sm:px-8">
                <p className="text-sm font-bold text-ink-2">Everything you’ve recorded</p>
                <p className="display num mt-1 text-[clamp(2.4rem,4.6vw,3.6rem)] leading-none tracking-[-0.03em]">{money(total)}</p>
              </div>
              <div className="grid md:grid-cols-[1.15fr_1fr]">
                <ul role="radiogroup" aria-label="Asset types" className="p-3 sm:p-4">
                  {holdings.map((h, index) => {
                    const on = h.id === sel;
                    return (
                      <li key={h.id}>
                        <button role="radio" aria-checked={on} tabIndex={on ? 0 : -1} onKeyDown={e => radioKeys(e, index, holdings.length, next => setSel(holdings[next].id))} onClick={() => setSel(h.id)} className={`w-full rounded-[10px] px-3 py-3 text-left transition-colors ${on ? "bg-ink text-paper" : "hover:bg-paper-2"}`}>
                          <span className="flex items-baseline gap-2">
                            <span className="font-bold">{h.label}</span>
                            <span aria-hidden="true" className={`flex-1 translate-y-[-3px] border-b border-dotted ${on ? "border-paper/40" : "border-line"}`} />
                            <span className="num font-bold">{money(h.value)}</span>
                          </span>
                          <span className="mt-2 block h-[3px] overflow-hidden rounded-full bg-current/15"><i className="block h-full rounded-full bg-coral" style={{ width: `${(h.value / max) * 100}%` }} /></span>
                        </button>
                      </li>
                    );
                  })}
                </ul>
                <div className="border-t border-line bg-paper-2/50 p-6 md:border-l md:border-t-0" aria-live="polite">
                  <p className="text-sm font-bold text-ink-2">{cur.label}</p>
                  <p className="num display mt-2 text-4xl leading-none tracking-[-0.03em]">{money(cur.value)}</p>
                  <p className="mt-2 text-sm text-ink-3">{cur.updated}</p>
                  <p className="mt-5 text-[0.95rem] text-ink-2">{cur.hint}</p>
                  <p className="mt-6 text-xs font-bold text-ink-3">Values you’ve entered</p>
                  <div className="mt-3 flex h-20 items-end gap-2" aria-hidden="true">
                    {HISTORY.map((f, i) => (<i key={i} className={`block flex-1 ${i === HISTORY.length - 1 ? "bg-coral" : "bg-ink/20"}`} style={{ height: `${f * 100}%`, transition: "height .5s var(--ease-out-quint)" }} />))}
                  </div>
                  <div className="mt-1.5 flex gap-2 text-[0.7rem] font-bold text-ink-3" aria-hidden="true">{MONTHS.map((m) => <span key={m} className="flex-1 text-center">{m}</span>)}</div>
                </div>
              </div>
            </div>
          </Reveal>
        </div>
      </div>
    </section>
  );
}
