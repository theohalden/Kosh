import { cp, mkdir, readFile, rm, stat } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { spawn } from "node:child_process";

// Build an isolated copy: never delete backend routes from the real application.
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const source = path.join(root, "apps/web");
const staging = path.join(root, ".static-site/web");
const excluded = ["src/app/api", "src/app/confirmed", "src/app/delete", "src/app/unsubscribe"];
if (!process.env.NEXT_PUBLIC_SITE_URL) excluded.push("src/app/opengraph-image.tsx");
await rm(staging, { recursive: true, force: true });
await mkdir(staging, { recursive: true });
for (const name of ["src", "public", "package.json", "tsconfig.json", "next.config.ts", "postcss.config.mjs"]) {
  if (name === "public" && !(await stat(path.join(source, name)).then(() => true, () => false))) {
    await mkdir(path.join(staging, name));
    continue;
  }
  await cp(path.join(source, name), path.join(staging, name), {
    recursive: true,
    filter: (file) => {
      const relative = path.relative(source, file).split(path.sep).join("/");
      return !excluded.some((entry) => relative === entry || relative.startsWith(`${entry}/`));
    },
  });
}
const child = spawn(process.execPath, [path.join(root, "node_modules/next/dist/bin/next"), "build", staging], {
  cwd: root,
  stdio: "inherit",
  env: { ...process.env, TINWARD_STATIC_EXPORT: "1", NEXT_TELEMETRY_DISABLED: "1" },
});
const code = await new Promise((resolve, reject) => { child.on("error", reject); child.on("exit", resolve); });
if (code !== 0) process.exit(code ?? 1);
await stat(path.join(staging, "out/index.html"));
const home = await readFile(path.join(staging, "out/index.html"), "utf8");
const privacy = await readFile(path.join(staging, "out/privacy/index.html"), "utf8");
if (!home.includes("Email signup is temporarily unavailable") || /<form[\s>]/.test(home + privacy)) {
  throw new Error("Static preview must not expose signup or deletion forms");
}
for (const route of ["api", "confirmed", "delete", "unsubscribe"]) {
  if (await stat(path.join(staging, "out", route)).then(() => true, () => false)) {
    throw new Error(`Unexpected backend route in static export: ${route}`);
  }
}
if (!process.env.NEXT_PUBLIC_SITE_URL && /https?:\/\/localhost/.test(home)) {
  throw new Error("Static preview must not publish localhost metadata");
}
console.log(`Static marketing artifacts: ${path.join(staging, "out")}`);
