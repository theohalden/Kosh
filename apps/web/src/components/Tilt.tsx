"use client";
import { useRef, type ReactNode } from "react";

/** Subtle pointer-follow tilt for product phones. Fine pointers only; off for reduced motion. */
export function Tilt({ children, max = 7 }: { children: ReactNode; max?: number }) {
  const ref = useRef<HTMLDivElement>(null);
  const set = (rx: number, ry: number) => { const el = ref.current; if (el) { el.style.setProperty("--rx", `${rx}deg`); el.style.setProperty("--ry", `${ry}deg`); } };
  const enabled = (e: React.PointerEvent) => e.pointerType === "mouse" && !window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  return (
    <div
      ref={ref}
      className="tilt"
      onPointerMove={(e) => {
        if (!enabled(e)) return;
        const r = e.currentTarget.getBoundingClientRect();
        const x = (e.clientX - r.left) / r.width - 0.5;
        const y = (e.clientY - r.top) / r.height - 0.5;
        set(-y * max, x * max);
      }}
      onPointerLeave={() => set(0, 0)}
    >
      {children}
    </div>
  );
}
