"use client";
import { useMarket } from "@/lib/market";
import { money } from "@/lib/format";
/** An illustrative amount shown in the visitor's currency (usable from server components). */
export function Money({ value }: { value: number }) { useMarket(); return <>{money(value)}</>; }
