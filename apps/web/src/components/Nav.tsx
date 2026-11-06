"use client";
import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { Logo } from "./Logo";
const LINKS = [{ href: "#features", label: "Features" }, { href: "#how", label: "How it works" }, { href: "#privacy", label: "Privacy" }, { href: "#pricing", label: "Pricing" }];
export function Nav() {
  const [open, setOpen] = useState(false);
  const [solid, setSolid] = useState(false);
  const toggle = useRef<HTMLButtonElement>(null);
  const [active, setActive] = useState("");
  useEffect(() => {
    const ids = LINKS.map(l => l.href.slice(1));
    const obs = new IntersectionObserver(entries => { for (const e of entries) if (e.isIntersecting) setActive(e.target.id); }, { rootMargin: "-40% 0px -55% 0px" });
    ids.forEach(id => { const el = document.getElementById(id); if (el) obs.observe(el); });
    return () => obs.disconnect();
  }, []);
  useEffect(() => {
    const onScroll = () => setSolid(window.scrollY > 16);
    onScroll(); window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);
  useEffect(() => {
    if (!open) return;
    const onKey = (event: KeyboardEvent) => { if (event.key === "Escape") { setOpen(false); toggle.current?.focus(); } };
    const closeOnDesktop = () => { if (window.innerWidth >= 1024) setOpen(false); };
    document.addEventListener("keydown", onKey); window.addEventListener("resize", closeOnDesktop);
    return () => { document.removeEventListener("keydown", onKey); window.removeEventListener("resize", closeOnDesktop); };
  }, [open]);
  return (
    <header className="site-header" data-solid={solid || open}>
      <a href="#main" className="skip-link">Skip to content</a>
      <nav aria-label="Primary" className="shell nav-inner">
        <Link href="/#top" aria-label="Tinward home" className="nav-brand"><Logo /></Link>
        <ul className="nav-links">{LINKS.map(l => <li key={l.href}><a href={l.href} aria-current={active === l.href.slice(1) ? "true" : undefined}>{l.label}</a></li>)}</ul>
        <a href="#early-access" className="btn btn-primary nav-cta">Join the beta <span aria-hidden="true">↗</span></a>
        <button ref={toggle} type="button" className="menu-toggle" aria-expanded={open} aria-controls="mobile-menu" onClick={() => setOpen(!open)} aria-label={open ? "Close menu" : "Open menu"}>
          <svg width="22" height="22" viewBox="0 0 22 22" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">{open ? <path d="m5 5 12 12M17 5 5 17"/> : <path d="M3 7h16M3 15h16"/>}</svg>
        </button>
      </nav>
      {open && <div id="mobile-menu" className="shell mobile-menu"><ul>{LINKS.map(l => <li key={l.href}><a href={l.href} onClick={() => setOpen(false)}>{l.label}<span aria-hidden="true">↗</span></a></li>)}</ul><a href="#early-access" onClick={() => setOpen(false)} className="btn btn-primary">Join the beta <span aria-hidden="true">↗</span></a></div>}
    </header>
  );
}
