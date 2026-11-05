"use client";

import { useState } from "react";
import { Reveal, LiveChip } from "../Reveal";
import { radioKeys } from "@/lib/radioKeys";
import { money } from "@/lib/format";

const SAMPLES = [
  { id: "grocer", sender: "VK-BANKSMS", text: "Rs.642.00 debited from a/c XX1234 on 03-11-26 to VPA greenleaf@upi. Not you? Call your bank.", parsed: { Amount: money(642, {locale: "en-IN", currency: "INR"}), Direction: "Money out", Account: "••1234", Paid: "Greenleaf Grocers", Category: "Food" }, tone: "ok", status: "Filed under Food from the merchant." },
  { id: "salary", sender: "VM-BANKSMS", text: "Rs.85,000.00 credited to a/c XX5678 on 01-11-26 by Acme Payroll. Avl bal Rs.1,18,430.", parsed: { Amount: money(85000, {locale: "en-IN", currency: "INR"}), Direction: "Money in", Account: "••5678", Paid: "Acme Payroll", Category: "Salary" }, tone: "ok", status: "Recognised as your salary." },
  { id: "person", sender: "VK-BANKSMS", text: "Rs.1,500.00 debited from a/c XX1234 on 04-11-26 to VPA 98xxxxxx10@upi. Not you? Call your bank.", parsed: { Amount: money(1500, {locale: "en-IN", currency: "INR"}), Direction: "Money out", Account: "••1234", Paid: "A person", Category: "Needs you" }, tone: "review", status: "Tinward can’t know what this was, so it leaves the purpose for you to review." },
] as const;

export function SmsTracking() {
  const [i, setI] = useState(0);
  const s = SAMPLES[i];
  return (
    <section id="features" className="border-t border-line">
      <div className="shell section">
        <div className="grid gap-14 lg:grid-cols-[0.9fr_1.1fr] lg:gap-24">
          <div>
            <Reveal><span className="eyebrow">Expense tracking</span></Reveal>
            <Reveal delay={80}><h2 className="h2 mt-6">From scattered records to a useful story.</h2></Reveal>
            <Reveal delay={160}>
              <p className="lead mt-8">When a supported bank sends a transaction message, Tinward can turn it into a useful record on your phone. Amount, account and purpose, with room to review what it can’t know.</p>
            </Reveal>
            <Reveal delay={220}><div className="mt-8"><LiveChip /></div></Reveal>
            <Reveal delay={260}>
              <p className="mt-8 max-w-md border-t border-line pt-5 text-[0.95rem] text-ink-2">
                <strong className="text-ink">Current regional support:</strong> Selected Indian message formats from HDFC, Kotak and Slice, plus a general UPI format. Broader coverage is not available yet. Messages Tinward doesn’t recognise are skipped, and a category can be wrong. That’s what the review queue is for.
              </p>
            </Reveal>
          </div>

          <Reveal delay={120}>
            <div>
              <p className="text-sm font-bold text-ink-2">Explore a supported-format example</p>
              <div role="radiogroup" aria-label="Sample bank messages" className="mt-3 grid gap-2">
                {SAMPLES.map((m, idx) => {
                  const on = idx === i;
                  return (
                    <button key={m.id} role="radio" aria-checked={on} tabIndex={on ? 0 : -1} onKeyDown={e => radioKeys(e, idx, SAMPLES.length, setI)} onClick={() => setI(idx)} className={`rounded-[10px] border px-4 py-3.5 text-left transition-colors ${on ? "border-ink bg-card" : "border-line hover:border-ink-3"}`}>
                      <span className="block text-[0.7rem] font-bold tracking-wider text-ink-3">{m.sender}</span>
                      <span className="mt-1 block font-mono text-[0.82rem] leading-snug text-ink-2">{m.text}</span>
                    </button>
                  );
                })}
              </div>
              <div className="mt-3 rounded-[14px] bg-ink p-6 text-paper sm:p-8" aria-live="polite">
                <p className="text-sm font-bold text-paper/55">How the current parser organises it</p>
                <dl className="mt-5 grid grid-cols-2 gap-x-6 gap-y-5 sm:grid-cols-3">
                  {Object.entries(s.parsed).map(([k, v]) => (
                    <div key={k}>
                      <dt className="text-xs font-semibold text-paper/50">{k}</dt>
                      <dd className={`mt-1 text-[1.1rem] font-bold ${k === "Category" ? (s.tone === "review" ? "text-saffron" : "text-app-positive") : ""}`}>{v}</dd>
                    </div>
                  ))}
                </dl>
                <p className="mt-6 border-t border-paper/15 pt-4 text-[0.95rem] text-paper/80">{s.status}</p>
              </div>
            </div>
          </Reveal>
        </div>
      </div>
    </section>
  );
}
