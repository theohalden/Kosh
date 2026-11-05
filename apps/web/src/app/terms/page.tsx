import type { Metadata } from "next";
import { StatusShell } from "@/components/StatusShell";

export const metadata: Metadata = { title: "Terms", description: "Terms for using the Tinward website and joining early access." };

export default function Terms() {
  return (
    <StatusShell wide>
      <article className="text-[1.02rem] text-ink-2 [&_h2]:mt-12 [&_h2]:text-3xl [&_h2]:text-ink [&_li]:mt-2 [&_p]:mt-4 [&_ul]:mt-4 [&_ul]:list-disc [&_ul]:pl-6">
        <h1 className="text-5xl text-ink">Terms</h1>
        <p>Last updated 5 November 2026.</p>
        <h2>This site</h2>
        <p>This website describes Tinward, a personal finance app for Android that is in early development. Nothing on it is an offer to sell you anything, and it is not financial, investment, tax or legal advice.</p>
        <h2>Early access</h2>
        <ul>
          <li>Joining the early-access list is free and does not require payment.</li>
          <li>Joining doesn&apos;t guarantee an invitation, a launch date, or any particular feature.</li>
          <li>Pricing shown on the site is planned and preliminary. Prices and what each tier includes may change, and no payment is taken today.</li>
        </ul>
        <h2>Illustrative content</h2>
        <p>Amounts, transactions, names and screens shown on this site are synthetic examples, not data from real people, and not screenshots of a released product. Features marked &ldquo;planned&rdquo; are not yet built.</p>
        <h2>Accuracy</h2>
        <p>The app&apos;s parsing and categorisation are incomplete. Totals should be verified against your bank records before you rely on them.</p>
        <h2>Open source</h2>
        <p>The Android app&apos;s source code is released under the MIT licence; its own licence text governs your use of that code.</p>
      </article>
    </StatusShell>
  );
}
