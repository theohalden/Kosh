// Usage: node scripts/shot.mjs <name> <width> <height> <url> <scrollY,scrollY,...> [selector]
import { chromium } from "playwright";
const [name, w, h, url = "http://localhost:3001", ys = "0", sel] = process.argv.slice(2);
const browser = await chromium.launch({ channel: "chrome" }).catch(() => chromium.launch());
const ctx = await browser.newContext({ viewport: { width: +w, height: +h }, deviceScaleFactor: 1, hasTouch: +w < 800, isMobile: +w < 800 });
const page = await ctx.newPage();
page.on("console", (m) => { if (["error", "warning"].includes(m.type())) console.log("console:", m.type(), m.text().slice(0, 200)); });
page.on("pageerror", (e) => console.log("pageerror:", e.message));
await page.goto(url, { waitUntil: "networkidle" });
await page.waitForTimeout(2600);
for (const y of ys.split(",")) {
  const target = y.endsWith("vh") ? (parseFloat(y) / 100) * (await page.evaluate(() => innerHeight)) : +y;
  await page.evaluate((t) => window.scrollTo(0, t), target);
  await page.waitForTimeout(500);
  const out = `shots/${name}-${y}.png`;
  if (sel) await page.locator(sel).first().screenshot({ path: out }); else await page.screenshot({ path: out });
  console.log(out);
}
const ov = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
console.log("horizontal overflow px:", ov);
await browser.close();
