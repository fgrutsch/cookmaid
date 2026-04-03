# syntax=docker/dockerfile:1

# ── Build stage ────────────────────────────────────────────────────────────────
FROM gradle:8-jdk21-alpine AS build
WORKDIR /workspace
COPY . .
RUN gradle :server:installDist :composeApp:wasmJsBrowserProductionWebpack \
    --no-daemon --parallel

# ── Runtime stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache gettext && \
    addgroup -S cookmaid && adduser -S cookmaid -G cookmaid

WORKDIR /app
COPY --from=build /workspace/server/build/install/server/ .
COPY --from=build \
    /workspace/composeApp/build/dist/wasmJs/productionExecutable/ \
    /app/web/
COPY --from=build /workspace/server/docker/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh && chown -R cookmaid:cookmaid /app

USER cookmaid

ENV WEB_DIR=/app/web
EXPOSE 8081

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["/app/bin/server"]
