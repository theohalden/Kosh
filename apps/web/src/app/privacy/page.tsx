import type { Metadata } from "next";
import { StatusShell } from "@/components/StatusShell";
import { DeletionRequest } from "@/components/DeletionRequest";
import { REPO_URL } from "@/content/site";

export const metadata: Metadata = { title: "Privacy policy", description: "What the Tinward website collects for early access, and what the Android app does with your data." };

const H = ({ children }: { children: React.ReactNode }) => <h2 className="mt-12 text-3xl">{children}</h2>;

export default function Privacy() {
  return (
    <StatusShell wide>
      <article className="[&_li]:mt-2 [&_p]:mt-4 [&_ul]:mt-4 [&_ul]:list-disc [&_ul]:pl-6 text-[1.02rem] text-ink-2">
        <h1 className="text-5xl text-ink">Privacy policy</h1>
        <p>Last updated 5 November 2026. This covers two separate things: the <strong>website</strong> (this site) and the <strong>Android app</strong>.</p>

        <H>The website: early-access list</H>
        <p>When you join the list we store:</p>
        <ul>
          <li>your <strong>email address</strong> (required);</li>
          <li>your <strong>optional interest</strong> choice;</li>
          <li>whether you ticked the optional <strong>product-news</strong> box, with the date and the wording version you saw;</li>
          <li>when you signed up, when you confirmed your email, and when (if ever) you were invited or unsubscribed.</li>
        </ul>
        <p>We do not ask for bank details, phone numbers, SMS content, or payment information. To limit abuse we briefly keep salted hashes of IP addresses (never the addresses themselves) in a rate-limit counter that expires within days.</p>
        <p><strong>Why:</strong> to confirm your address, run the early-access list, and email you an invitation. If you ticked the product-news box we may also send occasional news about Tinward. You&apos;ll receive your invitation either way. We don&apos;t sell or share the list, and we don&apos;t use advertising trackers on this site.</p>
        <p><strong>Who handles it:</strong> our database host (Supabase) and email provider (Resend) process this data on our behalf.</p>
        <p><strong>Keeping it:</strong> until the beta programme ends or you unsubscribe or ask for deletion, whichever is first.</p>

        <H>Unsubscribing</H>
        <p>Every invitation email includes an unsubscribe link. Unsubscribing stops all email and clears your product-news consent.</p>

        <h2 id="delete" className="mt-12 scroll-mt-24 text-3xl">Delete my data</h2>
        <p>Enter the address you signed up with. If it&apos;s on the list we&apos;ll email a confirmation link; clicking it permanently deletes your record. We show the same message either way so this form can&apos;t be used to check who is on the list.</p>
        <div className="mt-5"><DeletionRequest /></div>

        <H>The Android app</H>
        <p>The app is open source and, at the time of writing, still in early development. In the current code:</p>
        <ul>
          <li>transactions, the original SMS text and learned merchant categories are kept in a local database on your device;</li>
          <li>the manifest requests <code>READ_SMS</code> and <code>RECEIVE_SMS</code> to import and capture financial messages, and does not declare the <code>INTERNET</code> permission;</li>
          <li>there is no finance-data upload and no analytics integration;</li>
          <li>Android application backup is disabled;</li>
          <li>the local database is <strong>not</strong> currently encrypted by Tinward.</li>
        </ul>
        <p>You can verify this in the <a className="underline" href={REPO_URL} target="_blank" rel="noopener noreferrer">source code</a>. This section describes the code as of the date above and will be updated before any public release.</p>

        <H>Contact</H>
        <p>For questions that don&apos;t involve personal data, open an issue on <a className="underline" href={`${REPO_URL}/issues`} target="_blank" rel="noopener noreferrer">GitHub</a>. For deletion, use the form above.</p>
      </article>
    </StatusShell>
  );
}
