# Plan de seguridad — CHECKLIST

## E-commerce con Kotlin + Nuxt + Supabase

> **Cómo usar este documento.** Todo lo accionable está en `- [ ]` / `- [x]`. Las cajas marcadas `[x]` ya
> están hechas (auditoría del codebase). Las marcadas `[ ]` están pendientes y están **ordenadas por
> dependencia**: no saltes de fase, cada una se apoya en la anterior. El material puramente
> explicativo (código de ejemplo, tablas de referencia) está en el **Anexo**, al final.
>
> **Regla del proyecto:** *nunca modificar una migración ya aplicada* (Flyway reporta error de
> checksum). La siguiente migración libre es **`V7`**.

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

### 0.2 Lo que NO existe (hay que construir desde cero)

| # | Ausencia | Impacto |
|---|---|---|
| 1 | `spring-boot-starter-security` | No hay `SecurityFilterChain`, no hay `SecurityConfig`. Base a añadir. |
| 2 | Autenticación en cualquier forma | 0 filtros, 0 JWT, 0 JWKS, 0 sesiones, 0 cookies. Los 7 endpoints `/api/v1/auth/*` **no existen**. |
| 3 | Autorización | 0 `@PreAuthorize` / `@Secured`. **36 endpoints, 0 protegidos**. `DELETE /products/{id}` y `PUT /product-variant-prices/{id}` son públicos. |
| 4 | CSRF | 0. Irrelevante hoy; **obligatorio** en cuanto haya cookie de sesión. |
| 5 | Rate limiting | 0 en backend y `rateLimiter: false` en `nuxt.config.ts:262`. Sin protección contra fuerza bruta. |
| 6 | Modelo de roles | 0 enums de rol, 0 tabla de roles, **0 columna `user_id`/`owner_id`** en entidades o migraciones. |
| 7 | RLS | 0 `ENABLE ROW LEVEL SECURITY`, 0 `CREATE POLICY`, 0 `GRANT`/`REVOKE`. |
| 8 | Contexto de identidad en BD | **Imposible hoy**: JDBC directo con `postgres.<ref>` (owner) vía pooler → el JWT HTTP no establece `auth.uid()` y el owner **bypassa** RLS. Es el caso "B" del §9 del plan original. |
| 9 | `pinia` / `@pinia/nuxt` | No instalados. No existe `app/stores/`. |
| 10 | Middleware de ruta | No existe `app/middleware/`. `MiddlewareKey = never`. Sin protección de navegación. |
| 11 | Cliente HTTP centralizado | 0 interceptores, 0 manejo de 401/403. Solo un `useFetch` crudo. |
| 12 | Páginas de auth | No existen `/login`, `/registro`, `/recuperar`, `/reset-password`, `/cuenta`, `/pedidos`, `/admin`, `/checkout`, `/carrito`. |
| 13 | Tablas privadas | **No existen** `profiles`, `addresses`, `orders`, `order_items`. |
| 14 | Tests de seguridad | 0 tests de 401/403, 0 de aislamiento entre usuarios, 0 de token inválido. Además los tests usan `standaloneSetup`, así que **añadir Spring Security no los romperá ni los hará validar autorización**. |
| 15 | Tests frontend | 0 tests, sin `tsconfig.json`, sin `typecheck`, sin lint. |
| 16 | CORS | 0 `@CrossOrigin`. Hoy no hace falta por el proxy de origen único, **pero `docker-compose.yml:183` publica `:8080` al host**. |

### 0.3 Bloqueantes técnicos concretos del patrón BFF

