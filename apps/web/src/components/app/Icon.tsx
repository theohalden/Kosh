/** One consistent 24px stroke icon set for the product UI (stroke 1.8, round caps). */
const P: Record<string, string> = {
  home: "M4 11.2 12 4.5l8 6.7V19a1 1 0 0 1-1 1h-4.2v-5.4H9.2V20H5a1 1 0 0 1-1-1z",
  activity: "M5 7h14M5 12h14M5 17h8",
  plan: "M12 3.5a8.5 8.5 0 1 0 8.5 8.5H12z M15 3.9a8.5 8.5 0 0 1 5.1 5.1H15z",
  wealth: "M5 19v-6m5 6V6m5 13v-9m4 9H3",
  search: "M10.5 4a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13zm9.5 16-4.7-4.7",
  filter: "M4 7h16M7 12h10M10 17h4",
  food: "M7 3.5v7.2M4.8 3.5v4.6a2.2 2.2 0 0 0 4.4 0V3.5M7 10.7V20.5M16.5 20.5V3.5c-2.3 1.4-3.3 4-3.3 6.5 0 1.3.8 2.2 1.9 2.4h1.4",
  car: "M4.5 16.5v-4.3l1.8-4.7a1 1 0 0 1 .9-.7h9.6a1 1 0 0 1 .9.7l1.8 4.7v4.3zM4 16.5h16M7.5 19.8v-3.3m9 3.3v-3.3M7.8 12.5h8.4",
  bag: "M5.5 8.5h13l.9 11.2a.8.8 0 0 1-.8.8H5.4a.8.8 0 0 1-.8-.8zM9 8.5V7a3 3 0 0 1 6 0v1.5",
  bolt: "M13.2 3.5 6 13.5h5.2l-.9 7 7.2-10h-5.2z",
  play: "M8.5 5.5v13l10-6.5z",
  down: "M12 5v13.5m-5.5-5.5 5.5 5.5 5.5-5.5",
  swap: "M6.5 8h11.5m-3-3 3 3-3 3M17.5 16H6m3 3-3-3 3-3",
  calendar: "M5 6.5h14v13H5zM5 10.5h14M9 4v4m6-4v4",
  chevron: "m6.5 9.5 5.5 5.5 5.5-5.5",
  right: "m9.5 6.5 5.5 5.5-5.5 5.5",
  up: "m6.5 14.5 5.5-5.5 5.5 5.5",
  check: "m5.5 12.5 4.2 4.2 8.8-9.4",
  loan: "M4 9.5 12 5l8 4.5M6 10.5V17m4-6.5V17m4-6.5V17m4-6.5V17M4 19.5h16",
};

export function Icon({ name, className, strokeWidth = 1.8 }: { name: keyof typeof P | string; className?: string; strokeWidth?: number }) {
  return (
    <svg viewBox="0 0 24 24" className={className} fill="none" stroke="currentColor" strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
      <path d={P[name] ?? P.activity} />
    </svg>
  );
}
