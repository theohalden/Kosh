import type { KeyboardEvent } from "react";
/** Native-style roving focus for the custom preview radio groups. */
export function radioKeys(event: KeyboardEvent<HTMLButtonElement>, index: number, count: number, select: (next: number) => void) {
  const next = event.key === "ArrowRight" || event.key === "ArrowDown" ? (index + 1) % count
    : event.key === "ArrowLeft" || event.key === "ArrowUp" ? (index + count - 1) % count
    : event.key === "Home" ? 0 : event.key === "End" ? count - 1 : null;
  if (next === null) return;
  event.preventDefault();
  select(next);
  event.currentTarget.closest('[role="radiogroup"]')?.querySelectorAll<HTMLButtonElement>('[role="radio"]')[next]?.focus();
}
