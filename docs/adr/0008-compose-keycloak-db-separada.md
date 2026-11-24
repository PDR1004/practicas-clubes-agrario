# ADR-0008: Docker Compose con base de Keycloak separada

- **Estado**: Aceptada
- **Fecha**: 2026-11-23

## Contexto

El `docker-compose.yml` inicial hacía que Keycloak usara las mismas credenciales y la misma base que la
aplicación (`POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`). El resultado: las 101 tablas del IdP convivían
con el esquema de negocio en `dacs_db`, y el rol de la aplicación podía leerlas.

## Decisión

1. Dos bases en la **misma instancia** PostgreSQL 16: `dacs_db` (dominio) y `keycloak_db` (IdP).
2. Dos roles: `dacs` (dueño de `dacs_db`) y `keycloak` (dueño de `keycloak_db`), con contraseñas distintas.
3. `keycloak_db` se crea en la primera inicialización con
   `infra/postgres/init/01-create-keycloak-db.sh`, montado en `/docker-entrypoint-initdb.d`. El script es
   idempotente y cita identificadores con `format('%I')` / `%L`.
4. Keycloak apunta a `keycloak_db` con sus propias variables (`KEYCLOAK_DB`, `KEYCLOAK_DB_USER`,
   `KEYCLOAK_DB_PASSWORD`), no con las de la aplicación.
5. `pgadmin` recibe la misma política de reinicio que el resto (`unless-stopped`).

## Alternativas consideradas

- **Base compartida (estado inicial):** descartada. Mezcla dos *bounded contexts* en un esquema, complica el
  backup lógico y hace que un compromiso del IdP alcance los datos de negocio.
- **Instancia PostgreSQL separada para Keycloak:** descartada por costo operativo. Para un entorno de práctica
  la separación lógica de base y rol es suficiente.
- **Crear la base a mano:** descartada. No es reproducible para el resto del equipo ni para un clon nuevo.

## Consecuencias

Positivas:

- Aislamiento real, verificado: el rol `keycloak` no puede crear tablas en `dacs_db`.
- El dump de `dacs_db` es del dominio, sin ruido del IdP.

Negativas:

- **El script de init corre solo con el volumen vacío.** Un entorno que ya tenía datos no migra solo: hay que
  borrar el volumen (pierde datos) o ejecutar el script a mano.
- Dos juegos de credenciales que rotar y documentar.
- Para administrar `keycloak_db` desde pgAdmin hay que conectarse con el rol `keycloak`.

## Referencias

- `docs/INFRA.md` §4, §5 y §9.
- `infra/postgres/init/01-create-keycloak-db.sh`, `infra/docker-compose.yml`, `infra/.env.example`.
- Informe: §9.7.
