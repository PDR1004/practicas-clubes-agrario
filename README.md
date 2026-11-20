# Prácticas Supervisadas — Gestión de Clubes

Repositorio base con la infraestructura de contenedores (Docker Compose, Nginx con HTTPS local, Keycloak, PostgreSQL y pgAdmin) y pipeline de CI para el sistema de administración de clubes.

---

## Arquitectura Base

```text
Navegador
   │ HTTPS (443) — https://clubes.local
   ▼
Nginx ─── Frontend (frontend/)
   ├───── Keycloak (autenticación OAuth2 / OIDC)
   └───── BFF (:9001) ───── Backend Core (:9003) ───── PostgreSQL 16
                      └──── Conector (:9002)     ───── APIs Externas
```

- **Frontend (`frontend/`):** SPA servida detrás de Nginx. Autentica usuarios con Keycloak y consume el BFF (`/api/...`).
- **Backend (`backend/`):** Microservicios Spring Boot (Java 21) con Dockerfiles multi-stage listos para construir cada servicio.
- **Autenticación:** Keycloak con importación automática de realm en `infra/keycloak/`.
- **Infraestructura (`infra/`):** Nginx termina HTTPS y enruta las solicitudes; Docker Compose orquesta toda la plataforma junto con PostgreSQL 16 y pgAdmin.

---

## Estructura del Repositorio

```text
practicas-clubes-agrario/
├─ frontend/                 Dockerfile y .dockerignore del cliente web
├─ backend/                  Dockerfiles base para microservicios
│  ├─ dacs-bff/              Backend-for-Frontend (:9001)
│  ├─ dacs-backend/          API Core y persistencia PostgreSQL (:9003)
│  └─ dacs-conector/         Conector hacia APIs externas (:9002)
├─ infra/                    Orquestación Docker Compose, Nginx y Keycloak
│  ├─ docker-compose.yml     Definición de servicios y volúmenes
│  ├─ nginx/                 Configuración HTTPS (default.conf) y certificados locales (ssl/)
│  ├─ keycloak/              Exportación del realm para importación automática
│  └─ .env.example           Plantilla de variables de entorno
├─ .github/                  Workflows de CI, plantilla de Pull Request y CODEOWNERS
└─ README.md                 Guía de onboarding e infraestructura
```

---

## Configuración Inicial (Para Integrantes del Equipo)

> **Importante:** Por seguridad, ni `infra/.env` ni los certificados `infra/nginx/ssl/*.pem` se suben a Git. Cada integrante debe configurarlos localmente una sola vez.

### 1. Variables de entorno

Abrí PowerShell en la carpeta `infra`:

```powershell
Set-Location .\infra
Copy-Item .env.example .env
notepad .env
```

Reemplazá las contraseñas de ejemplo en `.env` por contraseñas locales propias.

### 2. Dominio local (`hosts`)

Agregá esta línea al archivo `C:\Windows\System32\drivers\etc\hosts` (abriendo el editor como Administrador):

```text
127.0.0.1 clubes.local
```

### 3. Certificado HTTPS local

Generá `cert.pem` y `key.pem` dentro de `infra\nginx\ssl\` para `clubes.local` usando OpenSSL (incluido con Git for Windows o OpenSSL Win64):

```powershell
openssl req -x509 -newkey rsa:2048 -nodes `
  -keyout .\nginx\ssl\key.pem `
  -out .\nginx\ssl\cert.pem `
  -days 3650 `
  -subj "/C=AR/ST=ER/L=C del U/O=UTN/CN=clubes.local" `
  -addext "subjectAltName=DNS:clubes.local"
```

*(Opcional)* Hacé doble clic en `infra\nginx\ssl\cert.pem` e instalalo en **Equipo local → Entidades de certificación raíz de confianza** para que el navegador no muestre advertencia de certificado autofirmado.

### 4. Levantar infraestructura con Docker

Desde `infra/`:

```powershell
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

> Mientras `frontend/` y `backend/` todavía no tengan código fuente, podés levantar solo los servicios de infraestructura base:
> ```powershell
> docker compose up -d postgres pgadmin keycloak
> ```

Accesos principales:

- **Aplicación Web:** <https://clubes.local/dacs/>
- **Consola Keycloak:** <https://clubes.local/auth/admin>
- **pgAdmin:** <http://localhost:5050>
- **Ping del BFF:** <https://clubes.local/api/ping>

Para detalles adicionales de Docker Compose y Keycloak, consultá [infra/README.md](infra/README.md).
