import path from "node:path";
import type { NextConfig } from "next";

const securityHeaders = [
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "X-Frame-Options", value: "DENY" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
];

const nextConfig: NextConfig = {
  poweredByHeader: false,
  ...(process.env.TINWARD_STATIC_EXPORT === "1" ? {
    output: "export" as const,
    trailingSlash: true,
    images: { unoptimized: true },
    experimental: { cpus: 1 },
  } : {}),
  outputFileTracingRoot: path.resolve(import.meta.dirname, "../.."),
  turbopack: { root: path.resolve(import.meta.dirname, "../..") },
  ...(process.env.TINWARD_STATIC_EXPORT === "1" ? {} : {
    async headers() {
      return [{ source: "/:path*", headers: securityHeaders }];
    },
  }),
};

export default nextConfig;
