import assert from "node:assert/strict";
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import os from "node:os";
import { chromium } from "playwright";

const origin = new URL(process.argv[2]).origin;
const output = process.argv[3] ?? path.join(os.tmpdir(), "tinward-browser-check");
await mkdir(output, { recursive: true });
const browser = await chromium.launch({
  headless: true,
  ...(process.env.TINWARD_BROWSER_EXECUTABLE ? { executablePath: process.env.TINWARD_BROWSER_EXECUTABLE } : {}),
});
const report = [];
try {
  for (const [width, height] of [[1440, 1000], [768, 1024], [390, 844]]) {
    const context = await browser.newContext({ viewport: { width, height }, reducedMotion: "reduce" });
    const page = await context.newPage();
    const errors = [], failedAssets = [];
    page.on("pageerror", (error) => errors.push(error.message));
    page.on("response", (response) => { if (response.status() >= 400) failedAssets.push(`${response.status()} ${response.url()}`); });
    const response = await page.goto(origin, { waitUntil: "networkidle" });
    assert.equal(response.status(), 200);
    assert.equal(response.headers()["x-content-type-options"], "nosniff");
    await page.evaluate(() => document.fonts.ready);
    assert.equal(await page.locator("h1").textContent(), "All your money.In perspective.");
    assert.equal(await page.locator("form").count(), 0);
    assert.equal(await page.locator("#early-access").getByText(/Email signup is temporarily unavailable/).count(), 1);
    assert.match(await page.getByRole("link", { name: "Download Android APK", exact: true }).getAttribute("href"), /github\.com\/theohalden\/Tinward\/releases\/download\//);
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `Horizontal overflow at ${width}`);
    await page.screenshot({ path: path.join(output, `home-${width}.png`) });
    if (width === 390) {
      await page.getByRole("button", { name: "Open menu", exact: true }).click();
      await page.locator("#mobile-menu").getByRole("link", { name: "The app" }).click();
      assert.equal(await page.getByRole("button", { name: "Open menu", exact: true }).getAttribute("aria-expanded"), "false");
    }
    const activity = page.getByRole("tab", { name: "Activity", exact: true });
    await activity.click();
    assert.equal(await activity.getAttribute("aria-selected"), "true");
    await activity.press("ArrowRight");
    assert.equal(await page.getByRole("tab", { name: "Plan", exact: true }).getAttribute("aria-selected"), "true");
    await page.locator("#product").scrollIntoViewIfNeeded();
    await page.screenshot({ path: path.join(output, `product-${width}.png`) });
    await page.getByRole("link", { name: "Privacy policy", exact: true }).first().click();
    await page.waitForURL(/\/privacy\/?$/);
    await page.reload({ waitUntil: "networkidle" });
    assert.equal(await page.getByRole("heading", { name: "This website preview", exact: true }).count(), 1);
    assert.equal(await page.locator("form").count(), 0);
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
    assert.deepEqual(errors, []);
    assert.deepEqual(failedAssets, []);
    report.push({ viewport: `${width}x${height}`, pageErrors: errors, failedAssets, overflow: false, navigation: "passed", productTabs: "passed" });
    await context.close();
  }
  const context = await browser.newContext();
  for (const [route, status] of [["/terms/", 200], ["/healthz", 200], ["/not-a-real-page", 404], ["/api/admin/export", 404], ["/api/dev/outbox", 404]]) {
    assert.equal((await context.request.get(origin + route)).status(), status, route);
  }
  const api = await context.request.post(origin + "/api/waitlist", { data: { email: "preview-test@example.invalid" } });
  assert.equal(api.status(), 503);
  assert.equal((await api.json()).error, "unavailable");
  await context.close();
  await writeFile(path.join(output, "results.json"), JSON.stringify({ origin, checkedAt: new Date().toISOString(), report }, null, 2));
  console.log(JSON.stringify({ result: "passed", origin, report, output }, null, 2));
} finally {
  await browser.close();
}
