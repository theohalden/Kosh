import { describe, expect, it } from "vitest";
import { holdings, leftToSpend, month, netWorth, overview, plan, snapshots, sum, upcomingTotal } from "@/content/sample";
import { money } from "@/lib/format";
import { PRICING } from "@/content/site";

describe("illustrative figures stay internally consistent", () => {
  it("overview categories add up to spend", () => expect(sum(overview.categories)).toBe(overview.spent));
  it("plan: month-to-date spend per line equals total spent", () => expect(plan.reduce((a, p) => a + p.spent, 0)).toBe(month.spent));
  it("plan: lines + unallocated = income", () => expect(plan.reduce((a, p) => a + p.plan, 0) + 1094).toBe(month.income));
  it("left to spend = income - spent - upcoming = unallocated + remaining discretionary", () => {
    expect(leftToSpend).toBe(1914);
    expect(upcomingTotal).toBe(2744);
    const discretionary = plan.filter((p) => ["Food", "Transport", "Fun & shopping", "Other"].includes(p.label)).reduce((a, p) => a + p.plan - p.spent, 0);
    expect(1094 + discretionary).toBe(leftToSpend);
  });
  it("upcoming EMI/SIP/bills match the plan lines", () => {
    const by = (n: string) => month.upcoming.filter((u) => n.split("|").includes(u.name)).reduce((a, u) => a + u.amount, 0);
    expect(by("Loan payment")).toBe(plan.find((p) => p.label === "Loan payment")!.plan);
    expect(by("Investment contribution")).toBe(plan.find((p) => p.label === "Savings & investing")!.plan);
    const bills = plan.find((p) => p.label === "Bills")!;
    expect(bills.spent + by("Electricity|Broadband")).toBe(bills.plan);
  });
  it("bank balances on Home tie to net-worth bank bucket; snapshots end at net worth", () => {
    expect(month.accounts.reduce((a, x) => a + x.balance, 0)).toBe(netWorth.assets[0].value);
    expect(snapshots.at(-1)!.v).toBe(114600);
  });
  it("net worth = assets − liabilities (₹11,46,000)", () => {
    expect(sum(netWorth.assets) - sum(netWorth.liabilities)).toBe(114600);
    expect(sum(netWorth.assets)).toBe(159950);
  });
  it("investment holdings & retirement tie to the net-worth buckets", () => {
    const by = (ids: string[]) => holdings.filter((h) => ids.includes(h.id)).reduce((a, h) => a + h.value, 0);
    expect(by(["stocks", "mf", "fd", "retirement", "pension"])).toBe(netWorth.assets[1].value);
    expect(by(["gold", "other"])).toBe(netWorth.assets[2].value);
  });
  it("formats synthetic amounts independently of a billing market", () => {
    expect(money(114600)).toBe("$114,600");
    expect(money(-43500)).toBe("-$43,500");
    expect(money(64.2)).toBe("$64.20");
    expect(money(1146000, {locale: "en-IN", currency: "INR"})).toBe("₹11,46,000");
    expect(money(1234.5, {locale: "de-DE", currency: "EUR"})).toBe("1.234,50 €");
  });
  it("does not imply configured billing prices or accept payment", () => {
    expect(PRICING.status).toBe("proposed");
    expect(PRICING.checkoutEnabled).toBe(false);
    expect(PRICING.markets).toEqual([]);
    expect(PRICING.plans.free.name).toBe("Tinward Free");
    expect(PRICING.plans.plus.features).toContain("Additional planning tools");
  });
});
