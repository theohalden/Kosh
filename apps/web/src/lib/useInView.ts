"use client";

import { useEffect, useRef, useState } from "react";

/** True once the element has entered the viewport (one-shot). Falls back to true without IntersectionObserver. */
export function useInView<T extends HTMLElement>(threshold = 0.25) {
  const ref = useRef<T>(null);
  const [seen, setSeen] = useState(false);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    if (typeof IntersectionObserver === "undefined") { const id = requestAnimationFrame(() => setSeen(true)); return () => cancelAnimationFrame(id); }
    const io = new IntersectionObserver(([e]) => { if (e.isIntersecting) { setSeen(true); io.disconnect(); } }, { threshold });
    io.observe(el);
    return () => io.disconnect();
  }, [threshold]);
  return [ref, seen] as const;
}
