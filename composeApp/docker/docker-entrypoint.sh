#!/bin/sh
set -e

# Replace Gradle expand() placeholders in index.html with environment variables.
# The production build leaves empty strings; this fills them at container start.
envsubst '$OIDC_DISCOVERY_URI $OIDC_CLIENT_ID $OIDC_SCOPE' \
    < /usr/share/nginx/html/index.html \
    > /usr/share/nginx/html/index.html.tmp \
    && mv /usr/share/nginx/html/index.html.tmp /usr/share/nginx/html/index.html

exec "$@"
