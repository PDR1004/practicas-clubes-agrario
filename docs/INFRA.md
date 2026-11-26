# Club Agrario Rocamora — Infraestructura y entorno local

> Fuente: §9.7 del informe y el andamiaje real del repositorio (`infra/`).
> Este documento describe **cómo está montado** el entorno y **cómo levantarlo**. La arquitectura de
> aplicación y sus principios están en `adr/0006`; el modelo de datos, en `DB-SCHEMA.md`.

## 1. Topología

```
Navegador
   │ HTTPS 443 — https://clubes.local
   ▼
Nginx ──► /clubes/ Frontend React (PWA, build estático)
   ├────► /auth/   Keycloak :8080
   └────► /api/    BFF :9001 ──► ms-backend :9003 ──► PostgreSQL :5432 (clubes_db)
                            └─► ms-conector :9002 ──► APIs externas
```

Servicios de apoyo: **pgAdmin** (`:5050`) y la base **`keycloak_db`** en la misma instancia PostgreSQL.

## 2. Servicios y puertos

Definidos en `infra/docker-compose.yml` (proyecto Compose: `nginx-docker`).

| Servicio Compose | Contenedor | Puerto host → interno | Notas |
|---|---|---|---|
| `web` | `nginx_web` | `80:80`, `443:443` | Nginx: TLS, estáticos y proxy inverso |
| `ms-bff` | `ms-bff` | `9001:9001` | Context path `/bff`; sin persistencia |
| `ms-backend` | `ms-backend` | `9003:9003` | Context path `/backend`; único con JPA |
| `ms-conector` | `ms-conector` | `9004:9002` | Context path `/conector`; sin persistencia |
| `postgres` | `postgres_db` | `5433:5432` | Aloja `clubes_db` y `keycloak_db` |
| `keycloak` | `keycloak-server` | `8080:8080` | `KC_HTTP_RELATIVE_PATH=auth` |
| `pgadmin` | `pgadmin4_ui` | `5050:80` | UI de administración de la base |

Red interna: `nginx-docker_default`. Volumen de datos: `nginx-docker_postgres_data`.

## 3. Nginx (`infra/nginx/default.conf`)

- **`:80`** → `301` a HTTPS.
- **`:443`** con `cert.pem` / `key.pem` montados desde `infra/nginx/ssl/`.
- **`location /clubes/`** → `alias /opt/clubes/` con `try_files $uri $uri/ /clubes/index.html`. Sirve el build de
  Vite copiado dentro de la imagen del frontend.
- **`location = /clubes`** → `301` a `/clubes/`.
- **`location = /`** → `302` a `/clubes/`.
- **`location /`** → `404`. Nada se sirve en la raíz del dominio.
- **`location /api/`** → reescribe `/api/(.*)` a `/bff/$1` y proxya a `http://ms-bff:9001`, propagando
  `Host`, `X-Real-IP`, `X-Forwarded-For` y `X-Forwarded-Proto`.
- **`location /auth/`** → proxya a `http://keycloak:8080/auth/` con `proxy_buffering off` y `proxy_cache off`
  (Keycloak necesita streaming y no cachear).

## 4. Keycloak

- **Realm:** `clubes`, importado automáticamente al primer arranque desde `infra/keycloak/clubes-realm.json`
  (`--import-realm`). El contenedor lo monta en `/opt/keycloak/data/import/clubes-realm.json`: si el realm no
  aparece, revisá primero ese mount. Si el realm ya existe en la base, **no se reimporta**: los cambios del JSON
  no pisan datos existentes.
- **Cliente del frontend:** `clubes-fe`, público, Standard Flow + PKCE.
  `redirectUris` incluyen `https://clubes.local/*` y `http://localhost:5173/*` (puerto de **Vite**).
- **Roles:** los 5 roles canónicos (`SUPERADMIN`, `ADMIN_CLUB`, `TESORERO`, `DELEGADO`, `ENTRENADOR`), definidos por `adr/0005`, ya están creados en `infra/keycloak/clubes-realm.json`.
- **Usuarios de desarrollo** del realm actual (contraseña de desarrollo: `1234`, definida en `infra/keycloak/clubes-realm.json`):
  - `admin` (SUPERADMIN, id_liga=1)
  - `admin-club` (ADMIN_CLUB, id_club=1)
  - `tesorero` (TESORERO, id_club=1)
  - `delegado` (DELEGADO, id_club=1)
  - `entrenador` (ENTRENADOR, id_club=1)
- **Base de datos:** `keycloak_db` con rol propio `keycloak` — **no** usa `clubes_db`.

## 5. PostgreSQL

Dos bases en la misma instancia, con credenciales distintas:

| Base | Rol dueño | Uso |
|---|---|---|
| `clubes_db` | `clubes` (`POSTGRES_USER`) | Dominio de la aplicación |
| `keycloak_db` | `keycloak` (`KEYCLOAK_DB_USER`) | Persistencia de Keycloak |

El rol `keycloak` **no puede escribir en `clubes_db`** (verificado: `permission denied for schema public`).

`keycloak_db` se crea con `infra/postgres/init/01-create-keycloak-db.sh`, que se ejecuta **solo cuando el
volumen está vacío**. Es idempotente y cita identificadores con `format('%I')`/`%L`, así que tolera
contraseñas con comillas.

> Si el volumen ya existía antes de este cambio, la base no se crea sola: hay que borrar el volumen
> (`docker compose down -v`, **pierde los datos**) o ejecutar el script manualmente una vez.

## 6. Dominio local y certificados

Tres pasos, una sola vez por máquina. **No** se versionan: `.env`, `cert.pem` y `key.pem` están en `.gitignore`.

