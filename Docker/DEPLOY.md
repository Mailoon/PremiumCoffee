# PremiumCoffe — despliegue gratuito

Este documento es la respuesta a una pregunta concreta: **cómo se monta este
proyecto entero sin pagar nada**, y **qué límites tiene cada pieza** para saber
dónde está el techo.

No es un plan teórico. Los números de memoria y de arranque de aquí están
**medidos en local** con los mismos límites que se ponen en el despliegue.

---

## 1. La arquitectura

Cuatro piezas, cuatro cuentas gratuitas, ninguna tarjeta de pago:

```
                    Cloudflare  (DNS + TLS + CDN, gratis)
                                  │
                ┌─────────────────┴─────────────────┐
                │                                   │
        Vercel (Hobby)                    Google Cloud Run
        Nuxt 4 SSR                          Kotlin · 512 MB · 0.5 vCPU
        100 GB ancho de banda                escala a 0 si no hay tráfico
                │                                   │
                └───────────────┬───────────────────┘
                                │
          ┌─────────────────────┼─────────────────────┐
          │                     │                     │
     Supabase              Cloudinary            Cloudflare R2
     Postgres               fotos 2D             .glb del visor 3D
     500 MB                 25 GB                 10 GB · EGRES GRATIS
```

### Por qué esta reparto y no otro

| Pieza | Dónde | Por qué ahí |
|---|---|---|
| **Frontend (Nuxt SSR)** | Vercel Hobby | SSR real, CDN global, 100 GB de ancho de banda, `*.vercel.app` sin tarjeta. El preview por PR sale gratis, que para un portfolio es oro. |
| **Backend (Kotlin)** | Cloud Run | Es lo caro dealojar: una JVM. Cloud Run es el único gratuito que escala a 0 sin dejar de pagar. Y es el que ya tiene equivalencia documentada en `ARCHITECTURE.md` (el servicio `migrate` → Cloud Run **Job**). |
| **Base de datos** | Supabase Free | Ya está configurado. 500 MB para un catálogo de café da de sobra. |
| **Fotos 2D** | Cloudinary Free | Ya está configurado. 25 GB. |
| **`.glb` del visor 3D** | Cloudflare R2 | **Cambio importante.** Ver §4. |

---

## 2. Los límites concretos de cada capa gratuita

Esto es lo que hay que tener grabado, porque son los techos reales.

### Google Cloud Run (backend)

| Concepto | Free tier |
|---|---|
| Peticiones | 2.000.000 / mes |
| vCPU-segundos | 180.000 / mes |
| GiB-segundos de memoria | 360.000 / mes |
| Egress | 1 GB / mes (Norteamérica) |
| Instancias mínimas | 0 (escala a cero) |
| Máximo por instancia | 4 vCPU / 16 GB |
| Timeout de petición | 60 min |

**Traducción a horas de disponibilidad.** Con el backend en 0.5 vCPU y 0.5 GB:

- CPU: 180.000 / 0.5 = **360.000 s ≈ 100 horas/mes**
- Memoria: 360.000 / 0.5 = 720.000 s ≈ 200 horas/mes

→ **El techo lo pone la CPU: unas 100 horas de backend encendido al mes.**

Para un portfolio eso es mucho: un sitio que recibe 50 visitas al día gasta
unas 5 horas. Even así, con `min-instances=1` (siempre encendido) el mismo
backend consumiría 1.296.000 vCPU-segundos al mes, que son **7,2 veces** el
tier gratuito. **No se puede tener el backend siempre encendido gratis.** Es
la restricción número uno de todo este plan.

### Vercel Hobby (frontend)

| Concepto | Límite |
|---|---|
| Ancho de banda | 100 GB / mes |
| Builds | 1.000 / día |
| Duración de función | 60 s (Hobby) |
| Memoria de función | 1.024 MB |
| Dominio | `*.vercel.app` incluido |
| HTTPS | automático |

Aviso: **Hobby es "uso personal"**. Para un portfolio personal, perfecto. Para
una empresa, no.

### Supabase Free (base de datos)

| Concepto | Límite |
|---|---|
| Tamaño | 500 MB |
| Egress | 1 GB / mes |
| **Pausa** | **tras 1 semana sin conexiones** |

La pausa es la trampa: si nadie visita el portfolio en una semana, la base de
datos se apaga y la primera visita tarda en despertar. Se*Dímelo* al visitante
o se acepta.

### Cloudinary Free (fotos)

| Concepto | Límite |
|---|---|
| Almacenamiento | 25 GB |
| Ancho de banda | 25 GB / mes |
| Transformaciones | ~10.000/mes (créditos) |

