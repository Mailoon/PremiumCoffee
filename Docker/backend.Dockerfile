FROM eclipse-temurin:21-jdk@sha256:3b3021d7e64bffd34cd5a61c102802e64e3c7e91aa867b1e9c2a7323f21c6f69 AS build
WORKDIR /app
COPY Backend/gradle ./gradle
COPY Backend/gradlew Backend/gradlew.bat Backend/settings.gradle.kts Backend/build.gradle.kts ./

COPY Backend/src ./src

RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew bootJar --no-daemon
FROM eclipse-temurin:21-jre@sha256:e6354536ca587af353f4927095003b3a5fc29fe1a1380fa1e55caec12815d188 AS runtime
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system app && useradd --system --gid app --home /app app

COPY --from=build --chown=app:app /app/build/libs/*.jar app.jar

USER app
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS "http://127.0.0.1:${PORT:-8080}/actuator/health" > /dev/null || exit 1

STOPSIGNAL SIGTERM

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]