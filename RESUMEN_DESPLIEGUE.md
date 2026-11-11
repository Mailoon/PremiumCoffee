# 📋 Resumen de despliegue — PremiumCoffe

**Fecha:** 09/11/2026  
**Proyecto:** PremiumCoffe (E-commerce de café especializado con visor 3D)  
**Arquitectura:** Kotlin + Spring Boot 4.1.1 (Backend) + Nuxt 4 + Vue 3 (Frontend) + Supabase (PostgreSQL + Auth)

---

## 🌐 Servicios desplegados

| Componente | URL | Estado | Notas |
|------------|-----|--------|-------|
| **Backend (Kotlin)** | `https://premiumcoffee-3k6p.onrender.com` | ✅ Activo | Render (plan gratuito). Healthcheck `/actuator/health` → `UP`. |
| **Frontend (Nuxt)** | `https://premium-coffee-nine.vercel.app` | ✅ Activo | Vercel (plan gratuito). Proxy Nitro listo. |
| **Base de datos** | Supabase pooler (`aws-0-us-east-2.pooler.supabase.com:5432`) | ✅ Conectada | Base de datos `postgres`, usuario `postgres.rjjevwxlnrxbnjcdmvvx`. |
| **CI/CD** | GitHub Actions | 📄 Workflow listo | Archivo `.github/workflows/ci.yml` creado (pendiente de subir a `master`). |

---

## ✅ Completado

### Backend — Render
- Docker build exitoso con `Docker/backend.Dockerfile` (Eclipse Temurin 21 JDK/JRE).
- Perfil `prod` activo (`SPRING_PROFILES_ACTIVE=prod`).
- Conexión a Supabase PostgreSQL vía pooler funcionando (`HikariPool-1` iniciado).
- Puerto `8080` detectado por Render ("Detected service running on port 8080").
- Variables de entorno configuradas en Render:
  - `DB_URL`, `DB_USER`, `DB_PASSWORD`
  - `SUPABASE_URL`, `SUPABASE_ANON_KEY`
  - `PORT=8080`
  - `SPRING_PROFILES_ACTIVE=prod`
  - `CLOUDINARY_ENABLED=false`
  - `FLYWAY_ENABLED=true`

### Frontend — Vercel
- Desplegado desde carpeta `Frontend/` del monorepo.
- Root Directory configurado como `Frontend`.
- Healthcheck del frontend respondiendo: `{"status":"ok","service":"frontend"}`.

### Proxy Nitro (BFF)
- Archivo `Frontend/server/api/[...path].ts` corregido:
  - Importa `useRuntimeConfig` desde `#imports` (no de `h3`).
  - Se eliminó la función falsa `function useRuntimeConfig()` que tiraba `"Function not implemented."`.
- Lee `config.apiBase` desde variable de entorno `NUXT_API_BASE`.

---

## ⚠️ Pendiente / Por hacer

| Tarea | Prioridad | Detalle |
|-------|-----------|---------|
| **Estilos Tailwind CSS** | Alta | Falta `@import "tailwindcss";` al inicio de `Frontend/app/assets/css/main.css`. Actualmente la página se ve sin estilos (texto plano). |
| **Variable `NUXT_API_BASE` en Vercel** | Alta | Agregar en Vercel → Settings → Environment Variables: `NUXT_API_BASE = https://premiumcoffee-3k6p.onrender.com` (sin prefijo `NUXT_PUBLIC_`). |
| **Redeploy en Vercel** | Alta | Después de agregar la variable y arreglar Tailwind, hacer redeploy. |
| **Subir GitHub Actions** | Media | Subir `.github/workflows/ci.yml` a la rama `master`. |
| **Caddy / certificados** | Baja | No necesario: Render y Vercel proveen HTTPS automático en sus subdominios (`*.onrender.com`, `*.vercel.app`). |

---

## 📁 Estructura del monorepo

```
PremiumCoffe/
├── Backend/                    # Kotlin + Spring Boot 4.1.1 + Java 21
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── src/main/resources/application.properties
│   └── src/main/kotlin/...
├── Frontend/                   # Nuxt 4 + Vue 3 + Tailwind CSS v4
│   ├── nuxt.config.ts
│   ├── package.json
│   ├── app/
│   │   ├── assets/css/main.css   ← Falta @import "tailwindcss"
│   │   ├── pages/
│   │   ├── composables/
│   │   └── app.vue
│   └── server/api/
│       └── [...path].ts          # Proxy Nitro (corregido)
├── Docker/
│   ├── backend.Dockerfile
│   ├── frontend.Dockerfile
│   ├── docker-compose.yml
│   ├── Caddyfile
│   └── .env.example
├── .github/
│   └── workflows/
│       └── ci.yml                # Creado, pendiente de subir a GitHub
├── features/
│   └── plan_seguridad_ecommerce_kotlin_nuxt_supabase.md
└── RESUMEN_DESPLIEGUE.md         # Este archivo
```

---

## 🔧 Comandos útiles

### Subir cambios a GitHub (rama master)
```bash
cd /home/marlon/Documents/Proyectos/PremiumCoffe
git add .
git commit -m "mensaje del cambio"
git push origin master
```

### Arreglar estilos (Tailwind)
Editar `Frontend/app/assets/css/main.css` y agregar al principio:
```css
@import "tailwindcss";
```

Luego reiniciar servidor de desarrollo:
```bash
cd Frontend
bun run dev   # o npm run dev
```

### Agregar variable en Vercel
1. Ir a **Vercel → Proyecto → Settings → Environment Variables**
2. Agregar:
   - **Name:** `NUXT_API_BASE`
   - **Value:** `https://premiumcoffee-3k6p.onrender.com`
   - **Environment:** Production
3. Ir a **Deployments → Redeploy** en el despliegue más reciente.

### Probar backend directo (Render)
```bash
curl https://premiumcoffee-3k6p.onrender.com/actuator/health
curl https://premiumcoffee-3k6p.onrender.com/api/categories?size=5
```

### Probar proxy Nitro (Vercel → Render)
```bash
curl https://premium-coffee-nine.vercel.app/api/categories?size=5
```

> ⚠️ Nota: Render (plan gratuito) se duerme tras 15 min de inactividad. El primer request puede tardar 30–60 segundos en despertar.

---

## 📌 Flujo recomendado para continuar

1. Arreglar `@import "tailwindcss";` en `main.css`.
2. Subir cambios a GitHub (`git push origin master`).
3. Agregar `NUXT_API_BASE` en Vercel y hacer Redeploy.
4. Verificar `https://premium-coffee-nine.vercel.app/api/categories?size=5`.
5. Subir `.github/workflows/ci.yml` a GitHub.
6. (Opcional) Configurar UptimeRobot o cron para mantener backend despierto.

---

## 💾 Archivo generado

Este resumen está guardado en:  
`/home/marlon/Documents/Proyectos/PremiumCoffe/RESUMEN_DESPLIEGUE.md`

Puedes descargarlo directamente desde tu explorador de archivos o IDE.