#!/usr/bin/env bash
# Install the Tinward auto-deploy updater on the website VM.  Run as root from this directory:  sudo ./install-updater.sh
# Idempotent. Touches only the Tinward site's compose file and its own units; Caddy, hello-web and the tunnel are untouched.
set -euo pipefail
[[ $EUID -eq 0 ]] || { echo "run as root (sudo)"; exit 1; }
HERE="$(cd "$(dirname "$0")" && pwd)"
SITE=/opt/web-hosting/sites/tinward

install -d -m 0755 /opt/web-hosting/updater
install -m 0755 "$HERE/tinward-update" /opt/web-hosting/updater/tinward-update

# The compose file no longer builds on the VM; keep the previous one for reference.
if [[ -f "$SITE/compose.yaml" && ! -f "$SITE/compose.yaml.pre-cicd" ]]; then cp -p "$SITE/compose.yaml" "$SITE/compose.yaml.pre-cicd"; fi
install -m 0644 "$HERE/../sites/tinward/compose.yaml" "$SITE/compose.yaml"

install -m 0644 "$HERE/tinward-update.service" /etc/systemd/system/tinward-update.service
install -m 0644 "$HERE/tinward-update.timer"   /etc/systemd/system/tinward-update.timer
systemctl daemon-reload
systemd-analyze verify /etc/systemd/system/tinward-update.service /etc/systemd/system/tinward-update.timer || true
systemctl enable --now tinward-update.timer
echo "installed. Status:  sudo /opt/web-hosting/updater/tinward-update --status     Logs:  journalctl -u tinward-update"
