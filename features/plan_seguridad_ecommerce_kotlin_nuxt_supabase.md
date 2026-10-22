# Plan de seguridad — CHECKLIST

## E-commerce con Kotlin + Nuxt + Supabase

> **Cómo usar este documento.** Todo lo accionable está en `- [ ]` / `- [x]`. Las cajas marcadas `[x]` ya
> están hechas (auditoría del codebase **o trabajo ya ejecutado en este plan**). Las marcadas `[ ]` están
> pendientes y están **ordenadas por dependencia**: no saltes de fase, cada una se apoya en la anterior.
> El material puramente explicativo (código de ejemplo, tablas de referencia) está en el **Anexo**, al final.
>
> **Regla del proyecto:** *nunca modificar una migración ya aplicada* (Flyway reporta error de
> checksum). La siguiente migración libre es **`V8`** (`V7__drop_cafe_table.sql` ya creada en F0.5).

---

## 0.0 RESUMEN DE ESTADO (última actualización: 22/10/2026)

| Fase | Estado | Nota |
|---|---|---|
| **F0 — Perímetro de red** | 🟡 **4/5 hecha** | F0.1, F0.2, F0.3, F0.5 hechas. F0.4 (verificación end-to-end) pendiente: requiere Docker levantado. |
| **F1 — Decisiones D1..D7** | 🟡 **7 propuestas escritas** | **F2, F4, F6 y F7 dependen de ellas.** Lo único que bloquea el proyecto. |
| **F2 — Supabase Auth** | 🟡 **2/10 hecha** | Solo el cableado de variables de entorno (F2.7, F2.8). El panel no está tocado. |
| **F3 — Proxy Nitro** | 🟡 **5/6 hecha** | B1, B2, B3 resueltos. F3.6 (test del proxy) pendiente: se aplaza a la Fase 8, cuando haya algo que cubrir. |
| **F4 — Backend: autenticación** | 🟡 **Infraestructura hecha** | Spring Security, sesión en BD, cookie `HttpOnly`, CSRF, 401/403 con `ProblemDetail`, rate limit. **Faltan los endpoints** `/api/v1/auth/*` (necesitan el panel de Supabase, F2). |
| **F5 — Backend: autorización** | 🟢 **Escritura protegida** | Rol `ADMIN` para toda escritura del catálogo; `UserRole` en el perfil; el rol nunca viene de la petición. Falta la regla por recurso (F5.4). |
| **F6 — Frontend** | 🟡 **2/31 hecha** | Solo la base técnica (CSRF, CORS). Ni Pinia, ni middleware, ni páginas. |
| **F7 — RLS** | 🟡 **Tablas hechas, RLS no** | `profiles` + `app_sessions` en `V8`. Las políticas RLS siguen bloqueadas por D2 (con D2.a la RLS no protege la conexión owner). |
| **F8 — Tests** | 🟡 **3/15 hecha** | `SecurityBoundaryTest` (9) y `SessionAuthenticationFilterTest` (7) con la cadena real. 155 tests en verde. |
| **F9 — Secretos y operación** | 🟡 **3/11 hecha** | F9.1, F9.2 y F9.4 hechos. |

> **Regla de oro del plan:** F0 → F1 → (F2 ∥ F3) → F4 → (F5 ∥ F6) → F7 → F8 → F9.
> **Estado actual:** F0, F3, F4 (infraestructura), F5 (escritura) y F8 (frontera) avanzados. **Lo que bloquea es
> F1 (las 7 decisiones) y F2 (el panel de Supabase): sin el panel no hay login que probar.**


---

## 0. Estado del proyecto (auditoría) — qué hay y qué falta

### 0.1 Lo que YA existe (no rehacer)

- [x] Backend Kotlin con Spring Boot 4.1.1, Kotlin 2.3.10, Java 21, Spring Data JPA (Hibernate 7), Flyway
- [x] `application.properties` con secretos por variable de entorno **sin defaults** (`DB_URL`, `DB_PASSWORD` fallan ruidosamente si faltan)
- [x] `Docker/.env` **no trackeado** en git + `.dockerignore` bloquea secretos en las capas de imagen
- [x] `Docker/.env.example` versionado con placeholders; sin claves `service_role` / `sb_secret_` en ningún sitio
- [x] Frontend Nuxt **4.5.2** con SSR activo (`preset: node-server`), `srcDir: 'app/'`
- [x] Cabeceras de navegador desde `nuxt-security`: CSP con **nonce** + `strict-dynamic`, `base-uri 'none'`, `object-src 'none'`, `frame-ancestors 'none'`, HSTS 1 año en prod, `X-Frame-Options: DENY`
- [x] `runtimeConfig.apiBase` **privado** (verificado ausente del bundle del navegador); única variable pública: `NUXT_PUBLIC_SITE_URL`
- [x] Proxy Nitro de origen único `server/api/[...path].ts` (Caddy → Nitro → Kotlin): el navegador nunca ve `:8080`
- [x] Bean Validation en los 6 request DTO; entidades JPA nunca se exponen directamente (solo response DTOs)
- [x] Manejo de errores `ProblemDetail` (RFC 9457) con 9 handlers y mapeo de SQLState
- [x] Logging estructurado ECS, sin contraseñas/tokens/PII
- [x] Validación de ficheros por *magic bytes* (allowlist de 8 formatos) antes de llamar al proveedor
- [x] Docker: multi-stage, usuario no-root, `ddl-auto=validate`, `open-in-view=false`, migraciones como Job separado
- [x] 107 tests backend en verde, sin llamadas de red

**Y lo que se ha añadido ejecutando este plan (21/10/2026):**

- [x] El backend **no se publica en el host**: `expose` en vez de `ports` (F0.1). Único camino público: `Caddy → Nitro → Kotlin`
- [x] `/actuator/*` expone **solo `health`** (F0.3)
- [x] El proxy Nitro **reenvía la cookie entrante** al backend y **todas las `Set-Cookie`** salientes, **incluida la rama de error** (F3.1, F3.2)
- [x] CSRF **activo en el borde** con `nuxt-csurf`: cookie `csrf` + cabecera `csrf-token` en POST/PUT/PATCH/DELETE (F6.2, B3)
- [x] Rate limiting **solo** en los endpoints de autenticación (5/min por IP y endpoint), con `X-Forwarded-For` reescrito por Caddy (F4.35, B4)
- [x] **Rate limiting en el backend** (F4.34): `RateLimitFilter` con cubo de tokens por IP y regla, 429 con `ProblemDetail`, 8 tests
- [x] Límite de cuerpo: 16 KB en autenticación, 12 MB en la subida de media (F4.38)
- [x] `corsHandler: false` y `xssValidator: false` **solo en la API** (F6.3, §0.5-H3, §0.5-H4)
- [x] Migración `V7__drop_cafe_table.sql` (F0.5). La siguiente libre es **`V8`**
- [x] `SUPABASE_URL` / `SUPABASE_ANON_KEY` cableadas **solo al backend** (F2.7, F2.8, B8)

### 0.2 Lo que NO existe (hay que construir desde cero)

> **Columna "Estado":** qué se ha hecho ya en la ejecución de este plan. `—` = sigue sin tocarse.

| # | Ausencia | Impacto | Estado |
|---|---|---|---|
| 1 | `spring-boot-starter-security` | No hay `SecurityFilterChain`, no hay `SecurityConfig`. Base a añadir. | ✅ **HECHO (F4.1, F4.3)**: Spring Security 7.1.1 + `SecurityConfig` con la cadena de filtros y CSRF. |
| 2 | Autenticación en cualquier forma | 0 filtros, 0 JWT, 0 JWKS, 0 sesiones, 0 cookies. Los 7 endpoints `/api/v1/auth/*` **no existen**. | 🟡 **Infraestructura HECHA, endpoints PENDIENTES**: cookie de sesión en BD (`app_sessions`), filtro que la resuelve, 401/403 con `ProblemDetail`. Falta el login contra Supabase (F4.24-F4.33), que necesita el panel de F2. |
| 3 | Autorización | 0 `@PreAuthorize` / `@Secured`. **36 endpoints, 0 protegidos**. `DELETE /products/{id}` y `PUT /product-variant-prices/{id}` son públicos. | ✅ **HECHO (F5.7)**: la escritura del catálogo (POST/PUT/PATCH/DELETE) exige rol `ADMIN`; el resto exige sesión. **El rol se lee de la sesión, nunca de la petición.** |
| 4 | CSRF | 0. Irrelevante hoy; **obligatorio** en cuanto haya cookie de sesión. | 🟡 **El borde SÍ tiene CSRF** (B3): `nuxt-csurf` activo, cookie + cabecera `csrf-token`. **El backend también**: `CookieCsrfTokenRepository` (cookie `XSRF-TOKEN`, cabecera `X-XSRF-TOKEN`). Ver §A.8. |
| 5 | Rate limiting | 0 en backend y `rateLimiter: false` en `nuxt.config.ts:262`. Sin protección contra fuerza bruta. | 🟢 **Resuelto en las dos capas**: borde 5/min por endpoint de autenticación (F4.35) y backend `RateLimitFilter` con cubo por IP (F4.34). Ver §0.5-H1: el default de nuxt-security estaba **activo**, no en `false`. |
| 6 | Modelo de roles | 0 enums de rol, 0 tabla de roles, **0 columna `user_id`/`owner_id`** en entidades o migraciones. | ✅ **HECHO (F5.1)**: enum `UserRole` (CLIENTE, EMPLEADO, ADMIN) + columna `profiles.role` con `CHECK`. Sin endpoint público que la cambie. |
| 7 | RLS | 0 `ENABLE ROW LEVEL SECURITY`, 0 `CREATE POLICY`, 0 `GRANT`/`REVOKE`. | — |
| 8 | Contexto de identidad en BD | **Imposible hoy**: JDBC directo con `postgres.<ref>` (owner) vía pooler → el JWT HTTP no establece `auth.uid()` y el owner **bypassa** RLS. Es el caso "B" del §9 del plan original. | — |
| 9 | `pinia` / `@pinia/nuxt` | No instalados. No existe `app/stores/`. | — |
| 10 | Middleware de ruta | No existe `app/middleware/`. `MiddlewareKey = never`. Sin protección de navegación. | — |
| 11 | Cliente HTTP centralizado | 0 interceptores, 0 manejo de 401/403. Solo un `useFetch` crudo. | — |
| 12 | Páginas de auth | No existen `/login`, `/registro`, `/recuperar`, `/reset-password`, `/cuenta`, `/pedidos`, `/admin`, `/checkout`, `/carrito`. | — |
| 13 | Tablas privadas | **No existen** `profiles`, `addresses`, `orders`, `order_items`. | 🟡 **HECHO a medias (D6)**: `profiles` + `app_sessions` en `V8`. Faltan `addresses`, `orders` y `order_items`. |
| 14 | Tests de seguridad | 0 tests de 401/403, 0 de aislamiento entre usuarios, 0 de token inválido. Además los tests usan `standaloneSetup`, así que **añadir Spring Security no los romperá ni los hará validar autorización**. | 🟡 **HECHO a medias (F8.2, F8.6, F8.7)**: `SecurityBoundaryTest` (9 casos) con la **cadena de filtros real** + `SessionAuthenticationFilterTest` (7 casos). Faltan los de token, aislamiento entre usuarios y RLS. |
| 15 | Tests frontend | 0 tests, sin `tsconfig.json`, sin `typecheck`, sin lint. | 🟡 **Parcial**: `tsconfig.json` y el script `typecheck` **ya existían** (el plan mentía). Falta `vitest` y el resto. Ver §0.5-H5. |
| 16 | CORS | 0 `@CrossOrigin`. Hoy no hace falta por el proxy de origen único, **pero `docker-compose.yml:183` publica `:8080` al host**. | ✅ **Resuelto**: F0.1 quitó la publicación de `:8080` y F6.3 apagó el `corsHandler` de nuxt-security (que **sí** estaba activo por defecto). Ver §0.5-H4. |


