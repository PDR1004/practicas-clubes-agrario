# Sistema de Gestión Integral — Club Agrario Rocamora

Prácticas Supervisadas Profesionalizantes (200 h) · UTN FR Concepción del Uruguay
Equipo: Juan Cruz, Agustín Ponce Perez, Ezequiel Martino, Pablo Racigh · Tutor: Adrián Tournour

Plataforma multi-club para la liga: padrón de jugadores y fichajes, cuotas sociales, seguro IAPSER,
fixture y estadísticas, y generación de documentación oficial.

> **La documentación vive en [`docs/`](docs/).** Este README es solo la puerta de entrada: cómo está
> montado el entorno y cómo levantarlo.

---

## Estado del proyecto

**Etapa de preparación técnica en curso.** El repositorio contiene hoy solo el andamiaje: Dockerfiles,
Compose, configuración de Nginx y Keycloak, y la documentación. La infraestructura base (PostgreSQL,
Keycloak y pgAdmin) levanta y está verificada; Nginx está configurado pero su servicio todavía no arranca,
y los servicios de aplicación aún no tienen código. `docker compose up -d --build` falla en los cuatro
hasta que exista `frontend/package.json` y `backend/*/pom.xml`. El avance está en el tablero de Jira (`KAN`).

---

## Stack

| Componente | Tecnología | Puerto host |
|---|---|---|
| Proxy inverso / TLS | Nginx 1.27 | `80`, `443` |
| Cliente web (PWA) | React + Vite | servido en `/clubes/` |
| BFF | Spring Boot 3 · Java 21 | `9001` |
| API de dominio | Spring Boot 3 · Java 21 · JPA | `9003` |
| Conector externo | Spring Boot 3 · Java 21 | `9004` → interno `9002` |
| Identidad (IAM) | Keycloak · OAuth2 / OIDC | `8080` |
| Base de datos | PostgreSQL 16 | `5433` |
| Administración de BD | pgAdmin 4 | `5050` |

## Arquitectura

```text
Navegador
   │ HTTPS 443 — https://clubes.local
   ▼
Nginx ──► /clubes/ Frontend React (build estático)
   ├────► /auth/   Keycloak  (Standard Flow + PKCE S256)
   └────► /api/    BFF :9001 ──► ms-backend :9003 ──► PostgreSQL 16 · clubes_db
                             └─► ms-conector :9002 ──► APIs externas
```

Dos bases separadas en la misma instancia: **`clubes_db`** (dominio, rol `clubes`) y **`keycloak_db`**
(identidad, rol `keycloak`). Se crean solas en el primer arranque con
`infra/postgres/init/01-create-keycloak-db.sh`.

## Estructura del repositorio

```text
.
├─ docs/                     Documentación del proyecto (ver tabla abajo)
├─ frontend/                 Dockerfile del cliente web (React + Vite)
├─ backend/
│  ├─ ms-bff/                BFF (:9001, context path /bff)
│  ├─ ms-backend/            API de dominio (:9003, /backend)
│  └─ ms-conector/           Conector externo (:9002, /conector)
├─ infra/
│  ├─ docker-compose.yml     Servicios, volúmenes y red
│  ├─ postgres/init/         Creación de keycloak_db en el primer arranque
│  ├─ nginx/                 default.conf y certificados locales (ssl/, ignorado)
│  ├─ keycloak/              Realm exportado para --import-realm
│  └─ .env.example           Plantilla de variables de entorno
└─ .github/workflows/ci.yml  Pipeline de CI
```

## Documentación

| Documento | Para qué sirve |
|---|---|
| [`docs/CONTEXTO.md`](docs/CONTEXTO.md) | Alcance, roles, RF, reglas de negocio y preguntas abiertas |
| [`docs/DOMINIO.md`](docs/DOMINIO.md) | Entidades, enumeradores y notas de implementación |
| [`docs/DB-SCHEMA.md`](docs/DB-SCHEMA.md) | Mapa entidad→tabla, restricciones en DDL e invariantes |
| [`docs/API-CONTRATO.md`](docs/API-CONTRATO.md) | Contrato HTTP, envelope de errores y accesos (borrador) |
| [`docs/INFRA.md`](docs/INFRA.md) | Topología, puertos, Nginx, Keycloak y certificados |
| [`docs/adr/`](docs/adr/README.md) | 11 decisiones de arquitectura con alternativas y consecuencias |

