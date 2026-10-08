# PremiumCoffe — infraestructura Docker

Este directorio es la **capa de orquestación**. No contiene código de aplicación:
solo la forma en que el backend se levanta, se migra y se expone.

```
PremiumCoffe/
├── .dockerignore          contexto de build del monorepo (Backend + Frontend)
├── .gitignore             secretos y artefactos de ambos lados
├── Backend/               Kotlin / Spring Boot / Gradle
│   ├── src/main/resources/db/migration/   ← los SQL que aplica "migrate"
│   └── src/main/resources/application.properties
├── Docker/                ← estás aquí
│   ├── docker-compose.yml
│   ├── backend.Dockerfile
│   ├── .env               secretos, NO se sube a git
│   ├── .env.example       plantilla, sí se sube a git
│   └── ARCHITECTURE.md
└── Frontend/              vacío — pendiente de definir (ver más abajo)
```

---

## Levantar el proyecto

```bash
cd Docker

cp .env.example .env      # rellena DB_URL, DB_USER, DB_PASSWORD
docker compose up --build
```

| Comando | Para qué |
|---|---|
| `docker compose up --build` | levanta `migrate` y luego `backend` |
| `docker compose logs -f backend` | sigue el arranque |
| `docker compose config --quiet` | **valida** el `.env` **sin imprimir secretos** |
| `docker compose down` | para todo |
| `docker compose build --no-cache backend` | rebuild limpio |

El orden lo garantiza `depends_on` con `condition: service_completed_successfully`:
`backend` no arranca hasta que `migrate` termina **con éxito**.

> ### ⚠️ No pegues la salida de `docker compose config` en ningún sitio
>
> Ese comando imprime **tus credenciales de Supabase y Cloudinary en texto
> plano**: la contraseña de la base de datos, la API key y la API secret.
> No lo pegues en un chat, en un issue, en un ticket, ni en un log de CI.
>
> Para depurar variables usa siempre estas dos, que no muestran valores:
>
> ```bash
> docker compose config --quiet          # solo dice si valida o no (exit 0 = OK)
>
> # ver SOLO qué variables están definidas, nunca sus valores:
> docker compose config --environment | cut -d= -f1
> # en PowerShell:
> (docker compose config --environment) -replace '=.*', ''
> ```
>
> Si crees que un `config` sin cuidado ya quedó pegado en algún log o chat,
> **rota las credenciales**. No hay forma de confiar en que esa copia siga
> siendo privada.

---

## Por qué las migraciones son un servicio aparte

Con un solo contenedor no se nota. El problema aparece al escalar.

Si Flyway corre dentro del backend, al subir a 5 réplicas pasan dos cosas:

1. Las 5 arrancan a la vez y las 5 ven el esquema desactualizado.
2. Las 5 intentan migrar. Una gana el lock de la tabla `flyway_schema_history`
   y las otras 4 fallan y tumban el arranque.

Sacarlo a un servicio de un solo uso elimina la carrera por construcción. **Y es
exactamente el mismo concepto en la nube**, solo cambia el nombre:

| Local (compose) | AWS | Azure | GCP |
|---|---|---|---|
| servicio `migrate` | ECS Task / Step Functions | Container Apps **Job** | Cloud Run **Job** |
| servicio `backend` | ECS Fargate / App Runner | Container Apps | Cloud Run |
| `Docker/.env` | Secrets Manager | Key Vault | Secret Manager |
| `LOG_FORMAT=ecs` en stdout | CloudWatch | Log Analytics | Cloud Logging |
| `/actuator/health` | target group health check | probes | startup probe |

El backend además corre con `SPRING_FLYWAY_ENABLED=false`, así que aunque alguien
lo levante suelto nunca toca el esquema. Hay un solo escritor.

---

## El `.env`: dónde está y por qué no te va a_traicionar

Compose lee **el `.env` que esté en el mismo directorio que el `docker-compose.yml`**,
o sea `Docker/.env`. No mira el de la raíz del repo ni el de `Backend/`.

El mecanismo tiene tres pasos, y separarlos es lo que evita la confusión:

1. Compose lee `Docker/.env` **en el host**, antes de construir nada.
2. Sustituye los `${DB_URL}`, `${DB_USER}`… de `docker-compose.yml`.
3. Docker **construye** la imagen usando el `.dockerignore` de la raíz del repo.

**El paso 1 y el paso 3 son independientes.** Por eso `.env` sigue apareciendo en
el `.dockerignore` sin romper nada: lo único que hace es impedir que tus
credenciales de Supabase y Cloudinary queden selladas dentro de una capa de la
imagen, donde ya **no** se podrían borrar nunca.

