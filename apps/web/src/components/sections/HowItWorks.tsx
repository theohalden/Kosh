import { Reveal } from "../Reveal";

const STEPS = [
  { n: "01", title: "Start with what you know.", body: "Choose your currency and add the financial details you want to track." },
  { n: "02", title: "Make it a habit.", body: "Record your everyday activity and organize it in a way that makes sense to you." },
  { n: "03", title: "See the patterns.", body: "Review your spending, keep your plans in view, and understand your financial position." },
  { n: "04", title: "Keep moving forward.", body: "Return whenever you need a clearer perspective on your money." },
];

export function HowItWorks() {
  return (
    <section id="how" className="shell section border-t border-line" aria-labelledby="how-h">
      <div className="grid gap-12 lg:grid-cols-[0.8fr_1.2fr] lg:gap-24">
        <div>
          <Reveal><span className="eyebrow">Beautifully straightforward</span></Reveal>
          <Reveal delay={80}><h2 id="how-h" className="h2 mt-6">Small steps. A clearer picture.</h2></Reveal>
        </div>
        <ol className="divide-y divide-line border-y border-line">
          {STEPS.map((s, i) => (
            <li key={s.n}>
              <Reveal delay={i * 70} className="grid gap-3 py-8 sm:grid-cols-[5rem_1fr] sm:gap-8">
                <span className="display text-[2.2rem] leading-none text-coral" aria-hidden="true">{s.n}</span>
                <div>
                  <h3 className="text-[clamp(1.6rem,2.6vw,2.2rem)] leading-tight tracking-[-0.02em]"><span className="sr-only">Step {i + 1}: </span>{s.title}</h3>
                  <p className="mt-3 max-w-lg text-ink-2">{s.body}</p>
                </div>
              </Reveal>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
