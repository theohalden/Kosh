/** Tinward mark: an ink "T" with an ochre square, redrawn as vector from the approved logo. */
export function LogoMark({ size = 30 }: { size?: number }) {
  return (
    <svg height={size} width={size * 1.23} viewBox="0 0 340 276" aria-hidden="true" focusable="false" className="brand-mark">
      <path className="bm-t" fill="currentColor" d="M24 15H212Q218 15 218 21V228Q218 268 178 268H132Q125 268 125 261V98H18Q10 98 10 90V29Q10 15 24 15Z" />
      <rect className="bm-sq" x="240" y="8" width="90" height="90" rx="10" fill="#b8801f" />
    </svg>
  );
}
export function Logo({ className = "" }: { className?: string }) {
  return <span className={`inline-flex items-center gap-3 ${className}`}><LogoMark /><span className="brand-wordmark">Tinward</span></span>;
}
