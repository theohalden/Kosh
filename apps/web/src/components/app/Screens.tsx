import type { CSSProperties } from "react";
import { Icon } from "./Icon";
import { NavBar, PhoneBody, type Tab } from "./AppPhone";
import { activity, leftToSpend, month, netWorth, plan, holdings, snapshots, sum, upcomingTotal, type Activity } from "@/content/sample";
import { money } from "@/lib/format";

const amount = (n: number) => money(n);
const GOLD = "#d5a64f", GREEN = "#b7d5ae", TEXT = "#f7f5f0";

function Bar({ title }: { title?: string }) {
  return (
    <div className="ui-bar" style={{ padding: "0 5.6cqw" }}>
      {title ? <span className="ui-title">{title}</span> : <span className="ui-brand"><i />Tinward</span>}
      <span className="ui-month">{month.label.slice(0, 3)} 2026<Icon name="chevron" /></span>
    </div>
  );
}

export function ActivityRow({ a }: { a: Activity }) {
  const cls = a.kind === "in" ? "in" : a.kind === "transfer" ? "tr" : "";
  return (
    <div className="ui-row">
      <span className={`ui-tile ${cls}`}><Icon name={a.icon} /></span>
      <div className="ui-main"><strong>{a.name}</strong><span>{a.meta}</span></div>
      <span className={`ui-amt ${a.kind === "in" ? "ui-pos" : a.kind === "transfer" ? "ui-dim" : ""}`}>{a.kind === "in" ? "+" : a.kind === "out" ? "−" : ""}{amount(a.amount)}</span>
    </div>
  );
}

/* ───────── A · Home ───────── */
export function HomeBody({ k }: { k?: number }) {
  const style = k === undefined ? undefined : ({ "--k": k } as CSSProperties);
  return (
    <PhoneBody>
      <Bar />
      <div style={{ marginTop: "2.4cqw" }}>
        <div className="ui-eyebrow">Left to spend this month</div>
        <div className="ui-num xl ui-pos" style={{ marginTop: "1.6cqw" }}>{amount(leftToSpend)}</div>
        <div className="ui-cap" style={{ marginTop: "1.6cqw" }}>about {amount(Math.round(leftToSpend / month.daysLeft / 10) * 10)} a day · {month.daysLeft} days to go</div>
      </div>
      <div className="ui-seg" style={style} role="img" aria-label="Income split into spent, upcoming and left">
        <i style={{ flex: month.spent, background: TEXT }} />
        <i style={{ flex: upcomingTotal, background: GOLD }} />
        <i style={{ flex: leftToSpend, background: GREEN }} />
      </div>
      <div className="ui-legend">
        <div style={{ "--c": TEXT } as CSSProperties}><small>Spent</small><b>{amount(month.spent)}</b></div>
        <div style={{ "--c": GOLD } as CSSProperties}><small>Coming up</small><b>{amount(upcomingTotal)}</b></div>
        <div style={{ "--c": GREEN } as CSSProperties}><small>Left</small><b>{amount(leftToSpend)}</b></div>
      </div>

      <div className="ui-sec"><h4>Coming up</h4><span>See all</span></div>
      {month.upcoming.slice(0, 3).map((u) => {
        const [d, m] = u.date.split(" ");
        return (
          <div className="ui-row" key={u.name}>
            <span className="ui-tile date"><b>{d}</b><small>{m.toUpperCase()}</small></span>
            <div className="ui-main"><strong>{u.name}</strong><span>Due {u.date}</span></div>
            <span className="ui-amt">{amount(u.amount)}</span>
          </div>
        );
      })}

      <div className="ui-sec"><h4>Accounts</h4></div>
      {month.accounts.map((a) => (
        <div className="ui-row" key={a.mask}>
          <span className="ui-tile"><Icon name="loan" /></span>
          <div className="ui-main"><strong>{a.name} ••{a.mask}</strong><span>As of the last bank message</span></div>
          <span className="ui-amt">{amount(a.balance)}</span>
        </div>
      ))}
      <div className="ui-sec"><h4>Recent</h4><span>See all</span></div>
      {activity[0].items.map((x) => <ActivityRow key={x.id} a={x} />)}
    </PhoneBody>
  );
}
export const HomeScreen = () => (<><HomeBody /><NavBar active="home" /></>);

/* ───────── B · Activity ───────── */
export function ActivityBody({ days = 4 }: { days?: number }) {
  return (
    <PhoneBody>
      <Bar title="Activity" />
      <div className="ui-search"><Icon name="search" />Search merchant or amount</div>
      <div className="ui-chips">
        <span className="ui-chip on">All</span><span className="ui-chip">Spent</span><span className="ui-chip">Received</span><span className="ui-chip">Transfers</span>
        <span className="f"><Icon name="filter" /></span>
      </div>
      {activity.slice(0, days).map((g) => (
        <div key={g.day}>
          <div className="ui-day"><span>{g.day}</span></div>
          {g.items.map((a) => <ActivityRow key={a.id} a={a} />)}
        </div>
      ))}
    </PhoneBody>
  );
}
export const ActivityScreen = () => (<><ActivityBody /><NavBar active="activity" /></>);

