"use client";
import "./showcase.css";
import { useEffect, useRef, useState } from "react";
import { Tilt } from "../Tilt";
import { AppPhone, type Tab } from "../app/AppPhone";
import { SCREENS } from "../app/Screens";
const STEPS: { tab: Tab; kicker: string; h: string; p: string; note: string }[] = [
  {tab:"home",kicker:"Know what’s happening",h:"Your finances aren’t separate stories.",p:"They’re all part of the same picture. Tinward gives everyday spending, financial plans and the things you own a place to come together. Understand your income, spending and activity without the noise.",note:"Transaction summaries are in the Android app. Available-to-spend planning is in development."},
  {tab:"activity",kicker:"Know what’s happening",h:"Every movement has a place.",p:"Search transactions, see where money moved and correct its purpose, so transfers between your own accounts don’t look like new spending.",note:"Current tracking supports selected message and statement formats. Uncertain records still need review."},
  {tab:"plan",kicker:"Know what’s ahead",h:"Give tomorrow a little space.",p:"Stay mindful of your budgets, commitments and plans. See what is committed, what is flexible and what is left for you.",note:"Budgets and goals are in development."},
  {tab:"wealth",kicker:"Know where you stand",h:"See how it all fits together.",p:"See how your assets, savings and obligations fit together, using values you enter and update yourself.",note:"Manual investments and net worth are in development. No broker connection or live prices."},
];
export function Showcase() {
  const ref = useRef<HTMLElement>(null);
  const [visible, setVisible] = useState(false);
  const [step, setStep] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [reduced, setReduced] = useState(false);
  const [pinned, setPinned] = useState(false);
  const tabs = useRef<(HTMLButtonElement | null)[]>([]);
  useEffect(() => {
    const observer = new IntersectionObserver(([entry]) => setVisible(entry.isIntersecting), {threshold: 0});
    if (ref.current) observer.observe(ref.current);
    const media = window.matchMedia("(prefers-reduced-motion: reduce)");
    const update = () => {
      const shouldReduce = media.matches;
      setReduced(shouldReduce);
      if (shouldReduce) setPlaying(false);
    };
    const pauseWhenHidden = () => {if (document.hidden) setPlaying(false);};
    const pin = window.matchMedia("(min-width: 1024px) and (min-height: 560px) and (prefers-reduced-motion: no-preference)");
    const syncPin = () => { setPinned(pin.matches); if (pin.matches) setPlaying(false); };
    syncPin(); pin.addEventListener("change", syncPin);
    update(); media.addEventListener("change", update); document.addEventListener("visibilitychange", pauseWhenHidden);
    return () => { pin.removeEventListener("change", syncPin); observer.disconnect(); media.removeEventListener("change", update); document.removeEventListener("visibilitychange", pauseWhenHidden); };
  }, []);
  useEffect(() => {
    if (!playing || reduced || !visible) return;
    const timer = window.setInterval(() => { if (!document.hidden) setStep(s => (s + 1) % STEPS.length); }, 4500);
    return () => window.clearInterval(timer);
  }, [playing, reduced, visible]);
  /* Pinned mode: the section is taller than the screen and the phone stays put while scroll position picks the step. */
  useEffect(() => {
    if (!pinned) return;
    let raf = 0;
    const onScroll = () => {
      cancelAnimationFrame(raf);
      raf = requestAnimationFrame(() => {
        const el = ref.current; if (!el) return;
        const rect = el.getBoundingClientRect();
        const total = rect.height - window.innerHeight;
        if (total <= 0) return;
        const p = Math.min(0.999, Math.max(0, -rect.top / total));
        setStep(Math.floor(p * STEPS.length));
      });
    };
    onScroll(); window.addEventListener("scroll", onScroll, { passive: true });
    return () => { cancelAnimationFrame(raf); window.removeEventListener("scroll", onScroll); };
  }, [pinned]);
  const choose = (i: number) => {
    const n = (i + STEPS.length) % STEPS.length;
    setPlaying(false); setStep(n);
    const el = ref.current;
    if (pinned && el) {
      const total = el.getBoundingClientRect().height - window.innerHeight;
      window.scrollTo({ top: el.getBoundingClientRect().top + window.scrollY + ((n + 0.5) / STEPS.length) * total, behavior: "smooth" });
    }
  };
  return (
    <section id="product" ref={ref} className="sc-track" data-pinned={pinned} aria-labelledby="sc-h">
      <div className="sc-pin"><div className="shell sc-grid">
        <div className="sc-text">
          <p className="eyebrow">Introducing Tinward</p><h2 id="sc-h" className="sc-heading">One app.<br/><em>A wider view.</em></h2>
          <div className="sc-tabs" data-playing={playing} role="tablist" aria-label="Product screens">{STEPS.map((s,i) => <button key={s.tab} ref={el => {tabs.current[i]=el;}} role="tab" type="button" id={`tab-${s.tab}`} aria-selected={step===i} tabIndex={step===i?0:-1} aria-controls={`screen-${s.tab}`} onClick={() => choose(i)} onKeyDown={e => {const n=e.key==="ArrowRight"?(step+1)%4:e.key==="ArrowLeft"?(step+3)%4:e.key==="Home"?0:e.key==="End"?3:null;if(n!==null){e.preventDefault();choose(n);tabs.current[n]?.focus();}}}>{s.tab[0].toUpperCase()+s.tab.slice(1)}</button>)}</div>
          <div className="sc-story" key={step} aria-live={playing?"off":"polite"}><p className="sc-kicker">0{step+1} <span>{STEPS[step].kicker}</span></p><h3>{STEPS[step].h}</h3><p className="sc-p">{STEPS[step].p}</p><p className="sc-note">{STEPS[step].note}</p></div>
          <div className="sc-controls"><button type="button" aria-label="Previous product screen" onClick={() => choose(step-1)}>←</button><span>{String(step+1).padStart(2,"0")} / 04</span><button type="button" aria-label="Next product screen" onClick={() => choose(step+1)}>→</button>{!reduced && <button type="button" className="sc-play" aria-pressed={playing} onClick={() => setPlaying(!playing)}>{playing?"Pause tour":"Play tour"}</button>}</div>
          <p className="sc-hint" aria-hidden="true"><i/>Scroll to move through the app</p>
          <p className="sc-disclosure">Illustrative figures shown in your local currency, not customer data. These concepts include planned functionality; the Android app may look different.</p>
        </div>
        <div className="sc-stage"><span className="sc-stage-label">TINWARD / {STEPS[step].tab.toUpperCase()}</span><div className="sc-phone"><Tilt><AppPhone label={`Tinward ${STEPS[step].tab} concept with synthetic financial figures`}><div className="sc-screens">{STEPS.map((s,i) => {const Screen=SCREENS[s.tab];return <div key={s.tab} id={`screen-${s.tab}`} role="tabpanel" aria-labelledby={`tab-${s.tab}`} className="sc-screen" data-on={step===i} inert={step!==i} aria-hidden={step!==i}><Screen/></div>;})}</div></AppPhone></Tilt></div><span className="sc-stage-foot">A little more perspective.</span></div>
      </div></div>
    </section>
  );
}
