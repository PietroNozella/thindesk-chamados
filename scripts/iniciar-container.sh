#!/bin/sh
set -eu

: "${MONGODB_URI:?Defina MONGODB_URI para conectar ao banco externo.}"
: "${APP_ADMIN_USERNAME:?Defina APP_ADMIN_USERNAME para a conta inicial.}"
: "${APP_ADMIN_PASSWORD:?Defina APP_ADMIN_PASSWORD para a conta inicial.}"

if [ "$APP_ADMIN_PASSWORD" = "thindesk123" ]; then
    echo "Defina uma senha propria para o administrador. A senha local padrao nao e aceita no container." >&2
    exit 1
fi

exec java -jar /app/app.jar