/* ───────── C · Plan ───────── */
export function PlanBody({ k }: { k?: number }) {
  const style = k === undefined ? undefined : ({ "--k": k } as CSSProperties);
  const planned = sum(plan.map((p) => ({ value: p.plan })));
  return (
    <PhoneBody>
      <Bar title="Plan" />
      <div className="ui-eyebrow">Income · received 1 Nov</div>
      <div className="ui-num lg" style={{ marginTop: "1.2cqw" }}>{amount(month.income)}</div>
      <div className="ui-seg" style={{ ...style, marginTop: "3.6cqw" }} role="img" aria-label="Income allocation">
        {plan.map((p) => <i key={p.label} style={{ flex: p.plan, background: p.color }} />)}
        <i style={{ flex: month.income - planned, background: "transparent", boxShadow: "inset 0 0 0 0.35cqw #3a3d3f" }} />
      </div>
      <div className="ui-callout">
        <span>Left to spend<br /><span style={{ fontSize: "2.5cqw" }}>after bills, loans &amp; savings</span></span>
        <b>{amount(leftToSpend)}</b>
      </div>
      <div className="ui-sec" style={{ marginBottom: "0.4cqw" }}><h4>This month</h4><span>Edit</span></div>
      {plan.map((p) => (
        <div className="ui-plan-row" key={p.label} style={style}>
          <div className="top"><b>{p.label}</b><span>{amount(p.spent)} of {amount(p.plan)}</span></div>
          <div className="ui-prog" style={{ "--c": p.color, "--k": p.spent / p.plan } as CSSProperties}><i /></div>
        </div>
      ))}
    </PhoneBody>
  );
}
export const PlanScreen = () => (<><PlanBody /><NavBar active="plan" /></>);

/* ───────── D · Wealth ───────── */
export function WealthBody({ k }: { k?: number }) {
  const style = k === undefined ? undefined : ({ "--k": k } as CSSProperties);
  const A = sum(netWorth.assets), L = sum(netWorth.liabilities);
  const W = 100, H = 30;
  const lo = Math.min(...snapshots.map((s) => s.v)) * 0.985, hi = Math.max(...snapshots.map((s) => s.v)) * 1.005;
  const pts = snapshots.map((s, i) => [4 + (i * (W - 8)) / (snapshots.length - 1), H - ((s.v - lo) / (hi - lo)) * (H - 4) - 2] as const);
  const d = pts.map(([x, y], i) => `${i ? "L" : "M"}${x.toFixed(1)} ${y.toFixed(1)}`).join(" ");
  const last = pts[pts.length - 1];
  return (
    <PhoneBody>
      <Bar title="Wealth" />
      <div className="ui-eyebrow">Net worth</div>
      <div className="ui-num xl" style={{ marginTop: "1.4cqw" }}>{amount(A - L)}</div>
      <div className="ui-cap" style={{ marginTop: "1.6cqw" }}>{amount(A)} assets − {amount(L)} owed</div>
      <svg className="ui-chart" viewBox={`0 0 ${W} ${H + 7}`} style={style} aria-hidden="true">
        <path className="ln" pathLength={1} d={d} />
        {pts.slice(0, -1).map(([x, y], i) => <circle key={i} cx={x} cy={y} r="0.9" fill="#6a6c69" style={{ opacity: "var(--k, 1)" }} />)}
        <circle className="pt" cx={last[0]} cy={last[1]} r="1.8" />
        {snapshots.map((s, i) => <text key={s.m} x={pts[i][0]} y={H + 6} textAnchor="middle">{s.m}</text>)}
      </svg>

      <div className="ui-sec"><h4>What you own</h4><span>Update</span></div>
      {holdings.slice(0, 4).map((h) => (
        <div className="ui-row" key={h.id}>
          <div className="ui-main"><strong>{h.label}</strong><span>{h.updated}</span></div>
          <span className="ui-amt">{amount(h.value)}</span>
        </div>
      ))}
      <div className="ui-sec"><h4>What you owe</h4></div>
      {netWorth.liabilities.map((l) => (
        <div className="ui-row" key={l.label}>
          <div className="ui-main"><strong>{l.label}</strong><span>Entered by you</span></div>
          <span className="ui-amt" style={{ color: "#f1a292" }}>−{amount(l.value)}</span>
        </div>
      ))}
    </PhoneBody>
  );
}
export const WealthScreen = () => (<><WealthBody /><NavBar active="wealth" /></>);

export const SCREENS: Record<Tab, () => React.JSX.Element> = { home: HomeScreen, activity: ActivityScreen, plan: PlanScreen, wealth: WealthScreen };