- [ ] 🔴 **B1** — `server/api/[...path].ts:60-63` **descarta la cabecera `cookie`** al llamar a Kotlin. Una sesión por cookie `HttpOnly` nunca llegaría al backend.
- [ ] 🔴 **B2** — `server/api/[...path].ts:72-73` **no reenvía `Set-Cookie`** de Kotlin al navegador. El paso "Kotlin establece la cookie de sesión" es imposible tal cual.
- [ ] 🔴 **B3** — `nuxt-security` no tiene `csrf` configurado (default `false`), así que **`nuxt-csurf` no se registra**. Con cookies de sesión esto es obligatorio. La librería ya está en `node_modules`.
- [ ] 🔴 **B4** — `rateLimiter: false` en `nuxt.config.ts:262`, con un comentario que dice explícitamente que se configurará "en la fase de autenticación". Deuda ya identificada.
- [ ] 🔴 **B5** — SSR activo: el estado de sesión debe resolverse **por petición en el servidor**, y las llamadas `useFetch` a `/api/**` durante el render **también** deben llevar la cookie, o el render del servidor y el hydrate discreparán.
- [ ] 🟡 **B6** — `connect-src` de la CSP solo permite `'self'` + `res.cloudinary.com`. Cualquier llamada del navegador a `*.supabase.co` quedaría **bloqueada**. Esto favorece el BFF, pero hay que decidirlo explícitamente.
- [ ] 🟡 **B7** — El servicio `frontend` de producción **no recibe `NUXT_API_BASE`** (`docker-compose.yml:27-30`) y usa el default hardcodeado. Funciona en la red de Compose, pero contradice el `.env.example`.
- [ ] 🟡 **B8** — No existe forma de pasar secretos de Supabase al contenedor `backend`: `Docker/.env.example` no define ninguna variable `SUPABASE_*`.

### 0.4 Deuda menor que conviene arreglar antes de empezar

- [ ] 🟡 Hueco en el versionado de migraciones: existen `V1,V2,V3,V4,V6`. Falta `V5` (`V1__crear_tabla_cafe.sql` sigue aplicada). La siguiente libre es **`V7`**.
- [ ] 🟡 `MEDIA_LOCAL_*` existe en `application.properties:58-60` pero no está cableado en `docker-compose.yml`, ni en `.env`/`.env.example`, ni en el `Caddyfile`. `LocalMediaStorage` es inalcanzable en Docker.
- [ ] 🟡 `Docker/ARCHITECTURE.md` tiene secciones "pendiente" ya obsoletas (frontend ya decidido; CORS ya no es "no hay frontend en otro origen" sino "el puerto 8080 está publicado").
- [ ] 🟡 `Frontend/env.local.example` se llama así (sin punto inicial); el comentario del `.gitignore` que lo menciona como `.env.local.example` no corresponde al archivo real.

---

## 1. FASE 0 — Cerrar el perímetro de red (rápido, bloqueante)

> **Objetivo:** que hoy no exista ninguna ruta pública y directa al backend sin pasar por el proxy.

- [ ] **F0.1** Dejar de publicar el puerto del backend al host.
      En `Docker/docker-compose.yml:183-184`, quitar `ports: - "${BACKEND_PORT:-8080}:8080"` del servicio `backend`.
      Solo debe quedar `expose:` para la red interna de Compose.
- [ ] **F0.2** Comprobar que `migrate` (Job de Flyway) sigue teniendo acceso a la red tras el cambio de F0.1. Si se rompe, darle su propio `expose`.
- [ ] **F0.3** Restringir `/actuator/*`. Hoy `application.properties:75-79` expone `health,info,metrics` sin restricción.
      Bajar a `health` solamente, o exigir rol para `info`/`metrics`.
- [ ] **F0.4** Verificar en red que el catálogo sigue funcionando end-to-end: `Caddy → /api/* → Nitro → :8080`.
- [ ] **F0.5** Correr el hueco `V5` de migraciones (borrar la tabla `cafe` residual) para no arrastrarlo al schema de auth.

**Definición de hecho:** con `curl` directo a `:8080` desde el host **no se obtiene respuesta**, y `GET /api/v1/catalog` a través de Caddy sigue devolviendo `200`.

---

## 2. FASE 1 — Decisiones bloqueantes (responder ANTES de escribir código)

> **No avances a la Fase 2 sin cerrar esto.** Cada respuesta cambia el diseño de las fases siguientes.

- [ ] **D1 — Framework Kotlin:** Spring Boot 4.1.1 + Spring Security.
      ⚠️ **Ojo:** Spring Boot 4 usa **Jackson 3** (`tools.jackson.*`). Verificar compatibilidad de Spring Security 7 con Jackson 3 y con Kotlin 2.3 antes de añadir el starter.
