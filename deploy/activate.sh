#!/usr/bin/env bash
# Execute on the authorized hosting VM using a committed release.
set -euo pipefail
source_dir=${1:?Absolute release directory required}
release=${2:?Release identifier required}
[[ "$source_dir" == /opt/web-hosting/sites/tinward/releases/* ]]
[[ "$release" =~ ^[a-zA-Z0-9._-]+$ ]]
site=/opt/web-hosting/sites/tinward
image=tinward-web:$release
next_env=$source_dir/deployment.env
printf 'TINWARD_IMAGE=%s\nTINWARD_SOURCE=%s\nNEXT_PUBLIC_SITE_URL=\n' "$image" "$source_dir" > "$next_env"
# Build before modifying the running project. This does not affect Caddy/other sites.
sudo -n docker compose --env-file "$next_env" -f "$source_dir/deploy/sites/tinward/compose.yaml" build web
sudo -n docker run --rm --read-only --tmpfs /tmp:uid=101,gid=101,size=16m --network none --cap-drop ALL --security-opt no-new-privileges "$image" -t
if [[ -f "$site/.env" ]]; then
  cp "$site/.env" "$site/.env.previous"
  cp "$site/compose.yaml" "$site/compose.yaml.previous"
fi
cp "$source_dir/deploy/sites/tinward/compose.yaml" "$site/compose.yaml"
cp "$next_env" "$site/.env.next"
mv "$site/.env.next" "$site/.env"
if ! sudo -n docker compose --project-directory "$site" -f "$site/compose.yaml" up -d --no-deps --wait --wait-timeout 60 web; then
  if [[ -f "$site/.env.previous" ]]; then
    cp "$site/compose.yaml.previous" "$site/compose.yaml"
    cp "$site/.env.previous" "$site/.env"
    sudo -n docker compose --project-directory "$site" -f "$site/compose.yaml" up -d --no-deps --wait --wait-timeout 60 web
    echo 'New deployment failed; previous release restored.' >&2
  else
    echo 'Initial deployment failed; no previous release is available.' >&2
  fi
  exit 1
fi
printf 'Activated %s\n' "$image"
