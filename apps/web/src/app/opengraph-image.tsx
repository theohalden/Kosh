import { ImageResponse } from "next/og";

export const alt = "Tinward — Your money. In perspective.";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default function OgImage() {
  return new ImageResponse(
    (
      <div style={{ width: "100%", height: "100%", display: "flex", background: "#f5efe3", color: "#12292c", padding: 72, position: "relative" }}>
        <div style={{ display: "flex", flexDirection: "column", justifyContent: "space-between", width: 700 }}>
          <div style={{ display: "flex", alignItems: "center", fontSize: 48, fontWeight: 700, letterSpacing: -2 }}>
            <div style={{ width: 22, height: 56, background: "#12292c", borderRadius: 11, marginRight: 14 }} />
            Tinward
          </div>
          <div style={{ display: "flex", flexDirection: "column", fontSize: 104, lineHeight: 0.98, fontWeight: 700, letterSpacing: -4 }}>
            <span>All your money.</span>
            <span style={{ color: "#b8801f" }}>In perspective.</span>
          </div>
          <div style={{ fontSize: 30, color: "#3d5659" }}>Early Android APK available · Android 8.0+</div>
        </div>
        <div style={{ position: "absolute", right: 70, top: 70, width: 300, height: 490, borderRadius: 48, background: "#0b0d0f", display: "flex", flexDirection: "column", padding: 28, color: "#f7f5f0" }}>
          <div style={{ fontSize: 22, color: "#b9b6b1" }}>Spent in November</div>
          <div style={{ fontSize: 54, fontWeight: 800, letterSpacing: -2 }}>$3,842</div>
          <div style={{ display: "flex", marginTop: 28, height: 22, borderRadius: 12, overflow: "hidden" }}>
            {[["#d5a64f", 57], ["#f1a292", 18], ["#7faedb", 8], ["#c8a7db", 7], ["#6fbfa3", 6], ["#8b8782", 4]].map(([c, w]) => (
              <div key={c as string} style={{ width: `${w}%`, background: c as string }} />
            ))}
          </div>
          <div style={{ marginTop: 32, fontSize: 22, color: "#b9b6b1" }}>Illustrative sample data</div>
        </div>
      </div>
    ),
    size,
  );
}
