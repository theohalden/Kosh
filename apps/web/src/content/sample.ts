/**
 * Every figure on the marketing site is ILLUSTRATIVE sample data.
 * It is not a real person's money and not a screenshot of a shipping build.
 * Keep totals internally consistent – tests/sample.test.ts checks the arithmetic.
 */

export type Slice = { label: string; value: number; color: string };

/** Month-to-date spending shown in the (existing) Overview screen. */
export const overview = {
  month: "November",
  income: 8500,
  spent: 3842,
  categories: [
    { label: "Rent", value: 2200, color: "#d5a64f" },
    { label: "Food", value: 694, color: "#f1a292" },
    { label: "Bills", value: 312, color: "#7fAEdb" },
    { label: "Shopping", value: 264, color: "#c8a7db" },
    { label: "Transport", value: 218, color: "#6fbfa3" },
    { label: "Other", value: 154, color: "#8b8782" },
  ] satisfies Slice[],
};

export type Txn = { merchant: string; category: string; amount: number; when: string; kind: "debit" | "credit"; color: string };

export const transactions: Txn[] = [
  { merchant: "Greenleaf Grocers", category: "Food", amount: 64.2, when: "Today", kind: "debit", color: "#f1a292" },
  { merchant: "CityRide", category: "Transport", amount: 21.8, when: "Today", kind: "debit", color: "#6fbfa3" },
  { merchant: "Acme Payroll", category: "Income", amount: 8500, when: "1 Nov", kind: "credit", color: "#b7d5ae" },
  { merchant: "StreamBox", category: "Subscriptions", amount: 49.9, when: "2 Nov", kind: "debit", color: "#c8a7db" },
  { merchant: "Bean & Brew", category: "Food", amount: 28, when: "2 Nov", kind: "debit", color: "#f1a292" },
];

/** Planned budget – illustrative. Allocations + free-to-spend must equal income. */
export const budget = {
  income: 8500,
  lines: [
    { label: "Rent", value: 2200, color: "#d5a64f" },
    { label: "Loan payment", value: 1240, color: "#9a93d9" },
    { label: "Bills", value: 520, color: "#7faedb" },
    { label: "Savings & investing", value: 1200, color: "#6fbfa3" },
    { label: "Food", value: 1000, color: "#f1a292" },
    { label: "Transport", value: 350, color: "#c8a7db" },
    { label: "Fun & shopping", value: 600, color: "#e08a7d" },
  ] satisfies Slice[],
  freeToSpend: 1390,
  daysLeft: 30,
};

export const bills = [
  { name: "Electricity", due: "12 Nov", amount: 184 },
  { name: "Broadband", due: "15 Nov", amount: 120 },
  { name: "Loan payment", due: "14 Nov", amount: 1240 },
];

/** Planned manual holdings – illustrative. */
export const holdings = [
  { id: "stocks", label: "Stocks", value: 18450, updated: "Updated 2 days ago", hint: "Record each holding, then refresh its value when you check your broker." },
  { id: "mf", label: "Mutual funds", value: 32500, updated: "Updated 5 days ago", hint: "Track units or invested value; update the current value monthly." },
  { id: "fd", label: "Term deposits", value: 20000, updated: "Matures Mar 2027", hint: "Principal, rate and maturity date, entered once." },
  { id: "retirement", label: "Retirement fund", value: 46200, updated: "Updated 3 weeks ago", hint: "Copy the balance from your provider statement whenever you like." },
  { id: "pension", label: "Pension", value: 12000, updated: "Updated 3 weeks ago", hint: "One balance, one yearly reminder to refresh it." },
  { id: "gold", label: "Gold", value: 9600, updated: "Updated last month", hint: "Grams held and the value you want to carry." },
  { id: "other", label: "Other assets", value: 7000, updated: "Updated last month", hint: "Anything else you want counted: a bike, a deposit, a loan you gave." },
];