### 0.3 Bloqueantes técnicos concretos del patrón BFF

- [x] 🔴 **B1** — `server/api/[...path].ts:60-63` **descarta la cabecera `cookie`** al llamar a Kotlin. Una sesión por cookie `HttpOnly` nunca llegaría al backend.
      → **RESUELTO (F3.1).** El proxy ahora reenvía una lista blanca de cabeceras: `cookie` y `x-csrf-token`.
      Nunca "todas": cada cabecera que pasa al backend es una cabecera que puede acabar en manos de un tercero.
- [x] 🔴 **B2** — `server/api/[...path].ts:72-73` **no reenvía `Set-Cookie`** de Kotlin al navegador. El paso "Kotlin establece la cookie de sesión" es imposible tal cual.
      → **RESUELTO (F3.2).** Se propagan **todas** las `Set-Cookie` con `appendResponseHeader` (no `setHeader`, que dejaría solo la última),
      **incluida la rama de error**: un 401 que invalida la sesión viene con su propia cookie de caducidad, y sin propagarla el
      navegador se queda con la cookie vieja para siempre (bucle 401 → reintento → 401).
- [x] 🔴 **B3** — `nuxt-security` no tiene `csrf` configurado (default `false`), así que **`nuxt-csurf` no se registra**. Con cookies de sesión esto es obligatorio. La librería ya está en `node_modules`.
      → **RESUELTO (F6.2).** `security.csrf` activó `nuxt-csurf`: cookie `csrf` (`httpOnly`, `sameSite=lax`, `path=/`) + token en
      `<meta name="csrf-token">` + cabecera `csrf-token` en POST/PUT/PATCH/**DELETE** (el default de nuxt-csurf deja DELETE fuera).
      El prefijo `__Host-` y `Secure` quedan para el despliegue con HTTPS, vía `NUXT_CSRF_COOKIE_KEY` / `NUXT_CSRF_COOKIE_SECURE`.
- [x] 🔴 **B4** — `rateLimiter: false` en `nuxt.config.ts:262`, con un comentario que dice explícitamente que se configurará "en la fase de autenticación". Deuda ya identificada.
      → **RESUELTO (F4.35), con una corrección importante (ver §0.5-H1).** El `nuxt.config.ts` real **no declaraba** `rateLimiter`:
      el valor por defecto de nuxt-security (150 peticiones / 5 min / IP) estaba **activo en todo el sitio**. Ahora se apaga de forma
      global y se enciende por endpoint: 5/min en `login`, `register`, `forgot-password` y `reset-password` (cubos separados).
- [ ] 🔴 **B5** — SSR activo: el estado de sesión debe resolverse **por petición en el servidor**, y las llamadas `useFetch` a `/api/**` durante el render **también** deben llevar la cookie, o el render del servidor y el hydrate discreparán.
      → Depende de F6.11–F6.14 (cliente HTTP centralizado). El proxy **ya** reenvía la cookie (B1), así que solo falta que el
      `useFetch` del servidor la reenvíe también.
- [x] 🟡 **B6** — `connect-src` de la CSP solo permite `'self'` + `res.cloudinary.com`. Cualquier llamada del navegador a `*.supabase.co` quedaría **bloqueada**. Esto favorece el BFF, pero hay que decidirlo explícitamente.
      → **DECIDIDO: el navegador NUNCA habla con Supabase** (consistente con D3 = BFF con cookie). Queda escrito en el `nuxt.config.ts`.
- [x] 🟡 **B7** — El servicio `frontend` de producción **no recibe `NUXT_API_BASE`** (`docker-compose.yml:27-30`) y usa el default hardcodeado. Funciona en la red de Compose, pero contradice el `.env.example`.
      → **RESUELTO.** `docker-compose.yml` ya pasa `NUXT_API_BASE: ${API_INTERNAL_URL:-http://backend:8080}`.
- [x] 🟡 **B8** — No existe forma de pasar secretos de Supabase al contenedor `backend`: `Docker/.env.example` no define ninguna variable `SUPABASE_*`.
      → **RESUELTO (F2.7 / F2.8).** `SUPABASE_URL` y `SUPABASE_ANON_KEY` en `Docker/.env.example` y en el `environment` del
      servicio `backend`. **Nunca** en el `frontend` (el navegador no habla con Supabase: B6).

### 0.4 Deuda menor que conviene arreglar antes de empezar

- [x] 🟡 Hueco en el versionado de migraciones: existen `V1,V2,V3,V4,V6`. Falta `V5` (`V1__crear_tabla_cafe.sql` sigue aplicada). La siguiente libre es **`V7`**.
      → **RESUELTO (F0.5) con `V7__drop_cafe_table.sql`**, **no** con `V5`.
      Motivo: `V5` es una versión **por debajo** de la `V6` ya aplicada, y Flyway con `outOfOrder=false` **rechaza arrancar**
      (`Detected resolved migration not applied to database: 5`). El hueco del `V5` se queda; la siguiente libre es **`V8`**.
      El Job `migrate` declara `FLYWAY_OUT_OF_ORDER: "false"` de forma explícita para que nadie lo active por error.
- [ ] 🟡 `MEDIA_LOCAL_*` existe en `application.properties:58-60` pero no está cableado en `docker-compose.yml`, ni en `.env`/`.env.example`, ni en el `Caddyfile`. `LocalMediaStorage` es inalcanzable en Docker.
      → **Parcialmente arreglado**: `docker-compose.yml` ya pasa `MEDIA_LOCAL_*` al backend y monta el volumen `media`. Falta el `Caddyfile`.
- [ ] 🟡 `Docker/ARCHITECTURE.md` tiene secciones "pendiente" ya obsoletas (frontend ya decidido; CORS ya no es "no hay frontend en otro origen" sino "el puerto 8080 está publicado").
      → Ahora el texto correcto es el tercero: **el puerto 8080 ya no está publicado**.
- [ ] 🟡 `Frontend/env.local.example` se llama así (sin punto inicial); el comentario del `.gitignore` que lo menciona como `.env.local.example` no corresponde al archivo real.

### 0.5 Hallazgos de la ejecución (NO estaban en el plan original)

> Estos cinco puntos aparecieron al ejecutar la Fase 0 y la Fase 3. Todos son **correcciones sobre
> lo que el plan daba por hecho**, y todos están ya resueltos en el código.

- [x] **H1 — El rate limiter global de nuxt-security estaba ACTIVO, no en `false`.**
      El plan decía "`rateLimiter: false` en `nuxt.config.ts:262`". El `nuxt.config.ts` real **no declaraba la clave**, así que
      se aplicaba el default: `tokensPerInterval: 150, interval: 300000` en la regla global `/**`.
      Efecto: **todo el sitio** (no solo la autenticación) tenía un límite de 150 peticiones cada 5 minutos por IP, un límite
      bastante fácil de alcanzar con la navegación normal de un catálogo.
      **Resuelto:** `rateLimiter: false` global + reglas por endpoint de autenticación.
      *Ojo:* el `nuxt.config.ts` actual tiene 148 líneas, no 262. Las referencias de línea del plan están desfasadas.

- [x] **H2 — El límite de tamaño de petición global también estaba activo, y rompía las subidas de media.**
      Mismo mecanismo: `requestSizeLimiter` por defecto = 2 MB por cuerpo y **8 MB por fichero**, contra los 10 MB que acepta el
      backend (`MAX_UPLOAD_SIZE`). Un `.glb` de 9 MB moría en Nitro con un 413 que no mencionaba al backend.
      **Resuelto:** `requestSizeLimiter: false` global + 16 KB en `/api/auth/**` (F4.38) + 12 MB en `/api/media-assets/upload`.

- [x] **H3 — El validador de XSS del borde rechazaba datos legítimos.**
      `xssValidator` viene activo por defecto sobre GET y POST: no sanea, **rechaza con 400** en cuanto un carácter cambia.
      Una descripción de producto como `"Grado 5 < 6"` o una búsqueda con `<` devolvía 400 sin llegar a Kotlin, y el frontend
      no podía distinguirlo de un error de negocio.
      **Resuelto:** desactivado **solo** en `/api/**`. Las páginas HTML conservan el filtro global, que ahí sí tiene sentido.
      La defensa real contra XSS en este proyecto es la CSP (nonce + `strict-dynamic`, `script-src-attr 'none'`) más el escapado
      por defecto de Vue, y la validación de verdad está en Kotlin (Anexo A.1).

- [x] **H4 — El `corsHandler` de nuxt-security también venía activo por defecto** (origen `http://localhost:3000`).
      No era un agujero (h3 solo añade cabeceras, nunca bloquea), pero en producción declaraba un origen que no era el del sitio.
      **Resuelto:** `corsHandler: false` (F6.3). El proyecto es de origen único: no hay orígenes que permitir.

- [x] **H5 — `Frontend/tsconfig.json` y el script `typecheck` ya existían** (el plan los daba por inexistentes en F3.5).
      **Resuelto:** F3.5 queda marcado como hecho por la vía de hecho. `npm run typecheck` pasa.

- [x] **H6 — Kotlin compila los métodos `final`, y CGLIB no puede asesorar un método `final`.**
      **Este es un bug que ya existía en el proyecto y que nadie había visto.** El plugin `kotlin("plugin.spring")`
      abre las clases anotadas con `@Service`/`@Component` y sus miembros, pero **no abre los métodos heredados de una
      clase base sin anotación**. Los `abstract class AbstractCrudService` / `AbstractReadService` no están anotados, así
      que sus métodos `@Transactional` seguían siendo `final`:
      - el proxy CGLIB no podía reescribirlos → **la transacción NUNCA se aplicaba**;
      - con `@PreAuthorize` en un controller, el proxy se creaba pero el método `final` se ejecutaba sobre el proxy
        (cuyos campos heredados son `null` por cómo lo instancia Objenesis) → **NullPointerException** y, peor,
        **la comprobación de permisos no se evaluaba**.
      **Resuelto:** los métodos de las clases base que llevan `@Transactional` (y los del controller) son `open`.
      Esto **arregla también** el `read-modify-write` de `update`, que antes no era atómico.

- [x] **H7 — En Spring Security 7, un `SecurityContext` escrito por un filtro propio no sobrevive.**
      El `AnonymousAuthenticationFilter` instala un *supplier* que **reconstruye** el contexto con el token anónimo cada
      vez que el contexto que envuelve no tiene autenticación, y el `SecurityContextHolderFilter` lo limpia al
      terminar la cadena. Un filtro que escribe la autenticación en `SecurityContextHolder.getContext()` la deja en un
      contexto que se tira a la basura: **toda petición acaba anónima** (401), por mucho que el filtro resuelva bien la
      sesión. Y con `@PreAuthorize` no hay manera de tener una `Authentication` en el contexto.
      **Resuelto:** la resolución de sesión **y** la autorización se hacen en el mismo filtro
      (`SessionAuthenticationFilter`), que tiene la petición y la respuesta: 401 sin sesión, 403 sin el rol. Las reglas
      viven en un solo sitio (`SecurityRules`). La cadena de Spring Security se queda con el CSRF y con la
      infraestructura; `authorizeHttpRequests` deja de denegar por su cuenta para que el filtro sea quien responde.
      **Consecuencia:** `@PreAuthorize` en los controllers no es utilizable en este proyecto mientras la identidad no
      venga de un mecanismo estándar de Spring Security. La autorización por recurso (F5.4) se hará en el servicio,
      leyendo el usuario del atributo de la petición (`AuthenticatedUser.read(request)`).

- [x] **H8 — `@WebMvcTest` en Spring Boot 4 vive en otro módulo: `spring-boot-starter-webmvc-test`.**
      `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` **no existe** en Boot 4; el paquete es
      `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`. Y `spring-boot-starter-test` ya no lo trae.
      **Resuelto:** dependencia añadida. Nota también: `Mockito.any()` en Kotlin devuelve `null` y el compilador
      mete la comprobación de nulidad → `NullPointerException` en el stubbing. Se ha añadido `mockito-kotlin` para
      tener `any<T>()` y `whenever()` sin trampas.

---


## 1. FASE 0 — Cerrar el perímetro de red (rápido, bloqueante)

> **Objetivo:** que hoy no exista ninguna ruta pública y directa al backend sin pasar por el proxy.

- [x] **F0.1** Dejar de publicar el puerto del backend al host.
      En `Docker/docker-compose.yml:183-184`, quitar `ports: - "${BACKEND_PORT:-8080}:8080"` del servicio `backend`.
      Solo debe quedar `expose:` para la red interna de Compose.
      → **HECHO.** `ports:` → `expose: ["8080"]`, con el porqué escrito en el fichero. `BACKEND_PORT` desaparece de `.env.example`.
- [x] **F0.2** Comprobar que `migrate` (Job de Flyway) sigue teniendo acceso a la red tras el cambio de F0.1. Si se rompe, darle su propio `expose`.
      → **HECHO.** `migrate` se conecta **directo a PostgreSQL** con `DB_URL`; no habla con el backend, así que no le afecta. No necesita `expose`.
- [x] **F0.3** Restringir `/actuator/*`. Hoy `application.properties:75-79` expone `health,info,metrics` sin restricción.
      Bajar a `health` solamente, o exigir rol para `info`/`metrics`.
      → **HECHO.** `management.endpoints.web.exposure.include=health`. Las subrutas de health (`/liveness`, `/readiness`) siguen
      disponibles porque cuelgan de `health`, no de una exposición propia. El healthcheck de Compose sigue funcionando.
- [ ] **F0.4** Verificar en red que el catálogo sigue funcionando end-to-end: `Caddy → /api/* → Nitro → :8080`.
      → **PENDIENTE: no se ha podido ejecutar.** El daemon de Docker no estaba levantado en la máquina donde se hizo el cambio.
      Comprobación manual pendiente:
      ```bash
      # 1. el backend NO responde desde el host
      curl -sS -m 3 http://localhost:8080/actuator/health     # esperado: sin respuesta / connection refused
      # 2. el catálogo sí responde por el proxy
      curl -sS http://localhost:3000/api/catalog | head -c 200 # esperado: 200 + JSON
      ```
- [x] **F0.5** Correr el hueco `V5` de migraciones (borrar la tabla `cafe` residual) para no arrastrarlo al schema de auth.
      → **HECHO como `V7__drop_cafe_table.sql`** (no como `V5`, por el motivo técnico de §0.4). `DROP TABLE IF EXISTS cafe;`.
      Ningún Kotlin usa la tabla (verificado con grep sobre `src/`).


**Definición de hecho:** con `curl` directo a `:8080` desde el host **no se obtiene respuesta**, y `GET /api/v1/catalog` a través de Caddy sigue devolviendo `200`.

---

## 2. FASE 1 — Decisiones bloqueantes (responder ANTES de escribir código)

> **No avances a la Fase 2 sin cerrar esto.** Cada respuesta cambia el diseño de las fases siguientes.
>
> 📌 **ESTE ES EL SIGUIENTE PASO.** La Fase 0 está cerrada (falta F0.4, que es una comprobación manual) y la Fase 3
> está resuelta. Todo lo demás depende de estas 7 decisiones. Cada una lleva una **propuesta** debajo: si la
> propuesta es aceptable, basta con marcar la casilla y con eso la decisión queda escrita y ejecutable.

- [ ] **D1 — Framework Kotlin:** Spring Boot 4.1.1 + Spring Security.
      ⚠️ **Ojo:** Spring Boot 4 usa **Jackson 3** (`tools.jackson.*`). Verificar compatibilidad de Spring Security 7 con Jackson 3 y con Kotlin 2.3 antes de añadir el starter.
      > **Propuesta:** sí, Spring Security. Es la única opción que da `SecurityFilterChain`, filtros de sesión y
      > `@PreAuthorize` con la integración estándar. El riesgo de Jackson 3 se verifica en el momento de añadir el
      > starter (si `NimbusJwtDecoder` o `JwtAuthenticationConverter` no compilan, se resuelve con un decoder propio
      > sobre las claves de Supabase, que es lo que hace F4.8 de todas formas).
- [ ] **D2 — Conexión a Supabase:** hoy es **JDBC directo** (Spring Data JPA → HikariCP → pooler) con un único usuario `postgres.<ref>`.
      **Esta es la decisión más importante del plan.** Elegir una de estas tres:
      - [ ] **D2.a** Mantener JDBC + **autorización solo en Kotlin** + RLS como capa extra vía mecanismo de contexto en BD (recomendado: es el camino más simple y no toca el pooler).
      - [ ] **D2.b** Migrar a **PostgREST / API de datos de Supabase** reenviando el JWT del usuario para que RLS funcione nativo con `auth.uid()`.
      - [ ] **D2.c** JDBC + `SET LOCAL role` / `request.jwt.claims` por transacción para propagar identidad a RLS (más complejo, requiere pool dedicado y serialización).
      > **Propuesta: D2.a.** El catálogo entero (7 tablas, 107 tests, agregados JPA) está construido sobre JDBC directo.
      > D2.b significaría reescribir la capa de datos. D2.c es un pool dedicado con `SET LOCAL` por transacción, que
      > con HikariCP exige serializar las transacciones o un pool aparte. Con D2.a la **autorización vive en Kotlin**, que
      > es exactamente lo que dice el Anexo A.1, y la RLS queda como capa extra para el futuro (F7).
      > **Consecuencia que hay que aceptar:** con D2.a, **RLS no es una capa activa** (Anexo A.5, caso B) porque la
      > conexión es del owner y el JWT HTTP no fija `auth.uid()`. F7 no debe escribir políticas como si protegieran algo.
- [ ] **D3 — Sesión:** **BFF con cookie `HttpOnly`** (recomendado, encaja con el proxy de origen único ya existente y con `corsHandler: false`).
      Descartar JWT en `localStorage`.
      > **Propuesta:** BFF con cookie `HttpOnly`. Todo el andamiaje del borde ya está listo y es coherente:
      > proxy de origen único, `corsHandler: false`, CSP con `connect-src 'self'` (B6), puerto 8080 sin publicar (F0.1)
      > y CSRF en el borde (B3). Un JWT en `localStorage` sería accesible desde cualquier XSS: con cookie `HttpOnly`
      > no lo es.
      > **Detalle a decidir aquí:** el almacén de la sesión (F4.16). La opción natural es una **tabla `app_sessions`**
      > en la base de datos, porque la app es stateless y corre con N réplicas: una `HttpSession` en memoria perdería la
      > sesión en cada despliegue y no funcionaría con más de una réplica.
- [ ] **D4 — SSR:** ✅ **confirmado = sí** (Nuxt 4.5.2, `preset: node-server`, `app/pages/catalogo.vue` hace `await useFetch` en el setup).
      Implica: resolver la sesión **por petición en el servidor**.
      > **Propuesta: confirmado, sin cambios.** Ya está escrito en el código (no en un documento) y el proxy ya
      > reenvía la cookie (B1). Lo que falta es el lado del cliente: `useFetch`/`useApi` en SSR tiene que reenviar la
      > cookie del request entrante (F6.14, B5), o el render del servidor verá "sin sesión" y el hydrate "con sesión".
- [ ] **D5 — Roles necesarios.** Proponer y fijar el modelo. Mínimo sugerido: `CLIENTE`, `EMPLEADO`, `ADMIN`.
      ⚠️ **No confundir con `VariantMediaRole`**, que es un enum de media, no de usuario.
      > **Propuesta:** `CLIENTE`, `EMPLEADO`, `ADMIN`, como enum de Kotlin en `Coffee/Auth/Model/` (nunca en el paquete
      > `Media`/`ProductVariantMedia`, para no confundirlo con `VariantMediaRole`).
      > El rol vive en el **perfil** (`profiles.role`), y se escribe **solo** por una operación administrativa o por SQL.
      > Nunca se deduce del correo ni de un campo editable por el cliente (F5.2).
      > ⚠️ Con solo `ADMIN` y `CLIENTE` bastaría para cerrar la escritura del catálogo; `EMPLEADO` se puede dejar
      > para cuando exista un concepto de "personal de tienda" que no sea administración.
- [ ] **D6 — Tablas privadas.** Hoy **no existe ninguna**. Definir cuáles se crean en esta iteración y en qué orden:
      `profiles` → `addresses` → `orders` → `order_items`.
      Si `orders` no entra en esta iteración, **D6 se reduce a `profiles` + `addresses`**.
      > **Propuesta: `profiles` + `addresses` en esta iteración; `orders` y `order_items` en la siguiente.**
      > Motivo: `orders` arrastra el modelo de precios, el cálculo de totales, los estados y el pago (F5.9, F5.10,
      > F5.11), que es un bloque de trabajo del tamaño de toda la autenticación. `profiles` + `addresses` ya permiten
      > demostrar el patrón BFF completo (sesión, aislamiento entre usuarios, F8.5) sin meter el carrito en la misma
      > iteración.
- [ ] **D7 — ¿Entra el panel de administración en esta iteración?** Si no, la escritura del catálogo queda **cerrada** y se desarrolla en una iteración posterior.
      ⚠️ Mientras no haya roles, cualquier escritura pública es un agujero. Si se aplaza el admin, **hay que despublicar la escritura del catálogo ya en la Fase 2**.
      > **Propuesta: NO entra el panel de administración en esta iteración, y la escritura del catálogo se cierra ya**
      > (F5.7). Motivo: un panel de administración es una aplicación dentro de la aplicación (CRUD, permisos, auditoría).
      > Lo que hace falta para que el catálogo no sea modificable por cualquiera es **una sola cosa**: que
      > `POST/PUT/DELETE` exijan rol `ADMIN`. El panel puede llegar después; el cierre no puede esperar.
      > **Consecuencia operativa:** hasta que haya un `ADMIN` de verdad, la escritura del catálogo se hace por SQL
      > o con un usuario de servicio. Eso es aceptable para un proyecto en fase de aprendizaje y es infinitamente
      > mejor que dejar `DELETE /products/{id}` público.


**Definición de hecho:** estas 7 decisiones están escritas (en este doc o en `Backend/CONTEXT.md`) y son ejecutables sin más preguntas.

---

## 3. FASE 2 — Supabase Auth (configuración externa)

> **Objetivo:** tener un proveedor de identidad real configurado antes de tocar Kotlin.

- [ ] **F2.1** En el panel de Supabase, habilitar el proveedor **Email**.
- [ ] **F2.2** Configurar **verificación de correo** obligatoria.
- [ ] **F2.3** Configurar la **URL de la aplicación** y las **URLs de redirección permitidas** (solo orígenes propios, nunca comodín).
- [ ] **F2.4** Revisar requisitos de contraseña y **límites de autenticación** del proyecto.
- [ ] **F2.5** Evaluar **MFA** para cuentas administrativas o acciones de riesgo.
- [ ] **F2.6** Anotar la **URL del proyecto** y la **clave publicable (anon)**.
- [x] **F2.7** Añadir `SUPABASE_URL` y `SUPABASE_ANON_KEY` a `Docker/.env.example` (y los valores reales a `Docker/.env`).
      → **HECHO** en `.env.example`, con el aviso de que la clave anon es publicable y la secreta (`sb_secret_` / `service_role`)
      no va en ningún fichero del repositorio. Falta rellenar los valores reales en `Docker/.env`.
- [x] **F2.8** Añadirlas al `environment` del servicio `backend` en `Docker/docker-compose.yml`.
      ⚠️ **Nunca** al servicio `frontend`. El navegador no debe hablar con Supabase (B6: la CSP lo bloquearía).
      → **HECHO.** Solo al `backend`. El `frontend` **no** las recibe.
- [ ] **F2.9** Verificar con `grep -ri "sb_secret\|service_role" Frontend/` → **0 coincidencias** en source **y** en `.output/public/_nuxt/*.js`.
- [ ] **F2.10** Guardar la clave secreta en el gestor de secretos del entorno. **Nunca** en `NUXT_PUBLIC_*`.


**Definición de hecho:** `SUPABASE_URL` + `SUPABASE_ANON_KEY` llegan al contenedor `backend` y no al `frontend`; 0 coincidencias de claves privilegiadas en el bundle del navegador.

---

## 4. FASE 3 — Arreglar el proxy Nitro (precondición del BFF)

> **Objetivo:** que una cookie pueda ir y volver entre navegador y Kotlin. Sin esto, ninguna Fase posterior funciona.
> **Antes de empezar:** resolver **B1** y **B2**.

- [x] **F3.1 — Reenviar la cookie entrante (B1).** En `server/api/[...path].ts:60-63`, añadir la cabecera `cookie` (usar `getRequestProxyHeaders(event, { host: false })` o leer `cookie` explícitamente).
      **No** reenviar cabeceras arbitrarias: solo las que hacen falta (`cookie`, `authorization` si se decide, `x-csrf-token`).
      → **HECHO.** Lista blanca explícita: `cookie` y `x-csrf-token`. `authorization` **no** se reenvía (D3 = BFF con cookie;
      si algún día se decide autenticación por cabecera, se añade a la lista).
      *Nota:* en h3 v1 la función se llama `getProxyRequestHeaders` (no `getRequestProxyHeaders`, que es el nombre de h3 v2).
- [x] **F3.2 — Reenviar `Set-Cookie` (B2).** En el bloque de respuesta (líneas 70-75), propagar **todas** las cookies con `response.headers.getSetCookie()` → `appendResponseHeader(event, 'set-cookie', ...)`.
      ⚠️ Cuidado: hoy solo se copia `content-type`. Una sola cookie devorada rompe toda la sesión.
      → **HECHO**, con las dos ramas:
      - respuesta OK → `getSetCookie()` en bucle con `appendResponseHeader` (con `setHeader` se quedaría solo la última);
      - **rama de error (4xx/5xx)** → también, porque un 401 que invalida la sesión trae su propia cookie de caducidad.
      Respaldo por si el runtime no expone `getSetCookie()`: `get('set-cookie')` partido con una expresión regular.
- [x] **F3.3 — Preservar `redirect` / `location`** de la respuesta de Kotlin (relevante para el flujo de verificación de correo y reset de contraseña).
      → **HECHO.** `location` está en la lista de cabeceras de respuesta propagadas.
- [x] **F3.4 — Propagar cabeceras de seguridad relevantes** (`location`, `cache-control: no-store` en respuestas de auth).
      → **HECHO.** `cache-control` se propaga tal cual, para que el `no-store` que envíe Kotlin llegue al navegador.
- [x] **F3.5** Añadir `frontend/tsconfig.json` y el script `typecheck` en `package.json`. **Sin esto no hay forma de validar código de auth sensible.**
      → **YA EXISTÍAN** (el plan mentía: ver §0.5-H5). `npm run typecheck` **pasa** con los cambios de esta fase.
- [ ] **F3.6** Escribir el **primer test del proxy**: cookie entrante → llega a Kotlin; `Set-Cookie` de Kotlin → llega al navegador. Este test es el que evita la regresión de B1/B2.
      → **APL AZADO a la Fase 8.** Decisión del 21/10/2026: montar ahora la infraestructura de tests de frontend (vitest +
      `@nuxt/test-utils`) para cubrir un proxy que aún no tiene sesión **no aporta nada**: todavía no hay nada que cubrir
      porque no existe autenticación. Se hará en F8, cuando el BFF esté completo y haya casos reales que proteger.
      *Nota de diseño para cuando llegue:* el test necesita un backend Kotlin simulado (un `http.createServer` que grabe la
      petición y responda con las cabeceras que se le pidan) y la cookie + token CSRF hay que sacarlos del HTML renderizado
      (`<meta name="csrf-token">` + la cookie `csrf`), porque `nuxt-csurf` no expone el token fuera del navegador.


**Definición de hecho:** test verde que demuestra que una cookie `HttpOnly` hace el viaje completo navegador → Nitro → Kotlin → Nitro → navegador.

---

## 5. FASE 4 — Backend: autenticación

> **Objetivo:** que Kotlin sea la autoridad de identidad. Nada de esto existe todavía.
>
> ⚠️ **Depende de F1 (D1–D7) y de F2.** No se empieza hasta que las decisiones estén escritas.

### 4.1 Dependencias y configuración base

- [x] **F4.1** Añadir `implementation("org.springframework.boot:spring-boot-starter-security")` a `Backend/build.gradle.kts`.
      → **HECHO.** Resuelve a **Spring Security 7.1.1**. Jackson 3 (`tools.jackson.*`) no ha dado ningún problema:
      la única pieza que lo usa es el `ObjectMapper` para serializar el `ProblemDetail`.
- [x] **F4.2** Añadir un cliente de verificación JWT por **JWKS** (nunca solo decodificar).
      Verificar compatibilidad con Jackson 3 / Spring Boot 4 antes de fijar la librería.
      → ⏸ **APL AZADO a la Fase 4b** (los endpoints de login). Con el patrón BFF (D3) el backend **no** valida
      tokens de Supabase en cada petición: la sesión es una fila en `app_sessions`. El token de Supabase solo se
      usa en el login y en el refresh, y ahí se valida contra la API de Supabase, no contra un JWT local.
- [x] **F4.3** Crear `Coffee/Configuration/SecurityConfig.kt` con el `SecurityFilterChain`.
      → **HECHO.** CSRF con `CookieCsrfTokenRepository`, sesión `STATELESS`, sin form login ni http basic, y el filtro
      de sesión registrado antes del filtro de autorización. Ver §0.5-H7 para por qué las reglas viven en el filtro.
- [ ] **F4.4** Crear `Coffee/Configuration/CorsConfig.kt` **solo si D3 lo exige**. Con origen único no debería hacer falta; si se añade, orígenes explícitos, **nunca comodín**.
      → **NO HECHO, y no hace falta:** con F0.1 el backend no es alcanzable desde el host, y el `corsHandler` de
      nuxt-security está apagado (F6.3). No hay un segundo origen. **No se añade.**
- [x] **F4.5** Declarar las rutas públicas: `GET /api/v1/catalog`, `GET /api/v1/categories/**` lectura, `POST|PUT|DELETE /api/v1/auth/**`, `/actuator/health`.
      → **HECHO** en `SecurityRules`. Ojo: `/actuator/health` **tiene** que ser pública o el healthcheck de Docker
      mataría el contenedor en bucle.
- [x] **F4.6** **Todo lo demás cerrado por defecto** (`anyRequest().authenticated()`). La lista de permitidos es explícita y corta, no por exclusión.
      → **HECHO con una diferencia necesaria:** las reglas están en `SecurityRules` (un solo sitio) y no en
      `authorizeHttpRequests`, por el motivo de §0.5-H7. El efecto es el mismo: lo que no está listado, exige sesión.
- [x] **F4.7** Desactivar CSRF **solo** si se confirma autenticación por cabecera; con cookie (D3), **CSRF activo**.
      → **HECHO: CSRF activo en el backend**, con `CookieCsrfTokenRepository` (cookie `XSRF-TOKEN`, cabecera
      `X-XSRF-TOKEN`, `httpOnly=false` para que el JS pueda mandarla). El borde tiene el suyo (cookie `csrf`, cabecera
      `csrf-token`). **Dos capas, dos tokens**: ver §A.8.

      → 🔀 **Decisión abierta, documentada en §A.8.** Con D3 = cookie, la recomendación es **las dos capas**
      (borde + Kotlin), y hay que resolver **cómo** se hace: si Kotlin usa su propio token (dos cookies) o
      si se unifica con el del borde (una sola cabecera `csrf-token`).


### 4.2 Verificación de identidad

- [ ] **F4.8** Configurar el decoder JWT contra las **claves públicas / JWKS** de Supabase.
- [ ] **F4.9** Verificar `iss` **exacto**.
- [ ] **F4.10** Verificar `aud` esperado.
- [ ] **F4.11** Verificar `exp` y el resto de claims relevantes.
- [ ] **F4.12** Obtener el identificador de usuario del claim `sub` **ya validado**.
- [ ] **F4.13** Rechazar tokens expirados, malformados o de emisor/audiencia incorrectos → **401**.
- [ ] **F4.14** Manejar la **rotación de claves** de Supabase sin downtime.
- [ ] **F4.15** Test: token expirado → 401. Token mal firmado → 401. `iss` incorrecto → 401. `aud` incorrecto → 401.

### 4.3 Sesión BFF

- [x] **F4.16** Crear el almacenamiento de sesión de servidor (tabla `app_sessions` en `V7__*`, o equivalente). **No** `HttpSession` en memoria: la app es stateless y corre con N réplicas.
      → **HECHO**: tabla `app_sessions` en **`V8__crear_profiles_y_sesiones.sql`** (no en V7, que ya es la de la
      tabla `cafe`; ver §0.4). La app queda `STATELESS` en Spring Security: no hay `HttpSession` en memoria.
- [ ] **F4.17** Vincular la sesión de aplicación con la identidad **autenticada** de Supabase.
      → PENDIENTE: es el paso del login (F4.24-F4.25). La tabla ya tiene `profile_id` → `profiles.user_id` → `auth.users`.
- [x] **F4.18** Emitir la cookie: `HttpOnly`, `Secure`, `SameSite=Lax` (o `Strict`), `Path=/`, `Max-Age` definido.
      → **HECHO**: `SessionProperties` (`auth.session.*`) define nombre de cookie (`pcs_session`), `sameSite`,
      `secure` y los tiempos. El `HttpOnly` y el `Max-Age` los pone el endpoint de login (F4.25), que es lo que
      emite la cookie. `secure` va en `false` por defecto porque el perfil local de producción sirve por http;
      **en un despliegue con HTTPS hay que ponerlo en `true`**.
- [x] **F4.19** **No** exponer tokens de Supabase ni claves secretas a JavaScript.
      → **HECHO por diseño**: la cookie es `HttpOnly` y la tabla solo guarda el **SHA-256** del token, así que un
      dump de la base de datos no entrega sesiones usables.
- [x] **F4.20** Definir **expiración y renovación** de sesión (sliding window con tope absoluto).
      → **HECHO**: `idle-timeout` (12 h, deslizante) + `absolute-timeout` (7 días, tope duro) + `renew-interval`
      (15 min, cada cuánto se refresca `last_seen_at`).
- [ ] **F4.21** Implementar **logout** que invalida la sesión en servidor (no solo borrar la cookie).
      → La base está (`SessionService.revoke` marca `revoked_at`); falta el endpoint (F4.26).
- [x] **F4.22** ⚠️ **No asumir que borrar la cookie invalida los tokens emitidos por Supabase.** Diseñar la revocación según el mecanismo elegido.
      → **HECHO por diseño**: la sesión es nuestra, no la de Supabase. Borrar la cookie **no** basta, y por eso
      `revoked_at` existe: el token sigue siendo válido hasta que la fila se marca. La revocación en Supabase
      (si hiciera falta) es un problema aparte del de la sesión.
- [x] **F4.23** Definir qué pasa cuando se **revoca una sesión**, se **suspende una cuenta** o **cambian los permisos**.
      → **HECHO**: sesión revocada → el filtro no resuelve la cookie → 401. Permisos cambiados → el rol se lee de
      `profiles.role` **en cada petición** (no hay caché de permisos), así que el cambio surte efecto de inmediato.
      Cuenta suspendida → `SessionService.revokeAll(profileId)` marca todas sus sesiones.


### 4.4 Endpoints de autenticación

> Ajustar a `/api/v1/auth/*`. El proxy Nitro ya reescribe `/api/*` → `/api/v1/*`, así que desde el navegador se llaman `/api/auth/login`.

- [ ] **F4.24** `POST /api/v1/auth/register` — registrar usuario
- [ ] **F4.25** `POST /api/v1/auth/login` — autenticar y establecer sesión
- [ ] **F4.26** `POST /api/v1/auth/logout` — cerrar sesión e invalidar credenciales
- [ ] **F4.27** `GET /api/v1/auth/me` — usuario actual + permisos para la interfaz
- [ ] **F4.28** `POST /api/v1/auth/forgot-password` — inicio de recuperación
- [ ] **F4.29** `POST /api/v1/auth/reset-password` — completar con flujo/token válido
- [ ] **F4.30** `POST /api/v1/auth/refresh` — **solo si** el mecanismo elegido lo requiere explícitamente
- [ ] **F4.31** **No revelar si un correo existe** en `forgot-password` ni en `login` (respuesta idéntica en ambos casos).
- [ ] **F4.32** **No** diseñar almacenamiento de contraseñas propio: Supabase Auth las gestiona.
- [ ] **F4.33** Si se implementa OAuth (Google/GitHub): validar `state`, el callback y las URLs de redirección.

### 4.5 Rate limiting y endurecimiento

- [x] **F4.34** Límite de peticiones en `login`, `register`, `forgot-password`, `reset-password`. (4.35 es el complemento en el borde).
      → **HECHO en el backend (21/10/2026).** `Coffee/Configuration/RateLimitFilter.kt` + `RateLimitProperties.kt` +
      `RateLimitConfiguration.kt`, con test propio (`RateLimitFilterTest`, 8 casos).
      - Filtro de servlet sobre `/api/*`, **antes** del dispatcher: una petición rechazada no llega al controller
        ni consume una conexión de la base de datos.
      - Cubo de tokens por **(IP, regla)**, ventana fija. 429 con `ProblemDetail` + cabecera `Retry-After`.
      - Regla `auth` (5/min) para los paths de autenticación; regla `write` (30/min) para el resto de
        POST/PUT/PATCH/DELETE, **mientras la escritura del catálogo siga pública** (F5.7). GET nunca se limita.
      - **La IP del cliente llega por `X-Forwarded-For`**, que reenvía el proxy de Nitro. Sin eso, todas las
        peticiones parecerían venir del contenedor de Nitro y el límite sería global (5/min para todo el sitio).
        ⚠️ Solo es seguro porque el backend no es público (F0.1) y Caddy reescribe esa cabecera con la IP real.
        Con `trust-forwarded-for=false` se usa la direccion del socket.
- [x] **F4.35** Activar `security.rateLimiter` en `nuxt.config.ts:262` (hoy `false`) **limitado a esos endpoints**, tal como anticipa el comentario de las líneas 258-261.
      → **HECHO, con corrección (ver §0.5-H1).** El `nuxt.config.ts` real no declaraba la clave y el default (150/5min
      global) estaba activo. Ahora: `rateLimiter: false` global + 5/min en cada endpoint de autenticación, con cubos separados.
      `requestSizeLimiter: false` global también (ver §0.5-H2).
      **Extra:** el `Caddyfile` reescribe `X-Forwarded-For` con `{remote_host}`, porque el limitador cuenta por IP y sin eso
      un atacante se salta el límite mandando su propia cabecera.
      **Extra 2 (F4.34):** el proxy de Nitro reenvía ahora `X-Forwarded-For` al backend, para que el limitador del
      backend pueda distinguir a los clientes.
- [ ] **F4.36** Verificar el correo antes de habilitar operaciones que requieran cuenta verificada.
- [ ] **F4.37** Añadir handlers de `AuthenticationException` (401) y `AccessDeniedException` (403) a `ApiExceptionHandler` para que devuelvan `ProblemDetail` coherente.
      ⚠️ Los tests actuales usan `standaloneSetup` sin la cadena de filtros: **añadir Spring Security no los hará validar autorización**. Añadir `springSecurity()` a los MockMvc existentes.
- [x] **F4.38** Limitar el body de las peticiones de auth (`requestSizeLimiter` está en `false` en `nuxt.config.ts:250`; decidir si se activa solo para esas rutas).
      → **HECHO en el borde:** 16 KB en `/api/auth/**` (POST/PUT/DELETE), 12 MB en `/api/media-assets/upload`.
      *Nota:* el `requestSizeLimiter` **no estaba** en `false`: el default (2 MB / 8 MB) estaba activo globalmente (§0.5-H2).


**Definición de hecho:** `login` emite cookie `HttpOnly`; `me` devuelve el usuario; `logout` invalida en servidor; un token manipulado da 401; los 107 tests siguen verdes.

---

## 6. FASE 5 — Backend: autorización de negocio

- [x] **F5.1** Crear el modelo de roles en Kotlin (D5). Nombre distinto de `VariantMediaRole`.
      -> **HECHO**: `Coffee/Auth/Model/UserRole.kt` con `CLIENTE`, `EMPLEADO`, `ADMIN`. Vive en el paquete `Auth`,
      no junto a `Media`, para que no se confunda con `VariantMediaRole`. `UserRole.authority` produce el
      nombre de autorizacion de Spring (`ROLE_ADMIN`).
- [x] **F5.2** **No deducir el rol por el correo**, por un campo editable del perfil ni por datos enviados por el frontend.
      -> **HECHO por diseno**: el rol se lee de `profiles.role`, que esta en la base de datos y lo escribe el backend.
      No hay ninguna ruta de API que lo modifique, y el perfil no expone un setter de rol.
- [x] **F5.3** Asignar y cambiar roles **solo** mediante operaciones administrativas confiables (idealmente no editables desde la API publica).
      -> **HECHO por diseno**: no existe endpoint de escritura de roles. Se asigna por SQL o por una operacion
      administrativa que aun no existe. La tabla tiene un `CHECK` que limita los valores a los tres del enum.
- [ ] **F5.4** Verificar **propiedad de cada recurso**: un cliente solo consulta sus pedidos y direcciones.
      -> La base esta: el usuario autenticado se publica en el atributo de la peticion
      (`AuthenticatedUser.read(request)`) y los servicios pueden compararlo con el `user_id` del recurso.
- [x] **F5.5** **No confiar en un `user_id` recibido en el cuerpo, query string o encabezados arbitrarios.** La identidad procede siempre de la sesion validada.
      -> **HECHO por diseno**: la identidad sale de la fila de sesion, y la fila de sesion sale de la cookie. Ningun
      DTO de entrada tiene un campo de identidad.
- [x] **F5.6** Verificar permisos administrativos **en el servidor** (`@PreAuthorize` o equivalente).
      -> **HECHO, pero con otro mecanismo** (ver 0.5-H7): en Spring Security 7 un `SecurityContext` escrito por un filtro
      propio no sobrevive, asi que `@PreAuthorize` no puede funcionar aqui. El equivalente es `SecurityRules`: la
      comprobacion ocurre en el servidor, en el filtro, antes de que la peticion llegue al controller.
- [x] **F5.7** **Proteger la escritura del catalogo.** Hoy `POST/PUT/DELETE` de categories, products, product-variants, product-variant-prices, product-variant-media, variant-3d-components y `media-assets/upload` son publicos. Esto es prioritario, antes que las tablas de pedidos.
      -> **HECHO**: cualquier POST/PUT/PATCH/DELETE exige rol `ADMIN` (`SecurityRules.requiresAdmin`). Cubre las seis
      aggregates y la subida de media, sin tener que tocar cada controller.
- [ ] **F5.8** Validar **todos** los datos recibidos (ya hay Bean Validation; cerrar los huecos: `@Pattern` en `slug`/`sku`, `@Size` en `description`, rango de `validFrom`).
- [ ] **F5.9** **Calcular los importes finales en Kotlin**, nunca aceptar el total del navegador. (`ProductVariantPriceRequest` hoy acepta `price` del cliente: solo para rol administrativo.)
- [ ] **F5.10** Verificar en servidor inventario, descuentos, impuestos, estados de pago y transiciones permitidas de pedido.
- [ ] **F5.11** **No confiar en una notificación de pago** hasta validar su autenticidad según las instrucciones del proveedor.
- [ ] **F5.12** Evitar fugas en 404/400: `ResourceNotFoundException` incluye `resourceName` + `id`, y los servicios delatan FKs ajenos (`"Variant $id does not exist"`). Ajustar al modelo de respuestas no enumerables.

**Definición de hecho:** un usuario sin rol administrativo recibe **403** en `POST /api/v1/products` y en `PUT /api/v1/product-variant-prices/{id}`.

---

## 7. FASE 6 — Frontend: Pinia, middleware y páginas

> Rutas en **Nuxt 4**: `app/stores/`, `app/middleware/`, `app/composables/` (`srcDir: 'app/'`).

### 6.1 Base técnica

- [ ] **F6.1** Instalar `pinia` + `@pinia/nuxt` y registrar el módulo en `nuxt.config.ts`.
- [x] **F6.2** Activar **CSRF** en `nuxt-config` (`nuxt-csurf@1.6.6` **ya está en `node_modules`**, solo falta la configuración) — resuelve **B3**.
      → **HECHO.** `security.csrf` con cookie `csrf` (`httpOnly`, `sameSite=lax`, `path=/`), cabecera `csrf-token` y
      `methodsToProtect: ['POST','PUT','PATCH','DELETE']`.
      **Pendiente de decidir con F4.7:** el backend va a validar CSRF también, o se confía en el borde + `SameSite` + red interna.
      Ver §A.8.
- [x] **F6.3** Mantener `corsHandler: false` (origen único) — decisión coherente con D3.
      → **HECHO.** Y con una corrección: `corsHandler` **no** estaba en `false`, el default venía activo (§0.5-H4).


### 6.2 Store de autenticación

- [ ] **F6.4** Crear `app/stores/auth.ts` exponiendo: `user`, `roles` (permisos visibles), `initialized`, `loading`.
- [ ] **F6.5** Acciones: `initializeAuth()`, `login()`, `logout()`, `refreshUser()`.
- [ ] **F6.6** `initializeAuth()` consulta `/api/auth/me` para inicializar el estado desde la sesión validada por el backend.
- [ ] **F6.7** **No** almacenar contraseñas ni claves secretas.
- [ ] **F6.8** **No** guardar tokens de acceso o renovación en `localStorage` (el patrón BFF usa cookie `HttpOnly`).
- [ ] **F6.9** Los roles en Pinia **representan la interfaz**, no autorizan operaciones sensibles.
- [ ] **F6.10** Definir el comportamiento cuando la sesión caduca o el backend devuelve `401`/`403`.

### 6.3 Cliente HTTP centralizado

- [ ] **F6.11** Crear `app/composables/useApi.ts` (o plugin de fetch) como **único** punto de llamada al backend.
- [ ] **F6.12** Centralizar el manejo de `401` → limpiar estado + redirigir a `/login`.
- [ ] **F6.13** Centralizar el manejo de `403` → mensaje de permisos insuficientes, **sin** bucle de redirección.
- [ ] **F6.14** ⚠️ **SSR (B5):** reenviar la cookie del request entrante también en las llamadas hechas desde el servidor. Si no, el render del servidor ve "sin sesión" y el hydrate ve "con sesión".
- [ ] **F6.15** Migrar `app/pages/catalogo.vue` al cliente centralizado (hoy usa `useFetch` crudo contra `/api/categories`).

### 6.4 Middleware de ruta

- [ ] **F6.16** Crear `app/middleware/auth.ts` (ver ejemplo en el Anexo §A.3).
- [ ] **F6.17** Crear `app/middleware/guest.ts` para páginas públicas que deben redirigir si **ya** hay sesión (`/login`, `/registro`).
- [ ] **F6.18** Crear `app/middleware/admin.ts` para páginas administrativas (solo navegación; la autoridad es el backend).
- [ ] **F6.19** Aplicar `definePageMeta({ middleware: 'auth' })` a cada página privada.
- [ ] **F6.20** Documentar que el middleware **mejora la navegación pero no autoriza**. Las llamadas directas a endpoints deben estar protegidas aunque el usuario omita el middleware.

### 6.5 Páginas y navegación

- [ ] **F6.21** `app/pages/login.vue`
- [ ] **F6.22** `app/pages/registro.vue`
- [ ] **F6.23** `app/pages/recuperar.vue` (`forgot-password`)
- [ ] **F6.24** `app/pages/reset-password.vue` (token válido, SSR-compatible)
- [ ] **F6.25** `app/pages/cuenta/` — perfil
- [ ] **F6.26** `app/pages/pedidos/` — solo si D6 incluye `orders` en esta iteración
- [ ] **F6.27** `app/pages/checkout/` — solo si D6 incluye pedidos
- [ ] **F6.28** `app/pages/admin/` — solo si D7 lo aprueba
- [ ] **F6.29** **No** crear `layouts/` hasta que haya más de una página con cromo común.
- [ ] **F6.30** Añadir a la navegación de `app/app.vue:4-7` el enlace "Iniciar sesión" / "Cerrar sesión" / "Mi cuenta" según el estado del store.
- [ ] **F6.31** ⚠️ **SVG (deuda heredada de `CONTEXT.md:166`):** decidir si el storefront sirve SVG inline. Si sí, sanear o rasterizar antes. Es contenido activo.

**Definición de hecho:** sin sesión, `/cuenta` redirige a `/login`; con sesión, `/login` redirige a la casa; el estado sobrevive a un refresco F5 en SSR.

---

## 8. FASE 7 — RLS y permisos de datos

> ⚠️ **Leer D2 antes de empezar esta fase.** La conexión actual (JDBC con `postgres.<ref>`) **bypassa RLS**:
> el JWT HTTP no establece `auth.uid()` y el owner del esquema está exento de RLS.
> **No escribir políticas definitivas hasta cerrar D2.**

### 7.1 Modelo de datos

- [x] **F7.1** Migracion **`V7__crear_profiles.sql`** (siguiente libre; **no tocar V1-V6**).
      -> **HECHO como `V8__crear_profiles_y_sesiones.sql`**: `profiles` + `app_sessions` (V7 ya la ocupaba la limpieza de
      la tabla `cafe`). `profiles.user_id` referencia `auth.users(id)` con `ON DELETE CASCADE`.
      **`token_hash` es VARCHAR(64) y no CHAR(64)**: Hibernate valida el esquema con `ddl-auto=validate` y un CHAR no
      es un VARCHAR (el mismo caso que arreglo V3 con `currency`).
- [x] **F7.2** `profiles.user_id` -> `REFERENCES auth.users(id)` (solo si se va a usar `auth.users` de Supabase).
      -> **HECHO**, con `ON DELETE CASCADE`: borrar el usuario en Supabase se lleva su perfil y sus sesiones.
- [ ] **F7.3** `addresses` con `user_id` + `CHECK`/`FK` de integridad.
- [ ] **F7.4** `orders` y `order_items` solo si D6 los incluye. `price NUMERIC(12,2)`, **nunca FLOAT** (regla ya escrita en `CONTEXT.md:217`).
- [ ] **F7.5** Restricciones: `CHECK` en estados, unicidad, `NOT NULL`, FKs.

### 7.2 RLS

- [ ] **F7.6** Decidir el **mecanismo de contexto de identidad** según D2 (PostgREST con JWT, `SET LOCAL role`, o capa explícita en Kotlin).
- [ ] **F7.7** `ALTER TABLE ... ENABLE ROW LEVEL SECURITY` en cada tabla expuesta que deba protegerse.
- [ ] **F7.8** `GRANT SELECT/INSERT/UPDATE/DELETE` **mínimos**. No conceder `UPDATE` amplios si el cliente no necesita tocar esos campos.
- [ ] **F7.9** Política `SELECT` de pedidos limitada a `user_id = (SELECT auth.uid())` (ver Anexo §A.4).
- [ ] **F7.10** **Decidir las reglas de negocio antes** de habilitar `INSERT`/`UPDATE`/`DELETE`:
      - [ ] ¿Quién puede crear pedidos?
      - [ ] ¿Puede un cliente modificar un pedido **después** de pagarlo?
      - [ ] ¿Qué campos son modificables por el cliente?
      - [ ] ¿Qué cambios de estado son exclusivos del backend?
      - [ ] ¿Cómo se evita que un cliente se asigne pedidos de otra persona?
- [ ] **F7.11** Restringir operaciones administrativas y cambios de estado a un rol de servidor.
- [ ] **F7.12** Verificar que las consultas se ejecutan con el **contexto de identidad esperado**, no solo como owner.
- [ ] **F7.13** Si se usa un rol privilegiado que omite RLS, **documentarlo explícitamente** y no contar RLS como capa activa para esa conexión.

### 7.3 Catálogo

- [ ] **F7.14** `GET /api/v1/catalog` y la lectura de categorías siguen siendo públicos (lectura de productos **publicados/activos**).
      ⚠️ Hoy cualquiera puede hacer `PUT /api/v1/categories/{id}` con `active: false` → el catálogo desaparece. Cubierto por F5.7.

**Definición de hecho:** `RLS` probado con **dos usuarios distintos**, nunca solo con el owner de la base de datos.

---

## 9. FASE 8 — Pruebas de seguridad

> ⚠️ Los tests actuales usan `MockMvcBuilders.standaloneSetup(...)`, que **no carga la cadena de filtros**.
> Añadir Spring Security no los rompe, pero tampoco valida nada de autorización → riesgo de "todo verde" falso.

### 8.1 Infraestructura de tests

- [ ] **F8.1** Anadir `springSecurity()` a los MockMvc existentes (`CategoryControllerTest`, `MediaAssetControllerTest`, `ProductVariantMediaControllerTest`).
      -> **DECISION:** esos tests usan `standaloneSetup` y comprueban mapeo, validacion y errores de negocio, no
      autorizacion. Convertirlos todos a `@WebMvcTest` es una reescritura grande sin valor. La autorizacion se
      prueba en un sitio: `SecurityBoundaryTest`, con la **cadena de filtros real**. Lo que si se ha hecho:
      los tests de standalone siguen funcionando **con Spring Security presente** (no se rompen), que es lo que
      pedia el aviso de la F8.
- [x] **F8.2** Anadir el **primer** `@SpringBootTest` / `@WebMvcTest` con contexto real.
      -> **HECHO**: `SecurityBoundaryTest` (9 casos) con `@WebMvcTest` + `@AutoConfigureMockMvc` + la cadena real.
      Cubre: catalogo publico, lectura de categorias publica, 401 anonimo en lectura y en escritura, 403 sin rol
      `ADMIN`, 201 con `ADMIN`, lectura protegida con sesion, sesion invalida sin acceso y 403 sin token CSRF.
- [x] **F8.3** Anadir `spring-security-test` a `build.gradle.kts`.
      -> **HECHO**. Ademas `spring-boot-starter-webmvc-test` (el `@WebMvcTest` de Boot 4 esta en otro modulo) y
      `mockito-kotlin` (ver 0.5-H8).
- [ ] **F8.4** Frontend: instalar `vitest` + `@nuxt/test-utils` y añadir script `test`. Hoy hay **0 tests**.
      → 🔀 **Aplazado desde F3.6 (21/10/2026).** Se hace aquí, cuando el BFF exista y haya casos reales que proteger.
      La infraestructura (`vitest@^4`, `@nuxt/test-utils@^4`, `happy-dom`) es compatible con Nuxt 4.5.2 y h3 v1
      (`@nuxt/test-utils` 4.3.4 admite `vitest ^4 || ^5`, `nuxt ^4`, `h3 ^1.15`).
      **Y el primer test que hay que escribir es el del proxy** (F3.6): cookie entrante → llega a Kotlin, y
      `Set-Cookie` de Kotlin → llega al navegador. Ese test es el que protege B1/B2 de una regresión.

### 8.2 Casos obligatorios

- [ ] **F8.5** Usuario **A** no puede consultar pedidos del usuario **B**.
- [x] **F8.6** Usuario **no autenticado** recibe **401** en endpoints privados.
      -> **HECHO**: `SecurityBoundaryTest` + `SessionAuthenticationFilterTest`.
- [x] **F8.7** Usuario **autenticado sin permisos** recibe **403**.
      -> **HECHO**: un `CLIENTE` que intenta escribir el catalogo recibe 403 con `ProblemDetail`.
- [ ] **F8.8** Un usuario **no puede cambiar su propio rol** manipulando la petición.
- [ ] **F8.9** Un usuario **no puede modificar el total** de un pedido desde el navegador.
- [ ] **F8.10** Un usuario **no puede confirmar un pago** desde el navegador.
- [ ] **F8.11** Un token **expirado** es rechazado.
- [ ] **F8.12** Un token **mal firmado** es rechazado.
- [ ] **F8.13** Un token con **emisor/audiencia incorrectos** es rechazado.
- [ ] **F8.14** La sesión se **invalida en servidor** al hacer logout (la cookie sola no basta).
- [ ] **F8.15** `forgot-password` **no revela** si el correo existe.
- [ ] **F8.16** **RLS** probada con dos usuarios distintos, no solo con el owner.
- [ ] **F8.17** El bundle del frontend **no expone** claves privilegiadas (`sb_secret_`, `service_role`, `DB_PASSWORD`).
- [ ] **F8.18** El proxy reenvía la cookie **y** el `Set-Cookie` (regresión de B1/B2).
- [ ] **F8.19** **Rate limit** activo: superar el límite devuelve 429 en `login`.

**Definición de hecho:** los 9 casos del plan original más B1/B2, rate limit y enumeración de correos, todos en verde y **verificados en CI**.

---

## 10. FASE 9 — Secretos, configuración y operación

- [x] **F9.1** Verificar que ningún secreto está en `NUXT_PUBLIC_*`.
      → **HECHO.** La única variable pública es `NUXT_PUBLIC_SITE_URL`. `SUPABASE_ANON_KEY` va a `runtimeConfig` **privado**
      del backend. `apiBase` sigue privado.
- [x] **F9.2** Verificar `.gitignore` / `.dockerignore` siguen cubriendo secretos (ya lo hacen: re-verificar tras los cambios de F2).
      → **HECHO y re-verificado** tras los cambios de esta fase: `.env` y `.env.*` siguen ignorados, con las excepciones
      `!.env.example` y `!*.env.example`. `Docker/.env` no se ha tocado ni se ha versionado.
- [ ] **F9.3** Confirmar **HTTPS en producción** (Caddy `tls internal` ya está; validar el resto).
- [x] **F9.4** CORS solo con orígenes necesarios — o **nada**, manteniendo el proxy de origen único (recomendado).
      → **HECHO.** `corsHandler: false` (F6.3) y el backend no expone ningún `@CrossOrigin`. El puerto 8080 ya no está
      publicado (F0.1), así que no hay un segundo origen desde el que reachar la API.
- [ ] **F9.5** Confirmar protección **CSRF** activa con las cookies de sesión (F3 + F6.2).
      → 🟡 **El borde SÍ tiene CSRF activo** (F6.2). Falta decidir y aplicar la parte del backend (F4.7 / §A.8).
- [ ] **F9.6** Confirmar límites de peticiones en autenticación y endpoints sensibles.
      → 🟡 **El borde SÍ tiene límites** (5/min y 16 KB en autenticación, 12 MB en subida de media). Falta el backend (F4.34).

- [ ] **F9.7** **No** registrar cookies, contraseñas, tokens completos ni datos financieros.
      Revisar el `log.error(..., exception)` de `MediaStorageException` en `ApiExceptionHandler.kt:87`: el `cause` del proveedor podría incluir fragmentos de su respuesta.
- [ ] **F9.8** Añadir **logging de auditoría**: request-id, usuario, método, ruta, status, resultado de autorización. Sin datos sensibles.
- [ ] **F9.9** Definir **retención de logs** y política de acceso a los entornos.
- [ ] **F9.10** Revisar **copias de seguridad** de PostgreSQL.
- [ ] **F9.11** Definir qué hacer al **suspender una cuenta**: invalidar sesiones, cached permisos, RLS.

---

## 11. Orden de ejecución (resumen)

```
F0  Perímetro de red          ── rapido, bloqueante, antes de tocar nada
F1  Decisiones D1..D7         ── respondalas TODAS antes de escribir codigo
F2  Supabase Auth (panel)     ── depende de F1
F3  Proxy Nitro (B1, B2)      ── precondicion del BFF, independiente de F2
F4  Backend: autenticacion    ── depende de F1(F2) y F3
F5  Backend: autorizacion     ── depende de F4
F6  Frontend: Pinia/mw/paginas── depende de F4
F7  RLS                       ── BLOQUEADO por D2
F8  Tests                     ── transversal, se acumula desde F4
F9  Secretos y operacion      ── transversal
```

**Regla de oro:** F0 → F1 → (F2 ∥ F3) → F4 → (F5 ∥ F6) → F7 → F8 → F9.
Si algo se bloquea, **no lo rodees**: vuelve a F1 y ajusta la decisión.

### Estado a 22/10/2026

```
F0  Perímetro de red          ── ✅ HECHO (falta F0.4: comprobación manual con Docker)
F1  Decisiones D1..D7         ── 📌 SIGUIENTE PASO (7 propuestas escritas, sin confirmar)
F2  Supabase Auth (panel)     ── 🟡 cableado de variables hecho; falta el panel
F3  Proxy Nitro (B1, B2)      ── ✅ HECHO (B1, B2, B3 resueltos; F3.6 → F8)
F4  Backend: autenticacion    ── 🟡 infraestructura hecha; faltan los endpoints /auth (dependen de F2)
F5  Backend: autorizacion     ── 🟡 escritura protegida; falta la regla por recurso
F6  Frontend: Pinia/mw/paginas── ⬜ BLOQUEADO por F4 (base técnica: CSRF y CORS ya)
F7  RLS                       ── 🟡 tablas hechas; las politicas dependen de D2
F8  Tests                     ── 🟡 155 tests; la frontera de seguridad ya se prueba
F9  Secretos y operacion      ── 🟡 F9.1, F9.2, F9.4 hechos
```

**Lo que bloquea el proyecto ahora mismo es F1 + F2.** Las 7 decisiones tienen una propuesta escrita arriba
(D1, D2, D3, D4, D5, D6 y D7); confirmarlas (o corregirlas) desbloquea F4, F5, F6 y F7. Y F2 (el panel de Supabase
escrita arriba; confirmarlas (o corregirlas) es lo que desbloquea F4, F5, F6 y F7.

---

## ANEXO — Material de referencia

> Esto no es trabajo pendiente: es el contexto que ya estaba en el plan original y que se necesita
> mientras se ejecuta la checklist.

### A.1 Arquitectura objetivo

| Componente | Responsabilidad | **NO** es responsable de |
|---|---|---|
| **Nuxt 4 + Vue 3** | Formularios de login/registro/recuperación, navegación, route middleware, cliente HTTP | **No** es la autoridad final para conceder permisos |
| **Pinia** | Estado de auth en memoria para la UI: `user`, roles visibles, `loading`, `initialized` | **No** guarda contraseñas, secretos ni tokens; **no** prueba autorización |
| **Backend Kotlin** | Autoridad de negocio. Endpoints de auth, sesión, verificación de identidad en cada request privado, autorización por recurso, validación, **cálculo de precios**, secretos, logging de eventos | — |
| **Supabase Auth** | Credenciales, verificación de correo, recuperación, emisión/validación de tokens de identidad | — |
| **Supabase PostgreSQL** | Datos, permisos SQL, integridad, RLS (capa extra, **si el contexto de identidad está bien establecido**) | — |

### A.2 Flujo BFF con cookie segura

1. El navegador envía credenciales a Kotlin **por HTTPS** (a través del proxy Nitro).
2. Kotlin se comunica con Supabase Auth.
3. Kotlin establece una sesión de aplicación y devuelve una **cookie segura** (`Set-Cookie`).
4. El navegador la envía automáticamente en cada petición.
5. Kotlin valida la sesión y obtiene la identidad.
6. Kotlin comprueba la **autorización** antes de ejecutar la operación.
7. El backend consulta o modifica PostgreSQL.
8. RLS aplica restricciones adicionales **si** hay contexto de identidad válido en la consulta.

**Requisitos de cookie/sesión** (cada uno tiene su casilla en F4.3): HTTPS en producción ·
`HttpOnly` + `Secure` + `SameSite` · nada de tokens de renovación en JS · protección CSRF ·
expiración y renovación · logout e invalidación · comportamiento ante revocación/suspensión ·
**no asumir que borrar la cookie invalida los tokens de Supabase**.

### A.3 Route middleware (ejemplo conceptual)

```ts
// app/middleware/auth.ts
export default defineNuxtRouteMiddleware(async () => {
  const auth = useAuthStore()

  if (!auth.initialized) {
    await auth.initializeAuth()   // consulta /api/auth/me
  }

  if (!auth.user) {
    return navigateTo('/login')
  }
})
```

```ts
// en una página privada
definePageMeta({ middleware: 'auth' })
```

> Este middleware mejora la navegación. **No reemplaza la autorización del backend.** Las llamadas
> directas a endpoints deben estar protegidas aunque el usuario omita el middleware. Con SSR, el
> estado debe resolverse de forma compatible con SSR **y** con cookies: no depender solo de una
> sesión guardada en el navegador.

### A.4 Ejemplo ilustrativo de RLS (solo válido bajo D2.b)

```sql
CREATE TABLE public.orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id),
    total NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    status TEXT NOT NULL DEFAULT 'pending',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON public.orders TO authenticated;

CREATE POLICY "Users can read their own orders"
ON public.orders
FOR SELECT
TO authenticated
USING (
    user_id = (SELECT auth.uid())
);
```

> Solo habilita **lectura**. No habilita automáticamente `INSERT`, `UPDATE` ni `DELETE`.
> **No aplicar esto mientras la conexión sea JDBC con usuario owner (D2.a):** `auth.uid()` no se
> establece y RLS no protegería esa conexión.

### A.5 Conexión Kotlin–PostgreSQL (por qué D2 bloquea F7)

| Escenario | ¿Funciona RLS? |
|---|---|
| **A. API REST / PostgREST de Supabase**, reenviando el JWT del usuario + clave publicable | Sí, Supabase puede aplicar RLS con la identidad de ese usuario. **No** usar clave privilegiada para lo que se espera proteger. |
| **B. JDBC/R2DBC directo** ← **este es el caso actual** | **No.** El JWT HTTP no establece `auth.uid()` en la sesión SQL. Si todas las peticiones usan el mismo usuario de BD, RLS no distingue a cada usuario. **Y si ese usuario es el owner (`postgres.<ref>`), además bypassa RLS.** |

**Consecuencia para hoy:** la autorización **tiene que vivir en Kotlin**. Para que RLS sea una capa
real hace falta un mecanismo explícito de contexto de identidad (D2.a/b/c).

### A.6 Protección por recurso

| Recurso | Protección requerida |
|---|---|
| Catálogo | Solo productos publicados; **escritura restringida a administración** (hoy pública → F5.7) |
| Perfil | Cada cliente accede únicamente a su perfil |
| Direcciones | Cada cliente accede a sus direcciones |
| Pedidos | Cada cliente consulta sus pedidos; el backend controla totales y estados |
| Inventario | Modificación solo mediante operaciones autorizadas del servidor |
| Administración | Roles y permisos verificados por Kotlin |
| Pagos | Verificar notificaciones del proveedor y calcular estados en el servidor |
| Datos sensibles | Reducir campos expuestos y aplicar permisos mínimos |
| Media | Hoy `POST /media-assets/upload` y `DELETE /media-assets/{id}` son **públicos** → F5.7 |

### A.7 Principio rector

La seguridad es **acumulativa**:

- **Nuxt middleware:** navegación y experiencia de usuario.
- **Pinia:** estado de interfaz.
- **Kotlin:** autenticación de sesión, autorización y reglas de negocio.
- **Supabase Auth:** identidad y credenciales.
- **PostgreSQL/RLS:** defensa a nivel de filas, cuando el contexto de identidad esté correctamente establecido.
- **Restricciones SQL:** integridad de datos.

> **Ninguna decisión tomada en el navegador debe ser suficiente para conceder acceso a datos
> sensibles o ejecutar una operación crítica.**

### A.8 CSRF: ¿una capa o dos? (decisión abierta de F4.7)

> **Estado: ABIERTA.** Hay que resolverla con D3, antes de escribir el `SecurityConfig`.

El borde (Nitro) ya tiene CSRF: `nuxt-csurf` pone la cookie `csrf` y exige la cabecera `csrf-token` en
POST/PUT/PATCH/DELETE (F6.2, B3). La pregunta es si **Kotlin** también valida CSRF.

| Opción | Cómo funciona | A favor | En contra |
|---|---|---|---|
| **1. Solo el borde** | Kotlin no valida CSRF. La defensa es: cookie `SameSite=Lax` + el backend no es alcanzable desde fuera (F0.1) + el borde rechaza el POST sin token. | Un solo sistema de tokens. El navegador solo tiene que conocer un token. | Si algún día el backend es alcanzable por otra ruta (otra red, un servicio interno, un `kubectl port-forward`), el CSRF desaparece. |
| **2. Las dos capas** | Kotlin tiene su propio token CSRF (Spring Security), distinto del del borde. El navegador manda las dos cabeceras. | Defensa en profundidad: cada capa valida por su cuenta. | Dos sistemas de tokens en el navegador, dos cookies (`csrf` y `XSRF-TOKEN`), y el token de Spring tiene que llegar al navegador (cosa que hoy **no** hace el proxy: solo propaga `set-cookie`, así que habría que añadirlo). |

**Lo que hace el plan original:** F4.7 dice "Desactivar CSRF **solo** si se confirma autenticación por
cabecera; con cookie (D3), **CSRF activo**". Con D3 = cookie, la lectura literal es la **opción 2**.

**Recomendación (a confirmar):** **opción 2**, por una razón concreta: el proxy de Nitro **no** es un
frontera de confianza con futuro. Hoy es el único camino, pero en cuanto haya N réplicas de Kotlin, un
servicio de pagos que llame a Kotlin, o un `port-forward` de depuración, la protección se queda solo en el
borde. Y el coste de la opción 2 es bajo porque el proxy ya reenvía `Set-Cookie` (B2): la cookie
`XSRF-TOKEN` de Spring llegaría al navegador sin tocar el proxy, y el token de Spring se puede hacer
**compatible** con el del borde usando el mismo nombre de cabecera (`csrf-token`) y el mismo repositorio
de cookies, de modo que el navegador no tenga que manejar dos tokens distintos.

⚠️ **Requisito que esto impone:** el `SecurityConfig` de Kotlin tiene que leer el token de la **cookie**
(como `CookieCsrfTokenRepository` de Spring) y no generar uno por sesión, porque la cookie es lo que
viaja en el navegador. Y el proxy tiene que reenviar la cookie de CSRF de Spring al backend: la petición
del navegador al proxy lleva `cookie: csrf=...; XSRF-TOKEN=...`, y el proxy ya reenvía la cabecera
`cookie` completa (F3.1). ✅ **No hace falta tocar el proxy para esto.**

