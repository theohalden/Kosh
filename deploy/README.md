> **Deployment is now automated: see [CICD.md](CICD.md).** The manual SSH/build steps below are the previous process, kept for history; `update.sh` and `activate.sh` were removed.

# Portable static website hosting

These are generic templates. Keep real hostnames, SSH identities, addresses,
firewall findings, credentials and infrastructure verification reports private.
The operator's private handover should record the actual authorized target.

## Audit and build modes

This npm-workspace monorepo uses Next.js 16, React 19, TypeScript and Tailwind 4.
Use Node 24 and npm 11 with the root package-lock.json. The ordinary
`npm run build:web` produces apps/web/.next with server-side early-access,
verification/deletion and admin APIs. It is NOT a static React bundle.

The separately approved marketing-only profile runs `npm run build:static`.
It stages source in .static-site/web, excludes api/confirmed/delete/unsubscribe
routes and exports to .static-site/web/out. Backend source in apps/web is
preserved. Signup and deletion forms display an unavailable notice; no emails
are collected or sent. Supported public routes are /, /privacy/ and /terms/.
The APK remains hosted on GitHub. Fonts are fetched at build time and served
from the export; no runtime font API is needed. Nginx preserves Next RSC files
and asset paths and returns real 404s rather than an SPA catch-all.

Full backend mode needs Supabase, Resend and private token secrets; see
apps/web/.env.example. None is required by this static deployment. No database,
Redis, development server, API secrets or Node runtime is deployed.
Only an optional public NEXT_PUBLIC_SITE_URL build argument exists. Leave it
empty for Quick Tunnels so no ephemeral hostname or localhost metadata is baked
into the image. Absolute social metadata is omitted without a permanent origin.
.dockerignore allowlists build inputs and excludes local env files, Git data,
Android data, signing keys, databases and build outputs.

## Layout

```text
/opt/web-hosting/
├── README.md
├── proxy/
│   ├── compose.yaml
│   └── conf/Caddyfile
└── sites/
    └── tinward/
        ├── compose.yaml
        ├── .env
        ├── .env.previous
        ├── compose.yaml.previous
        └── releases/<commit>/
            ├── deploy/sites/tinward/Dockerfile
            ├── deploy/sites/tinward/nginx.conf
            ├── apps/web/...
            └── deployment.env
```

Caddy and each website are separate Compose projects. web-proxy is a shared
internal-only Docker network; Caddy routes to tinward-web:8080 by service alias.
The app publishes no host ports. Caddy publishes ONLY 127.0.0.1:8081.
Caddy and the optional tunnel also share a separate IPv4-only ingress/egress
bridge: Docker 29 does not activate host port mappings on an internal-only
bridge. The tunnel is not attached directly to the site network.

## First deployment

Review configuration and obtain access to the authorized hosting VM first.
The operator needs Docker Compose and an approved sudo path. Preserve existing
sites and containers; choose an unused loopback port. Do not modify the
hypervisor, other VMs or host firewall as part of an application deployment.

On the authorized VM, create the new directories owned by your deploy user:

```sh
sudo install -d -o YOUR_DEPLOY_USER -g YOUR_DEPLOY_GROUP /opt/web-hosting
# Run remaining directory creation as that deploy user.
mkdir -p /opt/web-hosting/proxy/conf /opt/web-hosting/sites/tinward/releases
sudo docker network create --internal --driver bridge web-proxy
```

From a clean committed workstation checkout:

```sh
npm ci
npm run check:web
npm run build:static
bash deploy/update.sh YOUR_DEPLOY_USER@YOUR_HOST
```

update.sh transfers only git-tracked source, builds before changing the running
site, validates Nginx, and waits for healthy status. It does not restart Caddy
or unrelated websites. The script refuses to overwrite existing release
directories; inspect and resume a previously transferred release with:

```sh
bash /opt/web-hosting/sites/tinward/releases/COMMIT/deploy/activate.sh /opt/web-hosting/sites/tinward/releases/COMMIT COMMIT
```

Install the reviewed proxy files from the transferred release on the VM:

```sh
cp /opt/web-hosting/sites/tinward/releases/COMMIT/deploy/proxy/compose.yaml /opt/web-hosting/proxy/
cp /opt/web-hosting/sites/tinward/releases/COMMIT/deploy/proxy/conf/Caddyfile /opt/web-hosting/proxy/conf/
cd /opt/web-hosting/proxy
sudo docker compose run --rm --no-deps caddy caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
sudo docker compose up -d --wait caddy
curl --fail http://127.0.0.1:8081/healthz
```

All base images are pinned by digest. Review upstream security updates and
refresh these deliberately. The builder limits Node's heap to 768 MiB and uses
one Next worker; build one site at a time on small VMs. Check memory and disk
before building. No swap or persistent extra services are installed.

## Temporary public HTTPS

```sh
cd /opt/web-hosting/proxy
sudo docker compose --profile tunnel up -d quick-tunnel
sudo docker compose logs --tail=80 quick-tunnel
```

Use the NEW https://*.trycloudflare.com URL printed in logs, never a previous
URL. Do not put it in application configuration. The tunnel uses outbound
HTTP/2 and has no token or credential. restart=no makes it deliberately
temporary across VM boots. It has no healthcheck executable; check logs AND
the public endpoint. End only this deployment's public preview with:

