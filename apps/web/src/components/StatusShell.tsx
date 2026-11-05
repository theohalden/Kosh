import Link from "next/link";
import type { ReactNode } from "react";
import { Logo } from "./Logo";

/** Minimal chrome for transactional pages (confirmed / unsubscribe / delete / legal). */
export function StatusShell({ children, wide = false }: { children: ReactNode; wide?: boolean }) {
  return (
    <div className="min-h-svh">
      <header className="shell flex h-20 items-center"><Link href="/" aria-label="Tinward home" className="no-underline"><Logo /></Link></header>
      <main id="main" className={`shell pb-24 pt-10 ${wide ? "max-w-[860px]" : "max-w-[640px]"}`}>{children}</main>
    </div>
  );
}
