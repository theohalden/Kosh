import { Nav } from "@/components/Nav";
import { Footer } from "@/components/Footer";
import { Hero } from "@/components/hero/Hero";
import { Problem } from "@/components/sections/Problem";
import { Showcase } from "@/components/sections/Showcase";
import { SmsTracking } from "@/components/sections/SmsTracking";
import { SalaryBudget } from "@/components/sections/SalaryBudget";
import { Wealth } from "@/components/sections/Wealth";
import { NetWorth } from "@/components/sections/NetWorth";
import { HowItWorks } from "@/components/sections/HowItWorks";
import { Privacy } from "@/components/sections/Privacy";
import { Pricing } from "@/components/sections/Pricing";
import { EarlyAccess } from "@/components/sections/EarlyAccess";
import { Faq } from "@/components/sections/Faq";
import { FAQS, REPO_URL } from "@/content/site";

const jsonLd = {
  "@context": "https://schema.org",
  "@graph": [
    { "@type": "SoftwareApplication", name: "Tinward", applicationCategory: "FinanceApplication", operatingSystem: "Android", description: "Personal finance app for Android, in early development. Transaction tracking today, a wider financial picture in development.", sameAs: [REPO_URL] },
    { "@type": "FAQPage", mainEntity: FAQS.map((f) => ({ "@type": "Question", name: f.q, acceptedAnswer: { "@type": "Answer", text: f.a } })) },
  ],
};

export default function Home() {
  return (
    <>
      <Nav />
      <main id="main" tabIndex={-1} className="focus:outline-none">
        <Hero />
        <Problem />
        <Showcase />
        <SmsTracking />
        <SalaryBudget />
        <Wealth />
        <NetWorth />
        <HowItWorks />
        <Privacy />
        <Pricing />
        <EarlyAccess />
        <Faq />
      </main>
      <Footer />
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd).replace(/</g, "\\u003c") }} />
    </>
  );
}
