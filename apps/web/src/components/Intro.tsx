/** First-visit entrance. Pure CSS so it plays before hydration and never blocks the page:
 *  the mark is drawn, the dot drops in, the wordmark rises, then the curtain lifts onto the hero. */
export function Intro() {
  return (
    <div className="intro" aria-hidden="true">
      <div className="intro-stage">
        <svg className="intro-mark" height="96" width="118" viewBox="0 0 340 276" fill="none">
          <path className="intro-t" fill="currentColor" d="M24 15H212Q218 15 218 21V228Q218 268 178 268H132Q125 268 125 261V98H18Q10 98 10 90V29Q10 15 24 15Z" />
          <rect className="intro-dot" x="240" y="8" width="90" height="90" rx="10" fill="#b8801f" />
        </svg>
        <p className="intro-word">{"Tinward".split("").map((c, i) => <span key={i} style={{ animationDelay: `${1050 + i * 55}ms` }}>{c}</span>)}</p>
        <p className="intro-line">Your money. In perspective.</p>
        <i className="intro-bar" />
      </div>
    </div>
  );
}
