import type { Metadata, Viewport } from "next";
import { Instrument_Serif, Manrope } from "next/font/google";
import "./globals.css";

const instrument = Instrument_Serif({ subsets: ["latin"], weight: "400", style: ["normal", "italic"], variable: "--font-instrument", display: "swap" });
const manrope = Manrope({ subsets: ["latin"], variable: "--font-manrope", display: "swap" });

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3001";

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: { default: "Tinward — Your money. In perspective.", template: "%s · Tinward" },
  description: "A personal finance app in development: spending, income, budgets and a wider view of what you own and owe. Join Tinward early access.",
  openGraph: { type: "website", siteName: "Tinward", title: "Tinward — Your money. In perspective.", description: "Spending, income and your wider financial picture. Android app in development; early access is free.", locale: "en_US" },
  twitter: { card: "summary_large_image", title: "Tinward — Your money. In perspective.", description: "Your spending, income and wider financial picture. Android app in development." },
  alternates: { canonical: "/" },
};

export const viewport: Viewport = { themeColor: "#f7f5f0", width: "device-width", initialScale: 1 };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning className={`${instrument.variable} ${manrope.variable}`}>
      <head><script dangerouslySetInnerHTML={{__html: "try{if(localStorage.getItem('tinward-motion')==='reduced')document.documentElement.dataset.motion='reduced'}catch(e){}"}} /></head>
      <body>{children}</body>
    </html>
  );
}
