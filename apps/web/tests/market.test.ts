import { afterEach, describe, expect, it } from "vitest";
import { detectMarket, DEFAULT_MARKET, setMarket } from "@/lib/market-store";
import { money } from "@/lib/format";

describe("regional currency", () => {
  afterEach(() => setMarket(DEFAULT_MARKET));
  it("picks the market from the language region", () => {
    expect(detectMarket(["en-IN"], undefined).currency).toBe("INR");
    expect(detectMarket(["en-GB"], undefined).currency).toBe("GBP");
    expect(detectMarket(["de-DE"], undefined).currency).toBe("EUR");
    expect(detectMarket(["en-GB", "en-IN"], "Asia/Calcutta").currency).toBe("INR");
    expect(detectMarket(["en-IN"], "America/New_York").currency).toBe("USD");
  });
  it("falls back to the time zone, then USD", () => {
    expect(detectMarket(["en"], "Asia/Kolkata").currency).toBe("INR");
    expect(detectMarket(["en"], "Europe/Paris").currency).toBe("EUR");
    expect(detectMarket(["ja-JP"], "Asia/Tokyo")).toBe(DEFAULT_MARKET);
  });
  it("formats sample amounts at local scale; explicit options are untouched", () => {
    setMarket(detectMarket(["en-IN"], undefined));
    expect(money(114600)).toBe("₹11,46,000");
    expect(money(64.2)).toBe("₹642");
    expect(money(114600, { locale: "en-US", currency: "USD" })).toBe("$114,600");
  });
});
