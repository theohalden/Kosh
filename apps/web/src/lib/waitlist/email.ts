import { Resend } from "resend";

export type Mail = { to: string; subject: string; html: string; text: string; headers?: Record<string, string> };
export interface Mailer { send(mail: Mail): Promise<void> }

const g = globalThis as unknown as { __koshMailer?: Mailer; __koshOutbox?: Mail[] };

/** Development only: records emails in memory and prints them, so the full flow is testable without Resend. */
export const outbox = (): Mail[] => (g.__koshOutbox ??= []);

class DevOutbox implements Mailer {
  async send(mail: Mail) {
    outbox().push(mail);
    console.log(`[dev-outbox] to=${mail.to} subject="${mail.subject}"\n${mail.text}\n`);
  }
}

class ResendMailer implements Mailer {
  private client: Resend;
  constructor(key: string, private from: string) { this.client = new Resend(key); }
  async send(mail: Mail) {
    const { error } = await this.client.emails.send({ from: this.from, to: mail.to, subject: mail.subject, html: mail.html, text: mail.text, headers: mail.headers });
    if (error) throw new Error(`resend: ${error.message}`);
  }
}

export function getMailer(): Mailer {
  if (g.__koshMailer) return g.__koshMailer;
  const key = process.env.RESEND_API_KEY;
  const from = process.env.EMAIL_FROM;
  if (key && from) g.__koshMailer = new ResendMailer(key, from);
  else if (process.env.NODE_ENV !== "production") g.__koshMailer = new DevOutbox();
  else throw new Error("RESEND_API_KEY and EMAIL_FROM are required in production");
  return g.__koshMailer;
}

export function setMailer(m: Mailer | undefined) { g.__koshMailer = m; }

export const siteUrl = () => (process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3001").replace(/\/$/, "");

/* ───────────────────────── templates ───────────────────────── */

const esc = (s: string) => s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]!);

function layout(title: string, bodyHtml: string, footerHtml = "") {
  return `<!doctype html><html><body style="margin:0;background:#f5efe3;padding:32px 16px;font-family:Segoe UI,Arial,sans-serif;color:#12292c">
<table role="presentation" width="100%" style="max-width:520px;margin:0 auto"><tr><td>
<p style="font-family:Georgia,serif;font-size:28px;font-weight:600;margin:0 0 24px;letter-spacing:-0.03em">Tinward</p>
<div style="background:#fbf7ee;border-radius:16px;padding:28px">
<h1 style="font-family:Georgia,serif;font-size:26px;line-height:1.15;margin:0 0 14px">${esc(title)}</h1>${bodyHtml}</div>
<p style="font-size:12px;color:#6b7f80;margin:20px 4px 0;line-height:1.5">${footerHtml}</p>
</td></tr></table></body></html>`;
}
const button = (href: string, label: string) => `<p style="margin:24px 0"><a href="${esc(href)}" style="background:#121412;color:#fff;text-decoration:none;font-weight:800;padding:14px 24px;border-radius:8px;display:inline-block">${esc(label)}</a></p>`;

export function verificationEmail(to: string, link: string): Mail {
  return {
    to,
    subject: "Confirm your email for Tinward early access",
    html: layout("Confirm your email", `<p style="line-height:1.55">Thanks for your interest in Tinward. Please confirm this address so we know it&rsquo;s really yours. Only confirmed addresses are invited.</p>${button(link, "Confirm my email")}<p style="font-size:13px;color:#3d5659">The link works for 48 hours. If you didn&rsquo;t sign up, ignore this email and nothing will happen.</p>`, "Tinward is a personal finance app for Android, currently in early development."),
    text: `Confirm your email for Tinward early access:\n${link}\n\nThe link works for 48 hours. If you didn't sign up, ignore this email.`,
  };
}

export function invitationEmail(to: string, inviteUrl: string, unsubscribeUrl: string): Mail {
  return {
    to,
    subject: "Your Tinward early-access invitation",
    html: layout("You’re invited to the Tinward beta", `<p style="line-height:1.55">The Android beta of Tinward is opening and your spot is ready.</p>${button(inviteUrl, "Get the beta")}<p style="font-size:13px;color:#3d5659">Tinward reads bank messages on your phone to build your history locally, using supported regional formats. SMS access is optional; supported statements can also be imported.</p>`, `You&rsquo;re receiving this because you joined the Tinward early-access list. <a href="${esc(unsubscribeUrl)}" style="color:#6b7f80">Unsubscribe</a>.`),
    text: `The Android beta of Tinward is opening and your spot is ready:\n${inviteUrl}\n\nYou received this because you joined the Tinward early-access list. Unsubscribe: ${unsubscribeUrl}`,
    headers: { "List-Unsubscribe": `<${unsubscribeUrl}>` },
  };
}

export function deletionEmail(to: string, link: string): Mail {
  return {
    to,
    subject: "Confirm deletion of your Tinward early-access data",
    html: layout("Confirm deletion", `<p style="line-height:1.55">We received a request to delete the early-access details stored for this address. Confirm to remove them permanently.</p>${button(link, "Delete my data")}<p style="font-size:13px;color:#3d5659">The link works for 24 hours. If this wasn&rsquo;t you, ignore this email and nothing will be deleted.</p>`),
    text: `Confirm deletion of your Tinward early-access data:\n${link}\n\nThe link works for 24 hours. If this wasn't you, ignore this email.`,
  };
}
