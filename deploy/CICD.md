# Website CI/CD

Scope: **the website only** (`tinward.app`, the `tinward-web` container). The Android app has its own workflow
(`android.yml`) and is never deployed by this pipeline. Caddy, the Cloudflare tunnel and other sites on the VM are
never touched.

```
merge to main ──► GitHub Actions (deploy.yml)
                   1. verify      typecheck, lint, tests
                   2. image       build, smoke-test the exact image, publish to GHCR  (main only)
                   3. confirm     wait until https://tinward.app/version.json shows this commit
                                                   │ publishes ghcr.io/theohalden/tinward-web:main (+ :sha-<12>)
VM (web-prod01) ◄── tinward-update.timer, every minute, outbound only
                   pull :main → new digest? → config test → switch container → health gate → done
                                                                     └─ unhealthy → automatic rollback, digest marked bad
```

Pull requests run steps 1 and 2 (without publishing), so a broken build or a failing smoke test is caught before merge.

## Why pull, not push
The VM is on a private network and the repository is public. Nothing in GitHub can log in to the VM, no SSH key or
token is stored in GitHub, and no self-hosted runner executes pull-request code on the VM. The VM only makes
outbound requests to GHCR and only ever deploys `ghcr.io/theohalden/tinward-web`, referenced by immutable digest.
Only a workflow running on `main` can publish the `:main` tag.

## One-time setup
1. **Package visibility.** After the first successful run on `main`, open
   <https://github.com/users/theohalden/packages/container/tinward-web/settings> and set visibility to **Public**
   (the site itself is public and the image holds no secrets). GitHub does not allow this to be set from a workflow.
2. **Install the updater on the VM** (as root, from a copy of `deploy/ci/`): `sudo ./install-updater.sh`
3. Optional: in the repository's *Settings → Environments → production*, add a required reviewer if you want a
   manual approval before `confirm-live` marks a deployment.

## Operating
```sh
sudo /opt/web-hosting/updater/tinward-update --status   # running image, last check/deploy/error, live commit
journalctl -u tinward-update -n 50                      # history
curl -s https://tinward.app/version.json                # which commit is live
sudo systemctl start tinward-update.service             # check right now instead of waiting a minute
```

**Manual rollback:** `sudo docker images ghcr.io/theohalden/tinward-web` shows the last 3 images. Put the older
digest in `/opt/web-hosting/sites/tinward/.env` as `TINWARD_IMAGE=...` and run
`sudo docker compose --project-directory /opt/web-hosting/sites/tinward up -d --no-deps --wait web`. (Revert the
commit on `main` for a permanent fix.)

## Failure behaviour
| Situation | Result |
|---|---|
| Tests fail / image smoke test fails | nothing is published; the VM keeps serving the current version |
| New image fails its nginx config test | not deployed; marked bad |
| New container unhealthy after switch | previous release restored automatically (about 1 minute worst case); digest marked bad so it isn't retried; unit shows failed; `confirm-live` job fails with a pointer to the logs |
| GHCR unreachable / package still private | recorded in `--status`; retried next minute; site keeps running |
| Two updates at once | prevented by a lock |

## Changing the deployment itself
Edits to `deploy/sites/tinward/compose.yaml` or `deploy/ci/*` are not applied by the pipeline (it must not rewrite the
VM's own deployment machinery). After merging such a change, re-run `sudo ./install-updater.sh` on the VM.