Además hay dos defensas para que nunca te pase el «está ahí pero no lo toma»:

- `${DB_URL:?mensaje}` — si falta la variable, compose **se detiene y lo dice**.
  Sin ese `:?`, compose la sustituye por cadena vacía, el backend arranca y
  falla mucho después con un error mucho más difícil de leer.
- `env_file: [{path: .env, required: true}]` — si el archivo no existe, compose
  falla de entrada en vez de levantar un contenedor vacío.

Cuando algo falle por variables, `docker compose config --quiet` es el primer
paso: sale con código 0 si todo cuadra, y con el mensaje de `${VAR:?...}` si
falta algo. **No uses `config` a secas**, imprime los secretos.

---

## Decisiones ya tomadas

**PostgreSQL nunca en el compose de producción.** Supabase/RDS/Azure Database lo
gestionan. La app es **stateless** (media en Cloudinary, BD gestionada, cero
volúmenes) y eso es justamente lo que hace que escalar sea trivial: puedes clonar
contenadores sin ninguna Consideración de afinidad.

**Caché de Gradle vía BuildKit.** `--mount=type=cache,target=/root/.gradle`
conserva las dependencias entre builds. No acaba en la imagen final.

**Usuario no-root.** AWS, Azure y GCP rechazan o penalizan imágenes que corran
como root.

**`-XX:MaxRAMPercentage=75.0`.** La JVM toma su límite del límite de memoria del
contenedor, no de la RAM de la máquina. Sin esto, un contenedor con 512 MB
intentaría reservar heap según lo que ve en el host.

---

## Pendiente: Caddy como reverse proxy

Se implementa cuando exista el frontend. Un solo origen público:

- `/api/*` → `backend:8080`
- todo lo demás → `frontend:3000`
- TLS automático con `localhost` y con el dominio real

El objetivo es que **local y cloud se vean idénticos**, y que por eso mismo
**no necesites CORS en producción**. El esquema del servicio ya está comentado
en `docker-compose.yml`.

CORS **no está implementado** hoy y no hace falta: no hay frontend en otro origen.
Si algún día lo necesitas para desarrollo, se declara en el backend como
`WebMvcConfigurer` — pero la respuesta correcta sigue siendo el proxy.

---

## Pendiente: definir el frontend ⚠️

**No está decidido todavía.** Cuando se defina, hay que cerrar estas dos cosas
*antes* de escribir su Dockerfile:

1. **¿SSR o estático?** Si es Next.js con SSR, la imagen corre Node y usa
   `output: 'standalone'`; el standalone no incluye los assets estáticos y hay
   que copiarlos aparte. Si acaba siendo estático, la imagen es nginx sirviendo
   archivos y no lleva Node en runtime.
2. **¿El Three.js se renderiza en cliente?** SSR nunca va a renderizar el 3D;
   el navegador inicializa Three.js y carga el `.glb`. La decisión afecta al
   reparto entre servidor y cliente.

### La trampa del `NEXT_PUBLIC_*`

Las variables `NEXT_PUBLIC_*` **se hornean en el build**, no se leen en runtime.
Es decir: si la URL del API queda fija en la imagen, necesitas una imagen distinta
por entorno y no te sirve la misma para todos.

Dos formas de evitarlo, a decidir con el stack:

- **Runtime config** — leer la URL del API en el servidor y pasarla al cliente
  en una ruta `/config` o inyectándola en el `<head>`. Una sola imagen para todos
  los entornos.
- **Una imagen por entorno** — más simple, pero hay que construir N imágenes en CI.

---

## Cuando lleves esto a la nube

1. **No subas la imagen a la nube, súbela a un registro.** Publica
   `Docker/backend.Dockerfile` en ECR / ACR / Artifact Registry con un tag de
   commit. El build ya no ocurre en el servidor.
2. **Sustituye el `.env` por el gestor de secretos.** `${DB_URL:?}` del compose es
   la convención; en la nube la variable llega desde Secrets Manager / Key Vault /
   Secret Manager inyectada en tiempo de ejecución.
3. **CI:** build → tests → push de la imagen → Job de migración → despliegue de la
   app. El orden importa: migrar antes de desplegar, siempre.
4. **Cuidado con las conexiones.** `DB_POOL_SIZE=10` es **por contenedor**. Con 5
   réplicas son 50 conexiones. Sube el pooler de Supabase o baja el pool antes de
   escalar, no después de ver los timeouts.
5. **Ojo con la versión de Flyway.** El CLI de la imagen oficial (11.x) y el que
   gestiona Spring Boot pueden no coincidir. Hoy no importa porque solo el CLI
   escribe el esquema, pero si alguna vez aparece un error de validación de
   Flyway, mira ahí primero.