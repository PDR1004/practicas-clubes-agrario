#!/bin/sh
# Crea el rol y la base de datos dedicados de Keycloak.
# Solo se ejecuta cuando el volumen de datos de PostgreSQL esta vacio
# (docker-entrypoint-initdb.d). Es idempotente por si se invoca manualmente.
set -e

: "${KEYCLOAK_DB:?KEYCLOAK_DB must be set}"
: "${KEYCLOAK_DB_USER:?KEYCLOAK_DB_USER must be set}"
: "${KEYCLOAK_DB_PASSWORD:?KEYCLOAK_DB_PASSWORD must be set}"

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname "$POSTGRES_DB" \
     --set=kc_role="$KEYCLOAK_DB_USER" \
     --set=kc_db="$KEYCLOAK_DB" \
     --set=kc_password="$KEYCLOAK_DB_PASSWORD" <<'SQL'
-- format('%I') cita el identificador y format('%L') cita el literal,
-- de modo que los valores del .env se interpolan sin riesgo de inyeccion.
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'kc_role', :'kc_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'kc_role')
\gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'kc_db', :'kc_role')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'kc_db')
\gexec
SQL

echo "keycloak_db: rol '${KEYCLOAK_DB_USER}' y base '${KEYCLOAK_DB}' verificados"
