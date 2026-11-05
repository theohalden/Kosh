import type { Metadata } from "next";
import { StatusShell } from "@/components/StatusShell";
import { TokenAction } from "@/components/TokenAction";

export const metadata: Metadata = { title: "Unsubscribe", robots: { index: false } };

export default async function Unsubscribe({ searchParams }: { searchParams: Promise<{ token?: string }> }) {
  const { token = "" } = await searchParams;
  return (
    <StatusShell>
      <TokenAction endpoint="/api/waitlist/unsubscribe" token={token} idleTitle="Unsubscribe from Tinward emails?" idleBody="You’ll stop receiving invitations and product news. You can rejoin from the website at any time." button="Yes, unsubscribe me" doneTitle="You’re unsubscribed." doneBody="We won’t email you again. If you’d also like your details removed entirely, you can request deletion from our privacy page." />
    </StatusShell>
  );
}
