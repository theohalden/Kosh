import { getMarket } from "./market-store";

/** Illustrative display settings, independent of future billing markets. */
export const PREVIEW_REGION = { locale: "en-US", currency: "USD" } as const;

/** Format an illustrative amount. With no options it uses the visitor's market (value re-expressed at local scale);
 *  with explicit locale/currency it formats exactly as given. */
export function money(value: number, options: { locale?: string; currency?: string } = {}): string {
  const explicit = options.locale !== undefined || options.currency !== undefined;
  const m = getMarket();
  const v = explicit ? value : value * m.scale;
  return new Intl.NumberFormat(options.locale ?? m.locale, {
    style: "currency", currency: options.currency ?? m.currency,
    minimumFractionDigits: Number.isInteger(v) ? 0 : 2, maximumFractionDigits: 2,
  }).format(v);
}
