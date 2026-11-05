import { chromium } from "playwright";
const [prefix, w, h] = process.argv.slice(2);
const b = await chromium.launch({ channel: "chrome" });
const p = await (await b.newContext({ viewport: { width: +w, height: +h }, isMobile: +w < 800, hasTouch: +w < 800 })).newPage();
p.on("console", (m) => { if (m.type() === "error") console.log("console error:", m.text().slice(0, 300)); });
p.on("pageerror", (e) => console.log("pageerror:", e.message));
await p.goto("http://localhost:3001", { waitUntil: "networkidle" });
await p.waitForTimeout(1200);
const box = await p.evaluate(() => { const e = document.querySelector("#product"); const r = e.getBoundingClientRect(); return { top: r.top + scrollY, height: r.height }; });
const span = box.height - +h;
for (let i = 0; i < 4; i++) {
  await p.evaluate((y) => scrollTo(0, y), box.top + ((i + 0.5) / 4) * span);
  await p.waitForTimeout(2300);
  await p.screenshot({ path: `shots/${prefix}-sc${i}.png` });
}
console.log("done");
await b.close();
