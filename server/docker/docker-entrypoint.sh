#!/bin/sh
set -e

# Replace Gradle expand() placeholders in index.html with environment variables.
# The production build leaves empty strings; this fills them at container start.
envsubst '$OIDC_DISCOVERY_URI $OIDC_CLIENT_ID $OIDC_SCOPE' \
    < /app/web/index.html \
    > /app/web/index.html.tmp \
    && mv /app/web/index.html.tmp /app/web/index.html

exec "$@"
