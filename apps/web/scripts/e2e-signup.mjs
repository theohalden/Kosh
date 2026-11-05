// End-to-end: real browser -> form -> API -> (dev memory repo + dev outbox) -> verify link -> duplicate state.
import { chromium } from "playwright";
const base = "http://localhost:3001";
const b = await chromium.launch({ channel: "chrome" });
const p = await (await b.newContext({ viewport: { width: 1280, height: 900 } })).newPage();
const errs = [];
p.on("console", (m) => { if (m.type() === "error") errs.push(m.text()); });
p.on("pageerror", (e) => errs.push(e.message));
const email = `e2e+${Date.now()}@example.com`;
await p.goto(base, { waitUntil: "networkidle" });
await p.locator("#early-access").scrollIntoViewIfNeeded();
// 1. validation
await p.getByRole("button", { name: /Join early access/ }).click();
console.log("empty ->", await p.getByRole("alert").first().innerText());
await p.getByLabel(/Email address/).fill("not-an-email");
await p.getByRole("button", { name: /Join early access/ }).click();
console.log("bad  ->", await p.getByRole("alert").first().innerText());
// 2. success
await p.getByLabel(/Email address/).fill(email);
await p.getByLabel(/What interests you most/).selectOption("salary_budget");
await p.getByLabel(/product news/).check();
await p.getByRole("button", { name: /Join early access/ }).click();
await p.getByText("Check your inbox.").waitFor();
console.log("success ->", await p.getByText("Check your inbox.").innerText());
// 3. email in dev outbox -> verify
const outbox = await (await fetch(`${base}/api/dev/outbox`)).json();
const mail = outbox.find((m) => m.to === email);
console.log("mail sent:", !!mail, "|", mail?.subject);
const link = /(http\S+verify\?token=[\w-]+)/.exec(mail.text)[1];
const r = await fetch(link.replace(/^https?:\/\/[^/]+/, base), { redirect: "manual" });
console.log("verify ->", r.status, r.headers.get("location"));
await p.goto(base + new URL(r.headers.get("location")).pathname + new URL(r.headers.get("location")).search);
console.log("confirmed page:", await p.getByRole("heading", { level: 1 }).innerText());
// 4. duplicate
await p.goto(base, { waitUntil: "networkidle" });
await p.locator("#early-access").scrollIntoViewIfNeeded();
await p.getByLabel(/Email address/).fill(email.toUpperCase());
await p.getByRole("button", { name: /Join early access/ }).click();
await p.getByText("already on the list").waitFor();
console.log("duplicate ->", await p.getByText("already on the list").innerText());
// 5. server error state (simulate offline)
await p.goto(base, { waitUntil: "networkidle" });
await p.route("**/api/waitlist", (route) => route.abort());
await p.locator("#early-access").scrollIntoViewIfNeeded();
await p.getByLabel(/Email address/).fill("offline@example.com");
await p.getByRole("button", { name: /Join early access/ }).click();
await p.getByText(/couldn’t reach the server/).waitFor();
console.log("network error ->", await p.getByText(/couldn’t reach the server/).innerText());
console.log("console errors:", errs.filter((e) => !/Failed to load resource|ERR_FAILED/.test(e)));
await b.close();