- [ ] **D2 — Conexión a Supabase:** hoy es **JDBC directo** (Spring Data JPA → HikariCP → pooler) con un único usuario `postgres.<ref>`.
      **Esta es la decisión más importante del plan.** Elegir una de estas tres:
      - [ ] **D2.a** Mantener JDBC + **autorización solo en Kotlin** + RLS como capa extra vía mecanismo de contexto en BD (recomendado: es el camino más simple y no toca el pooler).
      - [ ] **D2.b** Migrar a **PostgREST / API de datos de Supabase** reenviando el JWT del usuario para que RLS funcione nativo con `auth.uid()`.
      - [ ] **D2.c** JDBC + `SET LOCAL role` / `request.jwt.claims` por transacción para propagar identidad a RLS (más complejo, requiere pool dedicado y serialización).
- [ ] **D3 — Sesión:** **BFF con cookie `HttpOnly`** (recomendado, encaja con el proxy de origen único ya existente y con `corsHandler: false`).
      Descartar JWT en `localStorage`.
- [ ] **D4 — SSR:** ✅ **confirmado = sí** (Nuxt 4.5.2, `preset: node-server`, `app/pages/catalogo.vue` hace `await useFetch` en el setup).
      Implica: resolver la sesión **por petición en el servidor**.
- [ ] **D5 — Roles necesarios.** Proponer y fijar el modelo. Mínimo sugerido: `CLIENTE`, `EMPLEADO`, `ADMIN`.
      ⚠️ **No confundir con `VariantMediaRole`**, que es un enum de media, no de usuario.
- [ ] **D6 — Tablas privadas.** Hoy **no existe ninguna**. Definir cuáles se crean en esta iteración y en qué orden:
      `profiles` → `addresses` → `orders` → `order_items`.
      Si `orders` no entra en esta iteración, **D6 se reduce a `profiles` + `addresses`**.
- [ ] **D7 — ¿Entra el panel de administración en esta iteración?** Si no, la escritura del catálogo queda **cerrada** y se desarrolla en una iteración posterior.
      ⚠️ Mientras no haya roles, cualquier escritura pública es un agujero. Si se aplaza el admin, **hay que despublicar la escritura del catálogo ya en la Fase 2**.

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
- [ ] **F2.7** Añadir `SUPABASE_URL` y `SUPABASE_ANON_KEY` a `Docker/.env.example` (y los valores reales a `Docker/.env`).
- [ ] **F2.8** Añadirlas al `environment` del servicio `backend` en `Docker/docker-compose.yml`.
      ⚠️ **Nunca** al servicio `frontend`. El navegador no debe hablar con Supabase (B6: la CSP lo bloquearía).
- [ ] **F2.9** Verificar con `grep -ri "sb_secret\|service_role" Frontend/` → **0 coincidencias** en source **y** en `.output/public/_nuxt/*.js`.
- [ ] **F2.10** Guardar la clave secreta en el gestor de secretos del entorno. **Nunca** en `NUXT_PUBLIC_*`.

**Definición de hecho:** `SUPABASE_URL` + `SUPABASE_ANON_KEY` llegan al contenedor `backend` y no al `frontend`; 0 coincidencias de claves privilegiadas en el bundle del navegador.

---

## 4. FASE 3 — Arreglar el proxy Nitro (precondición del BFF)

> **Objetivo:** que una cookie pueda ir y volver entre navegador y Kotlin. Sin esto, ninguna Fase posterior funciona.
> **Antes de empezar:** resolver **B1** y **B2**.

- [ ] **F3.1 — Reenviar la cookie entrante (B1).** En `server/api/[...path].ts:60-63`, añadir la cabecera `cookie` (usar `getRequestProxyHeaders(event, { host: false })` o leer `cookie` explícitamente).
      **No** reenviar cabeceras arbitrarias: solo las que hacen falta (`cookie`, `authorization` si se decide, `x-csrf-token`).
