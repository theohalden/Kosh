import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

// Gradle owns the Android build. npm is only a convenient root-level entry point.
const tasks = process.argv.slice(2);
if (!tasks.length) {
  console.error("Pass a Gradle task, for example: node scripts/android.mjs assembleDebug");
  process.exit(1);
}
const windows = process.platform === "win32";
const child = spawn(windows ? "cmd.exe" : "bash", windows
  ? ["/d", "/s", "/c", "gradlew.bat", ...tasks]
  : ["gradlew", ...tasks], {
  cwd: fileURLToPath(new URL("../apps/android/", import.meta.url)),
  stdio: "inherit",
  env: process.env,
});
child.on("error", error => {
  console.error(`Could not start Gradle: ${error.message}`);
  process.exitCode = 1;
});
child.on("exit", code => { process.exitCode = code ?? 1; });
