"use client";
import { useEffect, useState } from "react";
/** A local accessibility preference; system reduced-motion always takes precedence. */
export function MotionPreference() {
  const [reduced, setReduced] = useState(false);
  const [systemReduced, setSystemReduced] = useState(false);
  useEffect(() => {
    const media = window.matchMedia("(prefers-reduced-motion: reduce)");
    const sync = () => {setSystemReduced(media.matches);setReduced(document.documentElement.dataset.motion === "reduced");};
    sync();media.addEventListener("change",sync);
    return () => media.removeEventListener("change",sync);
  },[]);
  const toggle = () => {
    const value = !reduced;
    document.documentElement.dataset.motion = value ? "reduced" : "full";
    try { localStorage.setItem("tinward-motion",value ? "reduced" : "full"); } catch { /* The setting still works when storage is unavailable. */ }
    setReduced(value);window.dispatchEvent(new Event("tinward:motion"));
  };
  return <button type="button" className="motion-preference" aria-pressed={reduced || systemReduced} disabled={systemReduced} onClick={toggle}>{systemReduced ? "Reduced motion · system setting" : reduced ? "Motion reduced" : "Reduce motion"}</button>;
}
