"use client";
import "./showcase.css";
import { useEffect, useRef, useState } from "react";
import { AppPhone, type Tab } from "../app/AppPhone";
import { SCREENS } from "../app/Screens";
const STEPS: { tab: Tab; kicker: string; h: string; p: string; note: string }[] = [
  {tab:"home",kicker:"The everyday picture",h:"Less checking. More knowing.",p:"Income, spending and account activity, brought into one place. A calmer way to understand the month before getting lost in the details.",note:"Transaction summaries are in the Android app. Available-to-spend planning is in development."},
  {tab:"activity",kicker:"A story behind the numbers",h:"Every movement has a place.",p:"Search transactions, see where money moved and correct its purpose. Transfers between your accounts shouldn’t look like new spending.",note:"Current tracking supports selected message and statement formats. Uncertain records still need review."},
  {tab:"plan",kicker:"Room for what matters",h:"Give tomorrow a little space.",p:"The planned budget view sets bills, repayments and savings against income. See what is committed, what is flexible and what is left for you.",note:"Budgets and goals are in development."},
  {tab:"wealth",kicker:"Beyond the current balance",h:"See what you’re building.",p:"A place for the investments, assets and liabilities that make up your financial life. The planned tracker will use values you enter and update.",note:"Manual investments and net worth are in development. No broker connection or live prices."},
];
export function Showcase() {
  const ref = useRef<HTMLElement>(null);
  const [visible, setVisible] = useState(false);
  const [step, setStep] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [reduced, setReduced] = useState(false);
  const [hovered, setHovered] = useState(false);
  const [focused, setFocused] = useState(false);
  const tabs = useRef<(HTMLButtonElement | null)[]>([]);
  useEffect(() => {
    const observer = new IntersectionObserver(([entry]) => setVisible(entry.isIntersecting), {threshold: .2});
    if (ref.current) observer.observe(ref.current);
    const media = window.matchMedia("(prefers-reduced-motion: reduce)");
    const update = () => {
      const shouldReduce = media.matches || document.documentElement.dataset.motion === "reduced";
      setReduced(shouldReduce);
      if (shouldReduce) setPlaying(false);
    };
    const pauseWhenHidden = () => {if (document.hidden) setPlaying(false);};
    update(); media.addEventListener("change", update); window.addEventListener("tinward:motion", update); document.addEventListener("visibilitychange", pauseWhenHidden);
    return () => { observer.disconnect(); media.removeEventListener("change", update); window.removeEventListener("tinward:motion", update); document.removeEventListener("visibilitychange", pauseWhenHidden); };
  }, []);
  useEffect(() => {
    if (!playing || reduced || !visible || hovered || focused) return;
    const timer = window.setInterval(() => { if (!document.hidden) setStep(s => (s + 1) % STEPS.length); }, 4800);
    return () => window.clearInterval(timer);
  }, [playing, reduced, visible, hovered, focused]);
  const choose = (i: number) => { setPlaying(false); setStep((i + STEPS.length) % STEPS.length); };
  return (
    <section id="product" ref={ref} className="sc-track" aria-labelledby="sc-h" onMouseEnter={() => setHovered(true)} onMouseLeave={() => setHovered(false)} onFocusCapture={() => setFocused(true)} onBlurCapture={e => { if (!e.currentTarget.contains(e.relatedTarget)) setFocused(false); }}>
      <div className="shell sc-grid">
        <div className="sc-text">
          <p className="eyebrow">A closer look</p><h2 id="sc-h" className="sc-heading">One app.<br/><em>A wider view.</em></h2>
          <div className="sc-tabs" role="tablist" aria-label="Product screens">{STEPS.map((s,i) => <button key={s.tab} ref={el => {tabs.current[i]=el;}} role="tab" type="button" id={`tab-${s.tab}`} aria-selected={step===i} tabIndex={step===i?0:-1} aria-controls={`screen-${s.tab}`} onClick={() => choose(i)} onKeyDown={e => {const n=e.key==="ArrowRight"?(step+1)%4:e.key==="ArrowLeft"?(step+3)%4:e.key==="Home"?0:e.key==="End"?3:null;if(n!==null){e.preventDefault();choose(n);tabs.current[n]?.focus();}}}>{s.tab[0].toUpperCase()+s.tab.slice(1)}</button>)}</div>
          <div className="sc-story" aria-live={playing?"off":"polite"}><p className="sc-kicker">0{step+1} <span>{STEPS[step].kicker}</span></p><h3>{STEPS[step].h}</h3><p className="sc-p">{STEPS[step].p}</p><p className="sc-note">{STEPS[step].note}</p></div>
          <div className="sc-controls"><button type="button" aria-label="Previous product screen" onClick={() => choose(step-1)}>←</button><span>{String(step+1).padStart(2,"0")} / 04</span><button type="button" aria-label="Next product screen" onClick={() => choose(step+1)}>→</button>{!reduced && <button type="button" className="sc-play" aria-pressed={playing} onClick={() => {setPlaying(!playing);setFocused(false);}}>{playing?"Pause tour":"Play tour"}</button>}</div>
          <p className="sc-disclosure">Illustrative USD figures, not customer data. These concepts include planned functionality; the Android app may look different.</p>
        </div>
        <div className="sc-stage"><span className="sc-stage-label">TINWARD / {STEPS[step].tab.toUpperCase()}</span><div className="sc-phone"><AppPhone label={`Tinward ${STEPS[step].tab} concept with synthetic financial figures`}><div className="sc-screens">{STEPS.map((s,i) => {const Screen=SCREENS[s.tab];return <div key={s.tab} id={`screen-${s.tab}`} role="tabpanel" aria-labelledby={`tab-${s.tab}`} className="sc-screen" data-on={step===i} inert={step!==i} aria-hidden={step!==i}><Screen/></div>;})}</div></AppPhone></div><span className="sc-stage-foot">A little more perspective.</span></div>
      </div>
    </section>
  );
}