- [ ] **F3.2 — Reenviar `Set-Cookie` (B2).** En el bloque de respuesta (líneas 70-75), propagar **todas** las cookies con `response.headers.getSetCookie()` → `appendResponseHeader(event, 'set-cookie', ...)`.
      ⚠️ Cuidado: hoy solo se copia `content-type`. Una sola cookie devorada rompe toda la sesión.
- [ ] **F3.3 — Preservar `redirect` / `location`** de la respuesta de Kotlin (relevante para el flujo de verificación de correo y reset de contraseña).
- [ ] **F3.4 — Propagar cabeceras de seguridad relevantes** (`location`, `cache-control: no-store` en respuestas de auth).
- [ ] **F3.5** Añadir `frontend/tsconfig.json` y el script `typecheck` en `package.json`. **Sin esto no hay forma de validar código de auth sensible.**
- [ ] **F3.6** Escribir el **primer test del proxy**: cookie entrante → llega a Kotlin; `Set-Cookie` de Kotlin → llega al navegador. Este test es el que evita la regresión de B1/B2.

**Definición de hecho:** test verde que demuestra que una cookie `HttpOnly` hace el viaje completo navegador → Nitro → Kotlin → Nitro → navegador.

---

## 5. FASE 4 — Backend: autenticación

> **Objetivo:** que Kotlin sea la autoridad de identidad. Nada de esto existe todavía.

### 4.1 Dependencias y configuración base

- [ ] **F4.1** Añadir `implementation("org.springframework.boot:spring-boot-starter-security")` a `Backend/build.gradle.kts`.
- [ ] **F4.2** Añadir un cliente de verificación JWT por **JWKS** (nunca solo decodificar).
      Verificar compatibilidad con Jackson 3 / Spring Boot 4 antes de fijar la librería.
- [ ] **F4.3** Crear `Coffee/Configuration/SecurityConfig.kt` con el `SecurityFilterChain`.
- [ ] **F4.4** Crear `Coffee/Configuration/CorsConfig.kt` **solo si D3 lo exige**. Con origen único no debería hacer falta; si se añade, orígenes explícitos, **nunca comodín**.
- [ ] **F4.5** Declarar las rutas públicas: `GET /api/v1/catalog`, `GET /api/v1/categories/**` lectura, `POST|PUT|DELETE /api/v1/auth/**`, `/actuator/health`.
- [ ] **F4.6** **Todo lo demás cerrado por defecto** (`anyRequest().authenticated()`). La lista de permitidos es explícita y corta, no por exclusión.
- [ ] **F4.7** Desactivar CSRF **solo** si se confirma autenticación por cabecera; con cookie (D3), **CSRF activo**.

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

- [ ] **F4.16** Crear el almacenamiento de sesión de servidor (tabla `app_sessions` en `V7__*`, o equivalente). **No** `HttpSession` en memoria: la app es stateless y corre con N réplicas.
- [ ] **F4.17** Vincular la sesión de aplicación con la identidad **autenticada** de Supabase.
- [ ] **F4.18** Emitir la cookie: `HttpOnly`, `Secure`, `SameSite=Lax` (o `Strict`), `Path=/`, `Max-Age` definido.
- [ ] **F4.19** **No** exponer tokens de Supabase ni claves secretas a JavaScript.
- [ ] **F4.20** Definir **expiración y renovación** de sesión (sliding window con tope absoluto).
- [ ] **F4.21** Implementar **logout** que invalida la sesión en servidor (no solo borrar la cookie).
- [ ] **F4.22** ⚠️ **No asumir que borrar la cookie invalida los tokens emitidos por Supabase.** Diseñar la revocación según el mecanismo elegido.
- [ ] **F4.23** Definir qué pasa cuando se **revoca una sesión**, se **suspende una cuenta** o **cambian los permisos**.

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

- [ ] **F4.34** Límite de peticiones en `login`, `register`, `forgot-password`, `reset-password`. (4.35 es el complemento en el borde.)
- [ ] **F4.35** Activar `security.rateLimiter` en `nuxt.config.ts:262` (hoy `false`) **limitado a esos endpoints**, tal como anticipa el comentario de las líneas 258-261.
- [ ] **F4.36** Verificar el correo antes de habilitar operaciones que requieran cuenta verificada.
- [ ] **F4.37** Añadir handlers de `AuthenticationException` (401) y `AccessDeniedException` (403) a `ApiExceptionHandler` para que devuelvan `ProblemDetail` coherente.
      ⚠️ Los tests actuales usan `standaloneSetup` sin la cadena de filtros: **añadir Spring Security no los hará validar autorización**. Añadir `springSecurity()` a los MockMvc existentes.