---

## Puesta en marcha (local)

Requisitos: **Docker Desktop** (contenedores Linux) y **OpenSSL** en el PATH.

Nada de esto se versiona: `infra/.env`, `infra/nginx/ssl/*.pem` y cualquier clave quedan fuera del repo.

### 1. Variables de entorno

```powershell
Set-Location .\infra
Copy-Item .env.example .env
notepad .env
```

Reemplazá las contraseñas de ejemplo. Variables: `POSTGRES_*`, `KEYCLOAK_DB*` (base y rol de
Keycloak), `KEYCLOAK_ADMIN*` y `PGADMIN_DEFAULT_*`.

> pgAdmin rechaza dominios de uso especial como `.local`. El Compose ya lo habilita con
> `PGADMIN_CONFIG_ALLOW_SPECIAL_EMAIL_DOMAINS`; no cambies el email sin tocar esa línea.

### 2. Dominio local

Como administrador, en `C:\Windows\System32\drivers\etc\hosts`:

```text
127.0.0.1 clubes.local
```

### 3. Certificado HTTPS local

Desde `infra/`:

```powershell
openssl req -x509 -newkey rsa:2048 -nodes `
  -keyout .\nginx\ssl\key.pem `
  -out .\nginx\ssl\cert.pem `
  -days 3650 `
  -subj "/C=AR/ST=ER/L=C del U/O=UTN/CN=clubes.local" `
  -addext "subjectAltName=DNS:clubes.local,DNS:localhost,IP:127.0.0.1"
```

Cada integrante genera el suyo: `cert.pem` y `key.pem` deben ser **del mismo par** y nunca se commitean.

Opcional para el navegador: doble clic en `infra\nginx\ssl\cert.pem` → instalar en
**Equipo local → Entidades de certificación raíz de confianza**.

> **El certificado también es requisito de build.** Las imágenes Java lo importan al `cacerts` del JRE
> con `keytool` (vía el contexto de build `certs`). Sin `cert.pem`, el build de `backend/*` falla.

### 4. Levantar el entorno

```powershell
docker compose config --quiet
docker compose up -d --build      # todo el stack (falla hasta que exista código de aplicación)
docker compose up -d postgres pgadmin keycloak   # solo infraestructura
docker compose ps
```

Para detener sin perder datos: `docker compose down`.

> **Nunca `docker compose down -v`** salvo que quieras borrar la base y el realm de Keycloak. Es irreversible.

## Accesos

| Qué | Dónde |
|---|---|
| Aplicación web | <https://clubes.local/clubes/> |
| Consola Keycloak | <https://clubes.local/auth/admin> |
| Realm | <https://clubes.local/auth/realms/clubes> |
| pgAdmin | <http://localhost:5050> |
| Ping del BFF | <https://clubes.local/api/ping> |
| Ping al backend | <https://clubes.local/api/backendping> |
| Ping al conector | <https://clubes.local/api/conectorping> |
| Health del BFF | <https://clubes.local/api/metrics/health> |

Usuarios de desarrollo del realm (contraseña de desarrollo: `1234`, definida en `infra/keycloak/clubes-realm.json`):
- `admin` (SUPERADMIN, id_liga=1)
- `admin-club` (ADMIN_CLUB, id_club=1)
- `tesorero` (TESORERO, id_club=1)
- `delegado` (DELEGADO, id_club=1)
- `entrenador` (ENTRENADOR, id_club=1)

## Flujo de trabajo

- **Conventional Commits**, en español, sin `Co-Authored-By`.
- **Una rama corta por tarea** (`feat/kan-37-estructura-maven`) y PR revisado. Nadie empuja directo a `main`.
- **Archivos compartidos** (`docker-compose.yml`, `ci.yml`, `pom.xml`, `.env.example`): avisar antes de tocarlos.
- El backlog y la trazabilidad RF → HU se gestionan en **Jira, proyecto `KAN`**.

## Notas

- El CI está definido pero **no puede pasar** hasta que existan `frontend/package-lock.json` y
  `backend/*/pom.xml`: los jobs fallan en `setup-node` / `setup-java` por el caché. Está previsto en `KAN-36`.
- El historial de decisiones y su justificación están en [`docs/adr/`](docs/adr/README.md), no en los commits.
