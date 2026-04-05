#!/bin/sh
set -e

# Substitute OIDC environment variables into index.html at container startup.
# The production WasmJS build leaves these as empty strings; this fills them in.
envsubst '$OIDC_DISCOVERY_URI $OIDC_CLIENT_ID $OIDC_SCOPE' \
    < /app/web/index.html \
    > /app/web/index.html.tmp \
    && mv /app/web/index.html.tmp /app/web/index.html

exec "$@"
