import "./app.css";
import type { ReactNode } from "react";
import { Icon } from "./Icon";

export type Tab = "home" | "activity" | "plan" | "wealth";
const TABS: { id: Tab; label: string }[] = [
  { id: "home", label: "Home" },
  { id: "activity", label: "Activity" },
  { id: "plan", label: "Plan" },
  { id: "wealth", label: "Wealth" },
];

export function NavBar({ active }: { active: Tab }) {
  return (
    <div className="ui-nav">
      {TABS.map((t) => (
        <span key={t.id} className="i" aria-current={t.id === active}>
          <span className="pill"><Icon name={t.id} /></span>
          {t.label}
        </span>
      ))}
    </div>
  );
}

function SystemIcons() {
  return (
    <span className="sys" aria-hidden="true">
      <svg viewBox="0 0 16 16"><path d="M1 12.5 14.5 3v9.5z" opacity=".9" /></svg>
      <svg viewBox="0 0 16 16"><path d="M8 3C5.4 3 3.1 4 1.4 5.700L8 13l6.600-7.300C12.900 4 10.600 3 8 3z" /></svg>
      <svg viewBox="0 0 16 16"><rect x="3.500" y="1.500" width="9" height="13" rx="2" /></svg>
    </span>
  );
}

/** Android phone: punch-hole camera, system status bar, gesture-navigation pill. Content goes inside <PhoneBody>. */
export function AppPhone({ children, className = "", label }: { children: ReactNode; className?: string; label?: string }) {
  return (
    <div className={`ui-phone ${className}`} role={label ? "img" : undefined} aria-label={label} aria-hidden={label ? undefined : true}>
      <div className="ui-phone-in">
        <span className="ui-cam" />
        <div className="ui-status"><span>8:12</span><SystemIcons /></div>
        {children}
        <div className="ui-gesture"><i /></div>
      </div>
    </div>
  );
}

export const PhoneBody = ({ children }: { children: ReactNode }) => <div className="ui-body">{children}</div>;