### Cloudflare R2 (`.glb`)

| Concepto | Límite |
|---|---|
| Almacenamiento | 10 GB |
| Operaciones Clase A | 1.000.000 / mes |
| Operaciones Clase B | 10.000.000 / mes |
| **Egress** | **GRATIS** ← la razón de usarlo |

---

## 3. La simulación local

Los límites ya están puestos en `docker-compose.yml`, con `deploy.resources`:

| Servicio | Límite | RSS medido | Sin límite |
|---|---|---|---|
| `backend` | 512 MB / 0.5 vCPU | **294 MB** | 579 MB |
| `frontend` (modo prod) | 512 MB / 1 vCPU | **26 MB** | 26 MB |
| `frontend` (modo dev) | 1 GB / 1 vCPU | **~450 MB** | 475 MB |

El total del stack de producción (frontend + backend + caddy) se queda en
**~350 MB**, frente a los 1,05 MB que ocupaba antes de poner los límites.

### Por qué el límite de memoria reduce tanto el consumo

`-XX:MaxRAMPercentage=75.0` dice "toma el 75% de la memoria **del contenedor**".
Con un contenedor sin límite, la JVM ve los 31 GB de Docker Desktop y se
permite un heap máximo de 23 GB. G1 dimensiona sus estructuras internas para
ese heap, y ahí es donde se va la memoria de más.

Con el límite de 512 MB:

- `MaxRAMPercentage=75` → heap máximo de 384 MB
- G1 dimensiona para 384 MB en vez de para 23 GB
- RSS final: **294 MB**

**El límite de memoria no es una restricción que se impone: es lo que hace que
la JVM deje de reservar de más.** Y de paso reproduce el techo real del
despliegue, que es lo que se quería.

> **Aviso**: el límite duro también es un riesgo. Si el proceso se pasa de
> 512 MB, Docker lo mata con `SIGKILL` y el contenedor queda como
> `exited (137)` **sin stack trace**. Es preferible que se caiga visible a que
> se caiga en silencio en producción. Por eso hay que mirar los logs del
> contenedor, no solo el código de salida.

### Cold start

| | Medido | Cómo se midió |
|---|---|---|
| Frontend producción (Nuxt SSR) | **0,86 s** | 3 corridas: 876 / 852 / 859 ms |
| Backend (JVM + Spring Boot + conexión a Supabase) | **42-48 s** | 2 corridas: 48,6 s / 42,4 s |

> **Cómo hay que medir el arranque, y por qué importa.** Medirlo con un bucle
> tipo `Invoke-WebRequest -TimeoutSec 5` en PowerShell **da números inventados**:
> cada intento aborta a los 5 segundos y nunca completa, así que el bucle se
> queda hasta el final del timeout y reporta 60-120 s. Con eso se llegó a
> pensar que el frontend tardaba dos minutos en arrancar, cuando en realidad
> arranca en **0,86 s**.
>
> El método fiable es esperar a que la petición HTTP devuelva 200, con un
> timeout holgado por intento, y medir el total hasta el primer 200.

El frontend no tiene ningún problema de arranque. **El backend es el
problema**: 45 segundos son 45 segundos de "no responde" para el visitante que
llega a un sitio parado.

Mitigaciones, de menos a más invasiva:

1. **AppCDS.** Spring Boot 3.3+ trae soporte de Class Data Sharing nativo:
   guarda las clases ya cargadas en un archivo y las recarga en vez de
   parsearlas otra vez. Recorta el arranque del JVM entre un 20% y un 30%.
2. **Spring Boot lazy-init.** `spring.main.lazy-initialization=true` arranca
   los beans solo cuando se necesitan. En un catálogo con pocas rutas, ahorra
   bastante.
3. **Aceptarlo.** Si el portfolio se visita a diario, Cloud Run rarely estará
   frío y no se nota. Si se visita una vez al mes, el visitante espera 45 s.

**Lo que NO se puede hacer es `min-instances=1`**: como se vio en §2, eso
multiplica por 7,2 el consumo de CPU y se sale del tier gratuito.

---

## 3.b Ensayar el despliegue en local

Hay **un solo servicio de frontend** y **una sola imagen**. El modo lo decide
la variable `MODE`, y el override `docker-compose.override.yml` es lo que anade
volumenes y cambia `MODE` a `dev`.

| Comando | Que arranca | Frontend | Caddy |
|---|---|---|---|
| `docker compose up -d` | **desarrollo** | `:3001`, con HMR | no |
| `docker compose -f docker-compose.yml up -d` | **produccion** | `:3000`, build | no |
| `docker compose --profile tls up -d` | desarrollo + TLS | `:3001` | `:8443` |
| `docker compose -f docker-compose.yml --profile tls up -d` | **produccion + TLS** | `:3000` | `:8443` |

