/** Plan definitions are separate from market-specific commercial prices.
 * Empty markets deliberately means no billing currency or price is offered yet. */
export const PLAN_DEFINITIONS = {
  free: { name: "Tinward Free", features: ["Spending and income tracking", "Transaction history", "Categories you can correct", "Monthly summaries"] },
  plus: { name: "Tinward Plus", features: ["Budgets and financial goals", "Manual investment tracking", "Assets and liabilities", "Net-worth history"] },
} as const;
export type BillingMarket = { country: string; locale: string; currency: string; monthly: number; annual: number };
export const PRICING = { status: "proposed", plans: PLAN_DEFINITIONS, markets: [] as BillingMarket[], checkoutEnabled: false } as const;
