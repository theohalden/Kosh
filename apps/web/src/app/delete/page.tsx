import type { Metadata } from "next";
import { StatusShell } from "@/components/StatusShell";
import { TokenAction } from "@/components/TokenAction";

export const metadata: Metadata = { title: "Delete my data", robots: { index: false } };

export default async function DeleteConfirm({ searchParams }: { searchParams: Promise<{ token?: string }> }) {
  const { token = "" } = await searchParams;
  return (
    <StatusShell>
      <TokenAction danger endpoint="/api/waitlist/delete/confirm" token={token} idleTitle="Delete your early-access data?" idleBody="This permanently removes your email, preferences and consent records from the Tinward early-access list. It can’t be undone." button="Permanently delete my data" doneTitle="Your data has been deleted." doneBody="Your details are gone from the early-access list. You’re welcome to sign up again whenever you like." />
    </StatusShell>
  );
}
