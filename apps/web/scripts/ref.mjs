import { chromium } from "playwright";
const b = await chromium.launch({ channel: "chrome" });
const p = await (await b.newContext({ viewport: { width: 1440, height: 900 } })).newPage();
await p.goto("https://backdoorai.app/", { waitUntil: "networkidle", timeout: 60000 }).catch(e => console.log("goto", e.message));
await p.waitForTimeout(3500);
const total = await p.evaluate(() => document.documentElement.scrollHeight);
console.log("title:", await p.title(), "height:", total);
let i = 0;
for (let y = 0; y < Math.min(total, 9000); y += 800) {
  await p.evaluate((t) => scrollTo(0, t), y); await p.waitForTimeout(900);
  await p.screenshot({ path: `shots/ref/r${String(i++).padStart(2, "0")}.png` });
}
const info = await p.evaluate(() => {
  const out = { fonts: {}, colors: {}, bg: getComputedStyle(document.body).backgroundColor, headings: [] };
  document.querySelectorAll("h1,h2,h3,p,a,button,nav *").forEach((e) => { const s = getComputedStyle(e); out.fonts[s.fontFamily.split(",")[0]] = (out.fonts[s.fontFamily.split(",")[0]] || 0) + 1; out.colors[s.color] = (out.colors[s.color] || 0) + 1; });
  document.querySelectorAll("h1,h2,h3").forEach((e) => { const s = getComputedStyle(e); out.headings.push([e.tagName, e.textContent.trim().slice(0, 70), s.fontSize, s.fontWeight, s.letterSpacing, s.lineHeight, s.fontFamily.split(",")[0]]); });
  out.nav = [...document.querySelectorAll("nav a, header a")].map(a => a.textContent.trim()).filter(Boolean).slice(0, 12);
  return out;
});
console.log(JSON.stringify(info, null, 1).slice(0, 3500));
await b.close();
