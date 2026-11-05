export function LogoMark({ size = 30 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true" focusable="false" className="brand-mark">
      <path d="M4 7h24M16 7v22" stroke="currentColor" strokeWidth="3.4" strokeLinecap="round" />
      <circle cx="27" cy="25" r="3" fill="#b8801f" />
    </svg>
  );
}
export function Logo({ className = "" }: { className?: string }) {
  return <span className={`inline-flex items-center gap-2.5 ${className}`}><LogoMark /><span className="brand-wordmark">Tinward</span></span>;
}
