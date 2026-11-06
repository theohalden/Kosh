/** Plan definitions are separate from market-specific commercial prices.
 * Empty markets deliberately means no billing currency or price is offered yet. */
export const PLAN_DEFINITIONS = {
  free: { name: "Tinward Free", features: ["Income and expense tracking", "Organized transaction history", "Spending categories", "Everyday financial overview"] },
  plus: { name: "Tinward Plus", features: ["Additional planning tools", "Expanded financial views", "Flexible ways to manage financial records"] },
} as const;
export type BillingMarket = { country: string; locale: string; currency: string; monthly: number; annual: number };
export const PRICING = { status: "proposed", plans: PLAN_DEFINITIONS, markets: [] as BillingMarket[], checkoutEnabled: false } as const;
