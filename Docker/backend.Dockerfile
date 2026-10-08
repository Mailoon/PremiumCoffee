# syntax=docker/dockerfile:1

# ==========================================================================
# PremiumCoffe — imagen del backend (Kotlin / Spring Boot / Gradle)
#
# Contexto de build: la raiz del repo (por eso las rutas llevan "Backend/").
# Se invoca desde Docker/docker-compose.yml con:
#     context: ..
#     dockerfile: Docker/backend.Dockerfile
# ==========================================================================

# --------------------------------------------------------------------------
# Stage 1 — build
# --------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Solo los archivos de construccion primero. Asi Docker reutiliza esta capa
# mientras no cambien las dependencias, y no se re-descarga Gradle entero
# en cada cambio de codigo.
COPY Backend/gradle ./gradle
COPY Backend/gradlew Backend/gradlew.bat Backend/settings.gradle.kts Backend/build.gradle.kts ./

COPY Backend/src ./src

# --mount=type=cache conserva /root/.gradle entre builds. Es una cache de
# BuildKit: acelera reconstrucciones y NO acaba en la imagen final.
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew bootJar --no-daemon

# --------------------------------------------------------------------------
# Stage 2 — runtime
# --------------------------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# curl no viene en la imagen JRE, y lo necesita el healthcheck de compose y
# el health check de los balanceadores en la nube (/actuator/health).
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Usuario sin privilegios: las plataformas de contenedores en AWS, Azure y
# GCP corren como no-root por defecto y rechazan imagenes que lo ejecuten como root.
RUN groupadd --system app && useradd --system --gid app --home /app app

COPY --from=build --chown=app:app /app/build/libs/*.jar app.jar

USER app

# EXPOSE es solo documentacion; el puerto real lo fija el orquestador.
# Por defecto 8080, pero AWS/Azure/GCP inyectan la variable PORT.
EXPOSE 8080

# MaxRAMPercentage hace que la JVM tome su limite del limite de memoria del
# contenedor, en vez de la RAM de la maquina. Es lo que evita que un contenedor
# con 512 MB intente pedir al heap lo que ve en el host.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]