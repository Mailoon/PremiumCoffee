FROM node:22-alpine@sha256:2c752226d477b4a886378baa95b9af252be59301b725fdb0b7e15208131505a8 AS deps
WORKDIR /app

COPY Frontend/package.json Frontend/package-lock.json* ./
RUN npm ci --no-audit --no-fund

FROM node:22-alpine@sha256:2c752226d477b4a886378baa95b9af252be59301b725fdb0b7e15208131505a8 AS build
WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY Frontend/ ./

ENV NUXT_TELEMETRY_DISABLED=1
RUN npm run build

FROM node:22-alpine@sha256:2c752226d477b4a886378baa95b9af252be59301b725fdb0b7e15208131505a8 AS app
WORKDIR /app

ENV NODE_ENV=production \
    NUXT_TELEMETRY_DISABLED=1 \
    HOST=0.0.0.0
RUN apk add --no-cache su-exec

COPY --from=deps --chown=node:node /app/node_modules ./node_modules
COPY --from=build --chown=node:node /app/.output ./.output
COPY --chown=node:node Frontend/ ./

COPY --chmod=755 Docker/frontend-entrypoint.sh /usr/local/bin/frontend-entrypoint.sh

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD wget -qO- "http://127.0.0.1:${PORT:-3000}/api/health" > /dev/null 2>&1 || exit 1
STOPSIGNAL SIGTERM

ENTRYPOINT ["/usr/local/bin/frontend-entrypoint.sh"]