El primer comando auto-carga `docker-compose.override.yml` porque ese nombre
es el que Compose busca. **En cuanto escribes `-f` a mano, el override deja de
cargarse** — y con el, el modo dev. Esa es toda la mecanica.

Que sea **un servicio y no dos** no es un detalle de redaccion: es lo que
garantiza que no puedan coexistir. Dos servicios con nombres distintos pueden
quedarse levantados a la vez sin que nadie se entere; uno solo no puede. Al
cambiar de modo, el contenedor se recrea en el sitio, no se duplica.

El override describe solo el servicio `frontend`. Los demas (`migrate`,
`backend`, `caddy`) vienen del compose base, y por eso no aparecen ahi: un
override es un parche, no un compose completo.

### El coste de unificar: 320 MB de imagen

La imagen unificada lleva el `node_modules` de desarrollo, que en modo
producción no se usa. Son 570 MB en vez de 242 MB.

Se paga en **tiempo de descarga**, no en memoria ni en CPU: el contenedor de
producción sigue usando 26 MB de RAM, y en Cloud Run el tamaño de la imagen no
cuesta dinero (solo el tiempo de instancia). Si algún día el registro pasa a
molestar, `docker buildx` con `--output type=image` y un multi-stage puede
generar las dos variantes desde el mismo Dockerfile.

---

## 3.c Lo que ahora garantizan los Dockerfiles

| Garantía | Dónde | Por qué importa |
|---|---|---|
| **Base fijada por digest** | ambas | Un rebuild dentro de tres meses produce la misma base. Un tag (`node:22-alpine`) puede cambiar de contenido sin avisar. Con digest, un CVE nuevo se ve en el diff. |
| **Una sola imagen y un solo servicio** | frontend | La diferencia entre desarrollo y producción es la recarga en caliente, y nada más. Que sea un servicio y no dos es lo que **garantiza** que no puedan coexistir: dos servicios distintos pueden quedarse levantados a la vez sin que nadie se entere. |
| **`MODE` decide el modo** | frontend | `dev` arranca `nuxt dev`, `prod` arranca el build. El default es `prod`: si la variable se pierde, se levanta la versión optimizada, no un watcher escribiendo en el filesystem. |
| **HEALTHCHECK** | ambas | `depends_on: service_healthy` significa algo real, no "el proceso existe". |
| **STOPSIGNAL SIGTERM** | ambas | En la nube, la plataforma escala a cero mandando SIGTERM y esperando antes del SIGKILL. Ese margen es el que evita cortar peticiones a medias. |
| **`init: true` (tini)** | frontend, backend | Node y la JVM como PID 1 no reapean zombis ni reenvían señales bien. Con tini, el apagado es el que la app espera de verdad. |
| **`read_only: true`** | frontend, backend, caddy | Si un proceso intenta escribir fuera de `/tmp`, revienta en vez de dejar basura. Lo que se escribe de verdad va a volúmenes. |
| **`no-new-privileges`** | frontend, backend, caddy | Ningún proceso puede ganar privilegios con `setuid`. |
| **Sin `PORT` fijo en la imagen** | frontend | La plataforma lo inyecta (Cloud Run pone 8080). Si la imagen trajera 3000, el healthcheck de la plataforma miraría al puerto equivocado. |
| **Usuario no-root en producción** | frontend | El entrypoint baja a `node` con `su-exec` en modo prod. En modo dev se queda como root, porque tiene que escribir en los volúmenes que vienen del host. |
| **`.nuxt` y `.output` fuera del contexto** | `.dockerignore` | Sin esto, el build de Docker copia el `.nuxt` local y `nuxt build` lo reutiliza: la imagen sale con las rutas de la última vez que corriste el dev server. Es un bug que no falla, falla cuando le da la gana. |


El `start-period` de cada healthcheck está calibrado con el arranque real
medido: 20 s para el frontend (que arranca en 0,86 s) y 60 s para el backend
(que tarda 42-48 s). Ponerlos más cortos provoca reinicios en cadena cuando
la máquina está cargada.

### Cómo se lee un apagado limpio

| Exit code | Significado |
|---|---|
| **0** | apagado normal, el proceso terminó solo |
| **143** | 128+15: recibió SIGTERM y **hizo su limpieza** antes de morir |
| **137** | 128+9: lo mataron con SIGKILL porque no acabó dentro del margen |

