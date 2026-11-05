/** Illustrative display settings, independent of future billing markets. */
export const PREVIEW_REGION = { locale: "en-US", currency: "USD" } as const;
export function money(value: number, options: { locale?: string; currency?: string } = {}): string {
  return new Intl.NumberFormat(options.locale ?? PREVIEW_REGION.locale, {
    style: "currency", currency: options.currency ?? PREVIEW_REGION.currency,
    minimumFractionDigits: Number.isInteger(value) ? 0 : 2, maximumFractionDigits: 2,
  }).format(value);
}
