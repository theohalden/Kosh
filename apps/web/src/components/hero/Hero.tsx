import "./hero.css";
import { Tilt } from "../Tilt";
import { AppPhone } from "../app/AppPhone";
import { HomeScreen } from "../app/Screens";
import { Money } from "../Money";
import { month, netWorth, sum } from "@/content/sample";
export function Hero() {
  return (
    <section className="hero shell" id="top" aria-labelledby="hero-heading">
      <div className="hero-copy">
        <p className="hero-kicker"><span className="entry-line" aria-hidden="true"/>A more thoughtful way with money</p>
        <h1 id="hero-heading" className="hero-heading">All your money.<br/><em>In perspective.</em></h1>
        <p className="hero-sub">The little things you spend. The money you set aside.<br className="hero-break"/> The plans you’re working towards.</p>
        <p className="hero-description">Tinward brings your financial life into focus, so you can understand where you stand and make more of what you have.</p>
        <div className="hero-actions"><a href="#early-access" className="btn btn-primary">Join the Android beta <span className="cta-arrow" aria-hidden="true">↗</span></a><a href="#product" className="hero-link">Discover Tinward <span aria-hidden="true">↓</span></a></div>
        <p className="hero-note"><span aria-hidden="true"/>Personal finance, made simpler.</p>
      </div>
      <div className="hero-product">
        <span className="hero-orbit orbit-a" aria-hidden="true"/><span className="hero-orbit orbit-b" aria-hidden="true"/>
        <div className="hero-product-caption"><span>01 / THE CLEARER PICTURE</span><span>PERSONAL FINANCE, RECONSIDERED</span></div>
        <div className="hero-phone"><Tilt><AppPhone label="Tinward Home concept with synthetic financial figures"><HomeScreen/></AppPhone></Tilt></div>
        <div className="hero-float float-income" aria-hidden="true"><span className="float-dot"/>Income, accounted for.<strong><Money value={month.income} /><span> this month</span></strong></div>
        <div className="hero-float float-worth" aria-hidden="true"><span>THE BIGGER PICTURE</span><strong><Money value={sum(netWorth.assets) - sum(netWorth.liabilities)} /></strong><small>What you own, less what you owe.</small></div>
        <p className="hero-preview-note">A look at the vision. Explore the app below.</p>
      </div>
    </section>
  );
}