```sh
sudo docker compose --profile tunnel stop quick-tunnel
```

Do not stop unrelated existing tunnel containers. Quick Tunnels provide no
SLA or access control; anyone with the URL can view the preview.

## Logs and checks

```sh
sudo docker compose -f /opt/web-hosting/sites/tinward/compose.yaml ps
sudo docker compose -f /opt/web-hosting/sites/tinward/compose.yaml logs --tail=100 web
sudo docker compose -f /opt/web-hosting/proxy/compose.yaml logs --tail=100 caddy quick-tunnel
curl --fail http://127.0.0.1:8081/proxy-healthz
curl --fail http://127.0.0.1:8081/healthz
curl --fail http://127.0.0.1:8081/privacy/
curl --fail http://127.0.0.1:8081/terms/
curl -i -X POST http://127.0.0.1:8081/api/waitlist  # expected 503
sudo ss -lntup
sudo docker stats --no-stream
```

Nginx supplies the security headers because Next headers do not apply to
static exports. Access logging is disabled to avoid retaining IP addresses or
token query strings; error/container logs are bounded and rotated.
Nginx and Caddy healthchecks run every 30 seconds. Restart policies restart
exited processes, not merely unhealthy containers. Investigate unhealthy status.
Test process recovery only for this new site, never by rebooting a shared VM.

Browser smoke check, from a checkout with npm dependencies installed:

```sh
node deploy/smoke.mjs https://CURRENT-PREVIEW.trycloudflare.com
```

It checks desktop/tablet/mobile layouts, asset loads, JS errors, overflow,
mobile navigation, keyboard-operated tabs, deep-link reloads, disabled forms,
API responses and 404s. Screenshots/results go to the OS temporary directory.
If needed, deliberately install Chromium with `npx playwright install chromium`
or set TINWARD_BROWSER_EXECUTABLE to an existing compatible binary.

## Updates and rollback

Commit new source and run update.sh. The prior Compose file and image
configuration are restored if the new deployment fails its health gate.
No global compose down or image prune is used. Images and source snapshots
are retained: review disk usage and remove old releases only after approval.
Manual rollback after at least two deployments:

```sh
cd /opt/web-hosting/sites/tinward
cp compose.yaml.previous compose.yaml
cp .env.previous .env
sudo docker compose up -d --no-deps --wait --wait-timeout 60 web
curl --fail http://127.0.0.1:8081/healthz
```

Alternatively point TINWARD_IMAGE/TINWARD_SOURCE in .env at a retained release.
Never reuse an image tag for different source. Proxy changes are independent;
back up its Caddyfile, validate and hot reload rather than restarting sites:

```sh
cd /opt/web-hosting/proxy
sudo docker compose exec caddy caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
sudo docker compose exec caddy caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile
```

## Security boundaries

Containers are non-root, read-only and unprivileged with all capabilities
dropped and no-new-privileges enabled. Only small tmpfs mounts are writable.
PID, CPU, memory and log limits are set; no Docker socket or credentials are
mounted. No database, SSH, Docker API or Caddy admin port is published.

Internal-only networking removes the site's external default route. It is
not an absolute sandbox: host bridge endpoints and shared-network peers remain
trust boundaries. Shared sites must be mutually trusted. Prefer separate
per-site proxy networks for untrusted workloads. Caddy and the tunnel have an
egress bridge and need separate VM/upstream isolation from sensitive networks.
Host firewall, IPv6, management reachability and SSH exposure must be audited
independently. Keep actual findings and network topology in a private handover;
these templates do not establish a secure VLAN or verify WAN firewall policy.
Docker administration/sudo is root-equivalent. Quick Tunnels are temporary;
this is a single-VM setup with no high availability.

## Adding a second website

Create sites/karpo or sites/portfolio with its own Compose name, release image,
healthcheck, restart policy and hardening. Join web-proxy with a unique alias,
e.g. karpo-web, with no published app ports. Use multi-stage Nginx for static
artifacts or a production Node container for apps that truly require a server.
Add a host-specific Caddy block beside the existing :8080 fallback:

```caddyfile
http://karpo.local:8080 {
    reverse_proxy karpo-web:8080
}
```

Validate/hot reload Caddy and test with
`curl -H 'Host: karpo.local' http://127.0.0.1:8081/`.
Use an SSH forward/local DNS or separately reviewed public tunnel/domain route.
Deploying the second project must not restart the first. Do not strip path
prefixes blindly; that can break application assets and routing.

## Permanent domain migration

As a separate reviewed change, choose a domain you control and configure a
named Cloudflare Tunnel with explicit hostname routing and a final 404 fallback.
Store its credential in a restricted secret mount, not Git or build arguments.
A permanent restart policy needs approval. Keep Caddy internal/loopback-only;
Cloudflare terminates public TLS. Direct Caddy HTTPS needs a separate DNS,
router forwarding and firewall review.
Set NEXT_PUBLIC_SITE_URL to the permanent HTTPS origin and rebuild to enable
canonical/social metadata. Align Caddy hostname routing. Before enabling the
full backend, configure Supabase/Resend server secrets, apply migrations, add
narrowly scoped egress and review forwarded-header trust and API cache rules.
The static-only container intentionally cannot serve those APIs.
