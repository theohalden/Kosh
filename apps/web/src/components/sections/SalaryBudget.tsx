"use client";

import { Reveal, PlannedChip } from "../Reveal";
import { useInView } from "@/lib/useInView";
import { month, plan, sum } from "@/content/sample";
import { money } from "@/lib/format";

export function SalaryBudget() {
  const [ref, seen] = useInView<HTMLDivElement>(0.25);
  const planned = sum(plan.map((p) => ({ value: p.plan })));
  const buffer = month.income - planned;
  const rows = [...plan.map((p) => ({ label: p.label, value: p.plan, color: p.color })), { label: "Not yet spoken for", value: buffer, color: "#121412" }];
  return (
    <section className="bg-paper-2/60" aria-labelledby="salary-h">
      <div className="shell section">
        <div className="grid gap-14 lg:grid-cols-[0.9fr_1.1fr] lg:gap-24">
          <div>
            <div className="flex items-center gap-5"><Reveal><span className="eyebrow">Budgets and goals</span></Reveal><PlannedChip /></div>
            <Reveal delay={80}><h2 id="salary-h" className="h2 mt-6">Know what’s left after life happens.</h2></Reveal>
            <Reveal delay={160}>
              <p className="lead mt-8">The balance in your account isn’t all available to spend. Tinward’s planned budget view will set aside bills, repayments and savings goals, so you can see what is genuinely left.</p>
            </Reveal>
            <Reveal delay={220}>
              <ul className="mt-10 grid gap-3 border-t border-line pt-6 text-[0.98rem] text-ink-2">
                {["Account for bills and loan payments before you spend", "Plan around your actual income", "Savings treated as a commitment, not a leftover"].map((t) => (
                  <li key={t} className="flex gap-3"><span className="mt-2.5 h-px w-4 flex-none bg-ink" aria-hidden="true" />{t}</li>
                ))}
              </ul>
            </Reveal>
          </div>

          <div ref={ref} className="paper-card p-6 sm:p-9">
            <div className="flex items-end justify-between gap-4">
              <div><p className="text-sm font-bold text-ink-2">This month’s income</p><p className="display num mt-1 text-[clamp(2.6rem,5vw,4rem)] leading-none tracking-[-0.03em]">{money(month.income)}</p></div>
              <p className="pb-1 text-sm text-ink-3">credited 1 Nov</p>
            </div>
            <div className="mt-6 flex h-3 gap-[3px]" role="img" aria-label={`Income of ${money(month.income)} planned across ${plan.map((p) => `${p.label} ${money(p.plan)}`).join(", ")}, with ${money(buffer)} not yet spoken for`}>
              {rows.map((r, i) => (
                <div key={r.label} className="h-full rounded-full" style={{ flexGrow: r.value, flexBasis: 0, opacity: seen ? 1 : 0.35, background: r.label === "Not yet spoken for" ? "transparent" : r.color, boxShadow: r.label === "Not yet spoken for" ? "inset 0 0 0 1.5px #a9a99f" : undefined, transition: `opacity 700ms var(--ease-out-quint) ${i * 80}ms` }} />
              ))}
            </div>
            <ul className="mt-6 divide-y divide-line">
              {rows.map((r) => (
                <li key={r.label} className="flex items-center gap-3 py-3 text-[0.98rem]">
                  <span className="h-2.5 w-2.5 flex-none rounded-full" style={{ background: r.label === "Not yet spoken for" ? "transparent" : r.color, boxShadow: r.label === "Not yet spoken for" ? "inset 0 0 0 1.5px #a9a99f" : undefined }} aria-hidden="true" />
                  <span className="flex-1 font-semibold">{r.label}</span>
                  <span className="num font-bold">{money(r.value)}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </section>
  );
}
