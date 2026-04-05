FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache gettext && \
    addgroup -S cookmaid && adduser -S cookmaid -G cookmaid

WORKDIR /app
COPY server/build/install/server/ .
COPY composeApp/build/dist/wasmJs/productionExecutable/ /app/web/
COPY docker/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh && chown -R cookmaid:cookmaid /app

USER cookmaid

ENV WEB_DIR=/app/web
EXPOSE 8081

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["/app/bin/server"]
