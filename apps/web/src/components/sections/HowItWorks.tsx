import { Reveal } from "../Reveal";

const STEPS = [
  { n: "01", title: "Download the Android app", body: "Get the early Kosh APK from the download section. You need Android 8.0 or newer. Read the existing-user warning before attempting an update." },
  { n: "02", title: "Let Tinward read, or import", body: "Import a supported statement, allow supported message access, or add a transaction manually. Format coverage is limited; not every bank or region is supported." },
  { n: "03", title: "Settle the few it’s unsure about", body: "Tinward files what it recognises. Uncertain purposes and categories need review. Choose a category once and it sticks for that merchant." },
  { n: "04", title: "Watch the month make sense", body: "Follow your spending as it happens. Planning, investments and net worth join as those features arrive." },
];

export function HowItWorks() {
  return (
    <section id="how" className="shell section border-t border-line" aria-labelledby="how-h">
      <div className="grid gap-12 lg:grid-cols-[0.8fr_1.2fr] lg:gap-24">
        <div>
          <Reveal><span className="eyebrow">How it works</span></Reveal>
          <Reveal delay={80}><h2 id="how-h" className="h2 mt-6">Four small steps.</h2></Reveal>
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