- [ ] **F4.38** Limitar el body de las peticiones de auth (`requestSizeLimiter` está en `false` en `nuxt.config.ts:250`; decidir si se activa solo para esas rutas).

**Definición de hecho:** `login` emite cookie `HttpOnly`; `me` devuelve el usuario; `logout` invalida en servidor; un token manipulado da 401; los 107 tests siguen verdes.

---

## 6. FASE 5 — Backend: autorización de negocio

- [ ] **F5.1** Crear el modelo de roles en Kotlin (D5). ⚠️ Nombre distinto de `VariantMediaRole`.
- [ ] **F5.2** **No deducir el rol por el correo**, por un campo editable del perfil ni por datos enviados por el frontend.
- [ ] **F5.3** Asignar y cambiar roles **solo** mediante operaciones administrativas confiables (idealmente no editables desde la API pública).
- [ ] **F5.4** Verificar **propiedad de cada recurso**: un cliente solo consulta sus pedidos y direcciones.
- [ ] **F5.5** **No confiar en un `user_id` recibido en el cuerpo, query string o encabezados arbitrarios.** La identidad procede siempre de la sesión validada.
- [ ] **F5.6** Verificar permisos administrativos **en el servidor** (`@PreAuthorize` o equivalente).
- [ ] **F5.7** **Proteger la escritura del catálogo.** Hoy `POST/PUT/DELETE` de categories, products, product-variants, product-variant-prices, product-variant-media, variant-3d-components y `media-assets/upload` son públicos. Esto es prioritario, antes que las tablas de pedidos.
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
- [ ] **F6.2** Activar **CSRF** en `nuxt-config` (`nuxt-csurf@1.6.6` **ya está en `node_modules`**, solo falta la configuración) — resuelve **B3**.
- [ ] **F6.3** Mantener `corsHandler: false` (origen único) — decisión coherente con D3.

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

- [ ] **F7.1** Migración **`V7__crear_profiles.sql`** (siguiente libre; **no tocar V1-V6**).
- [ ] **F7.2** `profiles.user_id` → `REFERENCES auth.users(id)` (solo si se va a usar `auth.users` de Supabase).
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

- [ ] **F8.1** Añadir `springSecurity()` a los MockMvc existentes (`CategoryControllerTest`, `MediaAssetControllerTest`, `ProductVariantMediaControllerTest`).
- [ ] **F8.2** Añadir el **primer** `@SpringBootTest` / `@WebMvcTest` con contexto real.
- [ ] **F8.3** Añadir `spring-security-test` a `build.gradle.kts`.
- [ ] **F8.4** Frontend: instalar `vitest` + `@nuxt/test-utils` y añadir script `test`. Hoy hay **0 tests**.

### 8.2 Casos obligatorios

- [ ] **F8.5** Usuario **A** no puede consultar pedidos del usuario **B**.
- [ ] **F8.6** Usuario **no autenticado** recibe **401** en endpoints privados.
- [ ] **F8.7** Usuario **autenticado sin permisos** recibe **403**.
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

- [ ] **F9.1** Verificar que ningún secreto está en `NUXT_PUBLIC_*`.
- [ ] **F9.2** Verificar `.gitignore` / `.dockerignore` siguen cubriendo secretos (ya lo hacen: re-verificar tras los cambios de F2).
- [ ] **F9.3** Confirmar **HTTPS en producción** (Caddy `tls internal` ya está; validar el resto).
- [ ] **F9.4** CORS solo con orígenes necesarios — o **nada**, manteniendo el proxy de origen único (recomendado).
- [ ] **F9.5** Confirmar protección **CSRF** activa con las cookies de sesión (F3 + F6.2).
- [ ] **F9.6** Confirmar límites de peticiones en autenticación y endpoints sensibles.
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
