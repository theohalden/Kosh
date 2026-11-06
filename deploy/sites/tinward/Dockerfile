# syntax=docker/dockerfile:1@sha256:4edf897a3ffa55b89f906fc8cc78afdb3f1834cc9c7083565e611a8a7d5fe99e
FROM node:24-alpine@sha256:8a32c8cf649a49915a66e81e7a609223c708d0adabf288ee3f4077d765542f85 AS build
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1 NODE_OPTIONS=--max-old-space-size=768
COPY package.json package-lock.json ./
COPY apps/web/package.json ./apps/web/package.json
RUN npm ci --no-audit --no-fund
COPY apps/web/ ./apps/web/
COPY scripts/build-static.mjs ./scripts/build-static.mjs
# Public origin baked into canonical links and share previews (set by CI to https://tinward.app).
ARG NEXT_PUBLIC_SITE_URL=""
ENV NEXT_PUBLIC_SITE_URL=$NEXT_PUBLIC_SITE_URL
RUN npm run build:static
# Commit marker, so the pipeline (and anyone) can see exactly what is live: GET /version.json
ARG GIT_SHA=unknown
RUN printf '{"sha":"%s"}\n' "$GIT_SHA" > .static-site/web/out/version.json

FROM nginx:stable-alpine@sha256:0e52bd054126cdc040b1276c0af347aeb02a41b9cfe305d6094767ac30e97949 AS runtime
COPY deploy/sites/tinward/nginx.conf /etc/nginx/nginx.conf
COPY --from=build /app/.static-site/web/out/ /usr/share/nginx/html/
USER 101:101
ARG GIT_SHA=unknown
LABEL org.opencontainers.image.source="https://github.com/theohalden/Tinward" org.opencontainers.image.revision="$GIT_SHA"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
# Bypass image initialization scripts: configuration is immutable and fully supplied.
ENTRYPOINT ["nginx"]
CMD ["-g", "daemon off;"]
