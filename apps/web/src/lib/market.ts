"use client";
import { useEffect, useSyncExternalStore } from "react";
import { DEFAULT_MARKET, detectMarket, getMarket, setMarket, subscribeMarket } from "./market-store";

/** Subscribe a component to the visitor's market so `money()` re-renders in their currency. */
export function useMarket() {
  return useSyncExternalStore(subscribeMarket, getMarket, () => DEFAULT_MARKET);
}

/** Runs once on the client: picks the market from the browser's region, then tells subscribers. */
export function useDetectMarket() {
  useEffect(() => {
    let tz: string | undefined;
    try { tz = Intl.DateTimeFormat().resolvedOptions().timeZone; } catch { /* default market */ }
    setMarket(detectMarket(navigator.languages?.length ? navigator.languages : [navigator.language], tz));
  }, []);
}
