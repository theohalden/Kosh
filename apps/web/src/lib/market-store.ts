/** Illustrative display market. Sample figures are authored in USD terms; `scale` re-expresses them at a
 *  believable local level (₹ ×10 matches the Indian bank-message examples). Not an exchange rate. */
export type Market = { region: string; locale: string; currency: string; scale: number };

export const DEFAULT_MARKET: Market = { region: "US", locale: "en-US", currency: "USD", scale: 1 };
const MARKETS: Record<string, Market> = {
  US: DEFAULT_MARKET,
  IN: { region: "IN", locale: "en-IN", currency: "INR", scale: 10 },
  GB: { region: "GB", locale: "en-GB", currency: "GBP", scale: 1 },
  AU: { region: "AU", locale: "en-AU", currency: "AUD", scale: 1 },
  CA: { region: "CA", locale: "en-CA", currency: "CAD", scale: 1 },
  EU: { region: "EU", locale: "en-IE", currency: "EUR", scale: 1 },
};
const EURO = new Set(["AT", "BE", "CY", "DE", "EE", "ES", "FI", "FR", "GR", "HR", "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PT", "SI", "SK"]);

export function marketForRegion(region: string | undefined): Market {
  const r = (region ?? "").toUpperCase();
  return MARKETS[r] ?? (EURO.has(r) ? MARKETS.EU : DEFAULT_MARKET);
}

const ZONES: [RegExp, string][] = [[/^Asia\/(Kolkata|Calcutta)$/, "IN"], [/^Europe\/London$/, "GB"], [/^Australia\//, "AU"], [/^America\/(Toronto|Vancouver|Edmonton|Winnipeg|Halifax|Regina)$/, "CA"], [/^Europe\/(Berlin|Paris|Madrid|Rome|Amsterdam|Dublin|Lisbon|Vienna|Brussels|Helsinki|Athens)$/, "EU"]];

/** Region from the browser: the time zone says where someone is, so it goes first; then the language region. */
export function detectMarket(languages: readonly string[], timeZone: string | undefined): Market {
  const hit = ZONES.find(([re]) => timeZone && re.test(timeZone));
  if (hit) return MARKETS[hit[1]];
  if (timeZone && /^(America|US)\//.test(timeZone)) return DEFAULT_MARKET;
  for (const l of languages) {
    const region = l.split("-")[1];
    if (region && region.length === 2) {
      const m = marketForRegion(region);
      if (m !== DEFAULT_MARKET || region.toUpperCase() === "US") return m;
    }
  }
  return DEFAULT_MARKET;
}

let current: Market = DEFAULT_MARKET;
const listeners = new Set<() => void>();
export const getMarket = () => current;
export const subscribeMarket = (fn: () => void) => { listeners.add(fn); return () => { listeners.delete(fn); }; };
export function setMarket(m: Market) { if (m.region === current.region) return; current = m; listeners.forEach((fn) => fn()); }
