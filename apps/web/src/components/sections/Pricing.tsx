import { Reveal } from "../Reveal";
import { PRICING } from "@/content/pricing";
export function Pricing() {
  return (
    <section id="pricing" className="shell section" aria-labelledby="pricing-h">
      <div className="grid gap-12 lg:grid-cols-[.8fr_1.2fr] lg:gap-20">
        <div><Reveal><span className="eyebrow">Keeping things simple</span><h2 id="pricing-h" className="h2 mt-6">Better financial clarity, without the complexity.</h2><p className="lead mt-8">Start with the essentials. Discover a more complete experience as Tinward grows.</p><p className="mt-6 text-sm text-ink-2">Plus availability, included features and pricing have not been finalized. No payment is taken on this website.</p></Reveal></div>
        <div className="grid gap-4 sm:grid-cols-2">{Object.entries(PRICING.plans).map(([key,plan]) => <Reveal key={key}><article className={`pricing-plan ${key==="plus"?"pricing-plus on-ink":"paper-card"}`}><p className="pricing-label">{key==="plus"?"PLANNED":"THE EVERYDAY TOOLS"}</p><h3>{plan.name}</h3><p className="pricing-state">{key==="free"?"The everyday tools for keeping your money in view.":"More depth for people who want to go further."}</p><ul>{plan.features.map(f => <li key={f}><span aria-hidden="true">↗</span>{f}</li>)}</ul>{key==="plus" && process.env.TINWARD_STATIC_EXPORT === "1" ? <span className="pricing-soon">Planned, not yet available</span> : <a href={key==="plus"?"#early-access":"#download"} className={`btn ${key==="plus"?"btn-light":"btn-ghost"}`}>{key==="plus"?"Join the Plus waitlist":"Get Tinward for Android"} <span aria-hidden="true">{key==="plus"?"↗":"↓"}</span></a>}</article></Reveal>)}</div>
      </div>
    </section>
  );
}
