import "./hero.css";
import { AppPhone } from "../app/AppPhone";
import { HomeScreen } from "../app/Screens";
import { money } from "@/lib/format";
import { month, netWorth, sum } from "@/content/sample";
export function Hero() {
  return (
    <section className="hero shell" id="top" aria-labelledby="hero-heading">
      <div className="hero-copy">
        <p className="hero-kicker"><span className="entry-line" aria-hidden="true"/>A little clarity. A lot more life.</p>
        <h1 id="hero-heading" className="hero-heading">All your money.<br/><em>In perspective.</em></h1>
        <p className="hero-sub">Less piecing it together.<br className="hero-break"/> More knowing where you stand.</p>
        <p className="hero-description">We’re building one place for your spending, income, investments and goals. A clearer picture, starting with your everyday money.</p>
        <div className="hero-actions"><a href="#early-access" className="btn btn-primary">Join early access <span className="cta-arrow" aria-hidden="true">↗</span></a><a href="#product" className="hero-link">Explore Tinward <span aria-hidden="true">↓</span></a></div>
        <p className="hero-note"><span aria-hidden="true"/>Android in development · Free to join</p>
      </div>
      <div className="hero-product">
        <span className="hero-orbit orbit-a" aria-hidden="true"/><span className="hero-orbit orbit-b" aria-hidden="true"/>
        <div className="hero-product-caption"><span>01 / THE CLEARER PICTURE</span><span>PERSONAL FINANCE, RECONSIDERED</span></div>
        <div className="hero-phone"><AppPhone label="Tinward Home concept with synthetic financial figures"><HomeScreen/></AppPhone></div>
        <div className="hero-float float-income" aria-hidden="true"><span className="float-dot"/>Income, accounted for.<strong>{money(month.income)}<span> this month</span></strong></div>
        <div className="hero-float float-worth" aria-hidden="true"><span>THE BIGGER PICTURE</span><strong>{money(sum(netWorth.assets) - sum(netWorth.liabilities))}</strong><small>What you own, less what you owe.</small></div>
        <p className="hero-preview-note">A look at the vision. Explore the app below.</p>
      </div>
    </section>
  );
}
