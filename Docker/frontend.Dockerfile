# syntax=docker/dockerfile:1

# ==========================================================================
# PremiumCoffe — imagen del frontend (Nuxt 4 / Vue 3)
#
# Contexto de build: la raiz del repo.
# ==========================================================================

# --------------------------------------------------------------------------
# Stage 1 — deps
#
# Se copia SOLO package.json primero. Si esto no cambia (casi nunca), Docker
# reutiliza la capa y no vuelve a descargar ~300 MB de node_modules.
# El package-lock.json se copia tambien para installs reproducibles: sin el,
# dos builds en dias distintos pueden instalar versiones distintas.
# --------------------------------------------------------------------------
FROM node:22-alpine AS deps
WORKDIR /app

COPY Frontend/package.json Frontend/package-lock.json* ./
RUN npm ci --no-audit --no-fund

# --------------------------------------------------------------------------
# Stage 2 — build
# --------------------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY Frontend/ ./

# NUXT_TELEMETRY_DISABLED evita que el build intente phone home.
ENV NUXT_TELEMETRY_DISABLED=1
RUN npm run build

# --------------------------------------------------------------------------
# Stage 3 — runtime
#
# NO se copia node_modules entero: en producción Nuxt solo necesita los que
# estan en .output/server/node_modules, que ya son solo los de runtime.
# --------------------------------------------------------------------------
FROM node:22-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production \
    NUXT_TELEMETRY_DISABLED=1 \
    HOST=0.0.0.0 \
    PORT=3000

COPY --from=build --chown=node:node /app/.output ./.output

USER node

EXPOSE 3000

CMD ["node", ".output/server/index.mjs"]