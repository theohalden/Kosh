import { Reveal } from "../Reveal";
import { PRICING } from "@/content/pricing";
export function Pricing() {
  return (
    <section id="pricing" className="shell section" aria-labelledby="pricing-h">
      <div className="grid gap-12 lg:grid-cols-[.8fr_1.2fr] lg:gap-20">
        <div><Reveal><span className="eyebrow">A simple starting point</span><h2 id="pricing-h" className="h2 mt-6">Clarity first.<br/>The rest, in time.</h2><p className="lead mt-8">Early access is free. We’re considering two plans, with room to grow when you need it.</p><p className="mt-6 text-sm text-ink-2">Proposed plans. Features and regional prices are not final. No payment is taken on this website.</p></Reveal></div>
        <div className="grid gap-4 sm:grid-cols-2">{Object.entries(PRICING.plans).map(([key,plan]) => <Reveal key={key}><article className={`pricing-plan ${key==="plus"?"pricing-plus on-ink":"paper-card"}`}><p className="pricing-label">{key==="plus"?"THE WIDER VIEW":"THE EVERYDAY ESSENTIALS"}</p><h3>{plan.name}</h3><p className="pricing-state">{key==="free"?"Planned free tier":"Price to be announced"}</p><ul>{plan.features.map(f => <li key={f}><span aria-hidden="true">↗</span>{f}</li>)}</ul><a href="#early-access" className={`btn ${key==="plus"?"btn-light":"btn-ghost"}`}>Join early access <span aria-hidden="true">↗</span></a></article></Reveal>)}</div>
      </div>
    </section>
  );
}