Medido en el ensayo: frontend **0**, caddy **0**, backend **143**. Los tres
correctos. El 143 del backend es el esperado: la JVM recibe SIGTERM, Spring
Boot ejecuta su shutdown hook (`server.shutdown=graceful` deja de aceptar
conexiones y espera a las peticiones en vuelo) y el proceso muere por la señal
después de limpiar.

**Un 137 sí sería un problema**: significaría que el margen de gracia se agotó
y se cortó una petición a medias. En Cloud Run el margen lo da la plataforma,
pero el `stop_grace_period` del compose es el equivalente local.

### Lo que este ensayo destapo

El `Caddyfile` **no arrancaba**. Llevaba tiempo ahí y nunca se había
ejercitado, porque el perfil `tls` dependía de un servicio `frontend` que no
existía en el compose. El error era:

```
unknown subdirective 'encode', at /etc/caddy/Caddyfile:102
```

`encode` no es un subdirectivo de `file_server`: es una directive del mismo
nivel. Con esa sintaxis, Caddy sale con código 1 y **sin escribir nada en los
logs**, y el contenedor se queda en bucle de reinicio sin explicar por qué.

Dos cosas de ahí que conviene tener presentes:

1. `docker compose config --quiet` valida el **YAML**, no la configuración de
   Caddy. Para eso está `caddy validate --config /etc/caddy/Caddyfile`, que
   es lo que hay que meter en el CI.
2. Un contenedor que se reinicia en bucle y no dice nada es casi siempre un
   error de configuración de la aplicación, no de la imagen.
---



Esta es la decisión que más afecta al despliegue y la razón por la que R2
entra en el plan.

Un `.glb` con texturas KTX2 pesa entre 10 y 60 MB. Cloudinary está pensado
para imágenes y vídeo: su pipeline de transformación no va a tocar bien un
`.glb`, y cada descarga del visor **contaría contra los 25 GB de ancho de
banda** de Cloudinary. Un visitante que abre el visor dos veces puede llevar
100 MB de los 25 GB mensuales.

R2 da 10 GB de almacenamiento y **egress gratis**, servido por la CDN de
Cloudflare. Para assets 3D es la herramienta correcta.

### El coste oculto: la CSP

El `nuxt.config.ts` tiene una CSP estricta con nonce. Si los `.glb` pasan a
servirse desde R2, hay que tocar **tres** directives:

```css
'connect-src': ["'self'", 'https://res.cloudinary.com', 'https://<tu-bucket>.r2.dev'],
'worker-src':  ["'self'", 'blob:'],
'media-src':   ["'self'", 'blob:', 'https://res.cloudinary.com', 'https://<tu-bucket>.r2.dev'],
```

- `connect-src` → es la directive que gobierna el `fetch` que Three.js hace
  del `.glb`. Sin ella, el navegador bloquea la descarga.
- `worker-src` → los decodificadores de **Draco** y **KTX2** corren en Web
  Workers, y esos workers se crean con `blob:`. Ya está cubierta, pero
  conviene no tocarla.
- `media-src` → si el `.glb` o alguna textura se cargan como `<video>` o
  `<source>`, también pasa por aquí.

Y el bucket de R2 necesita `Access-Control-Allow-Origin`, porque Three.js
lo carga desde el origen del frontend.

### Y el peek más caro: el egress de Cloud Run

Si el `.glb` se sirve **a través** del backend, cada descarga sale de Cloud Run
y cuenta contra el 1 GB/mes gratuito de egress. Con 60 MB por `.glb`, eso son
**16 visitas al mes** antes de agotarlo.

**El `.glb` tiene que servirse directamente desde R2**, con URL pública, sin
pasar por el backend ni por el SSR. El backend solo guarda la URL en la base
de datos.

---

## 5. El plan de montaje, paso a paso

### Paso 1 — La base de datos (Supabase)

1. Crear el proyecto en Supabase.
2. Poner `DB_URL` (el **pooler** en puerto 6543, no el directo), `DB_USER` y
   `DB_PASSWORD` en `Docker/.env`.
3. `docker compose up migrate` para aplicar el esquema.
4. `DB_POOL_SIZE`: en Supabase Free el pooler da un máximo de conexiones
   conexiones por proyecto. **Poner 10 por réplica** con varias réplicas
   revienta el pool. Con una sola réplica, 10 va bien.

### Paso 2 — El backend (Cloud Run)