export const netWorth = {
  assets: [
    { label: "Bank balances", value: 14200, color: "#7d8f78" },
    { label: "Investments & retirement", value: 129150, color: "#b8801f" },
    { label: "Gold & other assets", value: 16600, color: "#cdc2a2" },
  ] satisfies Slice[],
  liabilities: [
    { label: "Car loan outstanding", value: 43500, color: "#a05a48" },
    { label: "Credit card due", value: 1850, color: "#cf9887" },
  ] satisfies Slice[],
};

export const sum = (xs: { value: number }[]) => xs.reduce((a, x) => a + x.value, 0);

/* ───────── Home / Plan screens (single source of truth; consistency checked in tests/sample.test.ts) ───────── */
export const month = {
  label: "November",
  today: "8 Nov",
  income: 8500,
  spent: 3842,
  daysLeft: 23,
  upcoming: [
    { name: "Electricity", date: "12 Nov", amount: 184, icon: "bolt" },
    { name: "Loan payment", date: "14 Nov", amount: 1240, icon: "calendar" },
    { name: "Broadband", date: "15 Nov", amount: 120, icon: "bolt" },
    { name: "Investment contribution", date: "20 Nov", amount: 1200, icon: "wealth" },
  ],
  accounts: [
    { name: "Income account", mask: "5678", balance: 11843 },
    { name: "Spending account", mask: "1234", balance: 2357 },
  ],
};
export const upcomingTotal = month.upcoming.reduce((a, u) => a + u.amount, 0);
export const leftToSpend = month.income - month.spent - upcomingTotal;

export const plan = [
  { label: "Rent", plan: 2200, spent: 2200, color: "#d5a64f" },
  { label: "Loan payment", plan: 1240, spent: 0, color: "#9a93d9" },
  { label: "Bills", plan: 616, spent: 312, color: "#7faedb" },
  { label: "Savings & investing", plan: 1200, spent: 0, color: "#6fbfa3" },
  { label: "Food", plan: 1000, spent: 694, color: "#f1a292" },
  { label: "Transport", plan: 350, spent: 218, color: "#c8a7db" },
  { label: "Fun & shopping", plan: 600, spent: 264, color: "#e08a7d" },
  { label: "Other", plan: 200, spent: 154, color: "#8b8782" },
];

export type Activity = { id: string; name: string; meta: string; amount: number; kind: "out" | "in" | "transfer"; icon: string };
export const activity: { day: string; items: Activity[] }[] = [
  { day: "Today", items: [
    { id: "a1", name: "Greenleaf Grocers", meta: "Food · Card", amount: 64.2, kind: "out", icon: "food" },
    { id: "a2", name: "CityRide", meta: "Transport · Card", amount: 21.8, kind: "out", icon: "car" },
  ] },
  { day: "Yesterday", items: [
    { id: "a3", name: "Bean & Brew", meta: "Food · Card", amount: 28, kind: "out", icon: "food" },
    { id: "a4", name: "To savings ••5678", meta: "Transfer · own account", amount: 1000, kind: "transfer", icon: "swap" },
  ] },
  { day: "6 Nov", items: [
    { id: "a5", name: "StreamBox", meta: "Subscriptions · Card ••8841", amount: 49.9, kind: "out", icon: "play" },
  ] },
  { day: "1 Nov", items: [
    { id: "a6", name: "Acme Payroll", meta: "Income · Bank transfer", amount: 8500, kind: "in", icon: "down" },
    { id: "a7", name: "Rent · Avery", meta: "Rent · Bank transfer", amount: 2200, kind: "out", icon: "home" },
  ] },
];

export const snapshots = [
  { m: "Jun", v: 94000 }, { m: "Jul", v: 97500 }, { m: "Aug", v: 101200 }, { m: "Sep", v: 104900 }, { m: "Oct", v: 109400 }, { m: "Nov", v: 114600 },
];
