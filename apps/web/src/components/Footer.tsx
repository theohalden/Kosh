import Link from "next/link";
import { Logo } from "./Logo";

const COLUMNS: { title: string; links: [string, string][] }[] = [
  { title: "Product", links: [["Features", "/#features"], ["How it works", "/#how"], ["Pricing", "/#pricing"]] },
  { title: "Resources", links: [["FAQ", "/#faq"], ["Join the beta", "/#early-access"]] },
  { title: "Legal", links: [["Privacy Policy", "/privacy"], ["Terms of Use", "/terms"]] },
];

export function Footer() {
  return (
    <footer className="site-footer on-ink">
      <div className="shell footer-inner">
        <div className="footer-top">
          <div className="footer-brand">
            <Logo />
            <p className="footer-tagline">Your money. In perspective.</p>
            <p className="footer-sub">A more thoughtful way to understand your financial life.</p>
            <Link href="/#early-access" className="btn btn-light footer-cta">Join the Android beta <span aria-hidden="true">↗</span></Link>
          </div>
          <nav aria-label="Footer" className="footer-cols">
            {COLUMNS.map((c) => (
              <div key={c.title}>
                <h2 className="footer-heading">{c.title}</h2>
                <ul>{c.links.map(([label, href]) => <li key={href}><Link href={href} className="footer-link">{label}</Link></li>)}</ul>
              </div>
            ))}
          </nav>
        </div>
        <p className="footer-mark" aria-hidden="true">Tinward</p>
        <p className="footer-legal">© 2026 Tinward.</p>
      </div>
    </footer>
  );
}
