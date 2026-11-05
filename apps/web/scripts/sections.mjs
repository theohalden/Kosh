// Usage: node scripts/sections.mjs <prefix> <width> <height> [url]
import { chromium } from "playwright";
const [prefix, w, h, url = "http://localhost:3001"] = process.argv.slice(2);
const browser = await chromium.launch({ channel: "chrome" });
const ctx = await browser.newContext({ viewport: { width: +w, height: +h }, hasTouch: +w < 800, isMobile: +w < 800 });
const page = await ctx.newPage();
page.on("console", (m) => { if (m.type() === "error") console.log("console error:", m.text().slice(0, 300)); });
page.on("pageerror", (e) => console.log("pageerror:", e.message));
await page.goto(url, { waitUntil: "networkidle" });
await page.waitForTimeout(1500);
const sels = ["section[aria-labelledby=problem-h]", "#product", "#features", "section[aria-labelledby=salary-h]", "section[aria-labelledby=wealth-h]", "section[aria-labelledby=nw-h]", "#how", "#privacy", "#pricing", "#early-access", "#faq", "footer"];
// walk the page so every reveal fires
const total = await page.evaluate(() => document.documentElement.scrollHeight);
for (let y = 0; y < total; y += +h * 0.6) { await page.evaluate((t) => scrollTo(0, t), y); await page.waitForTimeout(120); }
await page.waitForTimeout(1200);
let i = 0;
for (const s of sels) {
  const el = page.locator(s).first();
  await el.scrollIntoViewIfNeeded();
  await el.evaluate((n) => n.scrollIntoView({ block: "start" }));
  await page.waitForTimeout(1300);
  await page.screenshot({ path: `shots/${prefix}-${String(++i).padStart(2, "0")}.png` });
  const box = await el.boundingBox();
  console.log(s, "height", Math.round(box?.height ?? 0));
}
console.log("overflow-x:", await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth));
await browser.close();