### 6.1 Variables de entorno

```powershell
Set-Location .\infra
Copy-Item .env.example .env
```

Reemplazar las contraseñas de ejemplo. Variables requeridas por el Compose:

`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `KEYCLOAK_DB`, `KEYCLOAK_DB_USER`,
`KEYCLOAK_DB_PASSWORD`, `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`, `PGADMIN_DEFAULT_EMAIL`,
`PGADMIN_DEFAULT_PASSWORD`.

> pgAdmin rechaza dominios de uso especial (`.local`) en `PGADMIN_DEFAULT_EMAIL`. El Compose ya lo permite con
> `PGADMIN_CONFIG_ALLOW_SPECIAL_EMAIL_DOMAINS: "['local']"`; no cambiar el valor sin cambiar esa línea.

### 6.2 Dominio en el archivo `hosts`

Como administrador, en `C:\Windows\System32\drivers\etc\hosts`:

```text
127.0.0.1 clubes.local
```

### 6.3 Certificado autofirmado

Desde `infra/`:

```powershell
openssl req -x509 -newkey rsa:2048 -nodes `
  -keyout .\nginx\ssl\key.pem `
  -out .\nginx\ssl\cert.pem `
  -days 3650 `
  -subj "/C=AR/ST=ER/L=C del U/O=UTN/CN=clubes.local" `
  -addext "subjectAltName=DNS:clubes.local,DNS:localhost,IP:127.0.0.1"
```

**Cada integrante genera el suyo.** Los pares son independientes y no se comparten: `cert.pem` y `key.pem`
siempre deben ser del **mismo par**. Nunca commitearlos (`git add -f` sería el único modo de romper esto).

### 6.4 Confianza del navegador (opcional)

Doble clic en `infra\nginx\ssl\cert.pem` → *Instalar certificado* → **Equipo local** →
*Entidades de certificación raíz de confianza*. Evita la advertencia de certificado autofirmado.

### 6.5 Confianza de la JVM (obligatorio para los servicios Java)

Los servicios Java validan `https://clubes.local/auth` al descubrir el issuer de Keycloak. **No se desactiva
la validación TLS**: el certificado se importa al `cacerts`.

Esto ya está resuelto en el build de las imágenes: cada `backend/*/Dockerfile` copia `cert.pem` desde el
contexto de build nombrado `certs` y ejecuta `keytool -importcert` sobre el `cacerts` del JRE. El Compose lo
alimenta con `additional_contexts: certs: ./nginx/ssl`.

> Consecuencia: **el build de las imágenes backend requiere que el certificado exista**. Un clon sin
> `infra/nginx/ssl/cert.pem` falla al construir. Y si se corre Spring Boot desde el IDE, el JVM del host
> **no** tiene ese certificado; hay que importarlo también ahí.

## 7. Cómo levantar

Requisitos: Docker Desktop (contenedores Linux) y OpenSSL en el PATH.

```powershell
Set-Location .\infra
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

Mientras no exista código de aplicación, levantar solo la base:

```powershell
docker compose up -d postgres pgadmin keycloak
```

**Estado real del repositorio:** `web`, `ms-bff`, `ms-backend` y `ms-conector` **no construyen** todavía
(faltan `package.json`, `pom.xml` y `src/`). Con lo que hay, arriba únicamente la infraestructura.

Parar sin perder datos:

```powershell
docker compose down
```

> **Nunca** `docker compose down -v` salvo que se quiera borrar la base y el realm. Es irreversible.

## 8. Health checks

| Qué | Dónde |
|---|---|
| Ping del BFF | `https://clubes.local/api/ping` |
| Ping al backend | `https://clubes.local/api/backendping` |
| Ping al conector | `https://clubes.local/api/conectorping` |
| Actuator del BFF | `https://clubes.local/api/metrics/health` → `/bff/metrics/health` |
| Actuator por puerto directo | `http://localhost:9001/bff/metrics/health` — separa "BFF caído" de "Nginx caído" |
| Salud de PostgreSQL | healthcheck del Compose (`pg_isready`) |
| Realm de Keycloak | `https://clubes.local/auth/realms/clubes` |

## 9. Pendientes de infraestructura

1. **Código de aplicación inexistente** — bloquea el `up --build` completo y el CI.
2. [HECHO] **Roles del realm** — los 5 roles canónicos ya están definidos y sin placeholders en `infra/keycloak/clubes-realm.json`.
3. **`directAccessGrantsEnabled: true`** en el cliente público `clubes-fe`: el informe especifica Standard Flow
   con PKCE. Habilitar el password grant en un SPA es una decisión pendiente de cerrar.
4. **Migraciones** sin definir (Flyway / Liquibase / `ddl-auto`); ver `DB-SCHEMA.md` §7.
5. **JVM del host** sin el certificado (afecta solo a ejecución desde IDE, no a contenedores).
6. **pgAdmin puede mostrar `clubes_db` y `keycloak_db`** con el usuario `clubes`; para administrar `keycloak_db`
   hay que conectarse con el rol `keycloak` o como superusuario.
7. **Salida a internet desde `ms-conector`** para el envío por CallMeBot (`api.callmebot.com`): requiere egress
   de red. El `cacerts` de las imágenes conserva las CA públicas, así que el certificado local no interfiere;
   el envío está marcado como *best-effort* y su fallo no debe afectar la notificación interna.
8. **Credenciales de CallMeBot** sin almacenamiento definido (ver `DB-SCHEMA.md` §8): número y API key personal
   por delegado. Sin esa decisión, el canal automático no se puede implementar.
