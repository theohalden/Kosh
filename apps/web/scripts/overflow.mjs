import { chromium } from "playwright";
const b = await chromium.launch({ channel: "chrome" });
const p = await (await b.newContext({ viewport: { width: +process.argv[2] || 390, height: 844 }, isMobile: true, hasTouch: true })).newPage();
await p.goto("http://localhost:3001", { waitUntil: "networkidle" });
const total = await p.evaluate(() => document.documentElement.scrollHeight);
for (let y = 0; y < total; y += 500) { await p.evaluate((t) => scrollTo(0, t), y); await p.waitForTimeout(80); }
await p.waitForTimeout(1500);
console.log(await p.evaluate(() => { const W = document.documentElement.clientWidth; const out = []; document.querySelectorAll("body *").forEach((e) => { const r = e.getBoundingClientRect(); if (r.right > W + 1 && r.width > 0) out.push(`${e.tagName}.${(e.className?.toString?.() || "").slice(0, 40)} right=${Math.round(r.right)}`); }); return out.slice(0, 12).join("\n") + "\nscrollW=" + document.documentElement.scrollWidth + " W=" + W; }));
await b.close();
