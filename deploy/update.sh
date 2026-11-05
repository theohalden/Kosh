#!/bin/sh
# Run on your workstation from a clean, committed checkout. No secrets are transferred.
set -eu
host=${1:?Usage: bash deploy/update.sh SSH_USER@HOST}
root=$(git rev-parse --show-toplevel)
cd "$root"
if [ -n "$(git status --porcelain)" ]; then
  echo 'Commit or stash changes before deployment.' >&2
  exit 1
fi
release=$(git rev-parse --short=12 HEAD)
remote=/opt/web-hosting/sites/tinward/releases/$release
ssh "$host" "test ! -e '$remote' && mkdir '$remote'"
git archive HEAD | ssh "$host" "tar --no-same-owner -xf - -C '$remote'"
ssh "$host" "bash '$remote/deploy/activate.sh' '$remote' '$release'"