```bash
# 1) Publicar la imagen en Artifact Registry (no subir el código a la nube)
gcloud artifacts repositories create premiumcoffe \
    --repository-format=docker --location=europe-west1

gcloud builds submit --tag \
    europe-west1-docker.pkg.dev/PROYECTO/premiumcoffe/backend:latest \
    Backend/

# 2) Desplegar
gcloud run deploy premiumcoffe-backend \
    --image europe-west1-docker.pkg.dev/PROYECTO/premiumcoffe/backend:latest \
    --region europe-west1 \
    --cpu 0.5 --memory 512Mi \
    --min-instances 0 --max-instances 2 \
    --no-allow-unauthenticated
```

Después hay que **conceder acceso de invocar** al servicio al usuario que
firme, porque `--no-allow-unauthenticated` deja el servicio en privado:

```bash
gcloud run services add-iam-policy-binding premiumcoffe-backend \
    --region=europe-west1 \
    --member=allUsers \
    --role=roles/run.invoker
```

> `allUsers` en un servicio público es correcto **para un portfolio**: cualquiera
> puede llamarlo, que es lo que quieres. No lo pases por alto: significa que
> cualquiera puede hacer peticiones a tu API.

Los secretos van en Secret Manager, no en la imagen:

```bash
gcloud secrets create db-url --data-file=...
gcloud run services update premiumcoffe-backend --set-secrets=DB_URL=db-url:latest
```

### Paso 3 — Las migraciones (Cloud Run Job)

El mismo concepto que el servicio `migrate` del compose, con otro nombre:

```bash
gcloud run jobs create premiumcoffe-migrate \
    --image .../backend:latest \
    --region europe-west1 \
    --set-env-vars=SPRING_FLYWAY_ENABLED=true \
    --set-secrets=DB_URL=db-url:latest
```

Un solo uso, y **siempre antes** de desplegar la app.

### Paso 4 — El frontend (Vercel)

Vercel detecta el `Frontend/` por el `framework: nuxt`. Dos cosas a mirar:

1. **La URL del backend.** `NUXT_API_BASE` en el panel de Vercel, apuntando a
   la URL pública de Cloud Run. Es una variable de **runtime**, no de build:
   la misma imagen sirve para todos los entornos. (Este es exactamente el
   problema del `NEXT_PUBLIC_*` que describe `ARCHITECTURE.md`, ya resuelto
   con `runtimeConfig`.)
2. **La CSP.** Si el frontend va a otro origen que el backend, la CSP tiene que
   permitirlo, y **CORS** hay que declararlo en el backend como
   `WebMvcConfigurer`. La alternativa mejor es poner un proxy delante
   (Cloudflare Worker o el propio Caddy) y que todo sea un origen solo.

### Paso 5 — El dominio

Cloudflare Registrar lo vende **a coste**, sin markup. Un `.com` de dominios
baratos sale por menos de 10 €/año. Los subdominios gratuitos (`*.vercel.app`,
`*.run.app`) funcionan para empezar.

---

## 6. La lista de comprobación antes de publicar

- [ ] El `.glb` se sirve desde R2, no desde el backend (si no, el egress se
      agota en 16 visitas)
- [ ] La CSP permite el origen de R2 en `connect-src` y `media-src`
- [ ] El bucket de R2 tiene CORS para el origen del frontend
- [ ] `DB_POOL_SIZE` × réplicas ≤ conexiones del pooler de Supabase
- [ ] `min-instances=0` en el backend, y presupuesto de alertas en Google Cloud
- [ ] `gcloud run services add-iam-policy-binding` con `allUsers` ejecutado
- [ ] Los secretos en Secret Manager, nunca en la imagen ni en el `.env` del
      repositorio
- [ ] `docker compose config --quiet` en verde, y **nunca** el `config` a secas
      pegado en ningún sitio (imprime las credenciales)
- [ ] El healthcheck de Cloud Run apunta a `/actuator/health`
- [ ] `SPRING_FLYWAY_ENABLED=false` en el servicio de la app

---

## 7. Lo que sigue pendiente en el repositorio

- **Cloudinary sigue siendo el sitio de las fotos 2D.** Si en algún momento las
  imágenes son de Cloudinary y los `.glb` de R2, conviene documentar cuál es
  cuál en el `CONTEXT.md` del backend.
- **Falta decidir el `.glb` en el backend.** Hoy el proxy de API lee el cuerpo
  como JSON, así que una subida de `multipart` (un `.glb` de 60 MB) llegaría
  corrupta. Es lo que dice el aviso del propio `server/api/[...path].ts`: hace
  falta una ruta propia con `readMultipartFormData()`. Importa para el
  despliegue, porque define si el `.glb` pasa por el backend (y gasta egress)
  o va directo a R2.
- **AppCDS y lazy-init** siguen sin activarse. Están medidos como mitigación
  del cold start de 42-48 s (§3), pero requieren tocar el build de Gradle.

