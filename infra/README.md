# Infraestructura Docker, Nginx y Keycloak (`infra/`)

Esta carpeta concentra la orquestación de contenedores con Docker Compose, el proxy inverso HTTPS con Nginx, la configuración reproducible del realm de Keycloak y las variables de entorno locales.

## Requisitos

- Docker Desktop iniciado con contenedores Linux.
- Crear `.env` a partir de `.env.example` y cambiar cada contraseña de ejemplo por una propia (**no subir `.env` a GitHub**).
- En el archivo `C:\Windows\System32\drivers\etc\hosts`, agregar como administrador:

  ```text
  127.0.0.1 clubes.local
  ```

- Generar un certificado local válido para `clubes.local` en `infra\nginx\ssl\` (`cert.pem` y `key.pem`, **no subirlos a GitHub**):

  ```powershell
  openssl req -x509 -newkey rsa:2048 -nodes `
    -keyout .\nginx\ssl\key.pem `
    -out .\nginx\ssl\cert.pem `
    -days 3650 `
    -subj "/C=AR/ST=ER/L=C del U/O=UTN/CN=clubes.local" `
    -addext "subjectAltName=DNS:clubes.local"
  ```

## Iniciar los servicios

Desde PowerShell ubicados en `infra/`:

```powershell
Set-Location .\infra
Copy-Item .env.example .env
# Editar .env y generar nginx\ssl\cert.pem y nginx\ssl\key.pem antes de iniciar.
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

Si todavía no agregaron código en `frontend/` o `backend/`, pueden levantar solo la base de datos y autenticación:

```powershell
docker compose up -d postgres pgadmin keycloak
```

Para detener los contenedores sin borrar la base:

```powershell
docker compose down
```

La base persiste en el volumen `postgres_data`. **No usar `docker compose down -v` salvo que se quiera borrar también la base y el realm de Keycloak.**

## Importación Automática de Keycloak (`keycloak/dacs-realm.json`)

El contenedor `keycloak-server` arranca con `--import-realm` y monta `infra/keycloak/dacs-realm.json` en `/opt/keycloak/data/import/dacs-realm.json`. Al levantar por primera vez, Keycloak crea automáticamente:

- **Realm:** `dacs` (renombrable según definan el dominio del proyecto)
- **Cliente OIDC público (PKCE S256):** `dacs-fe`
- **Roles del Realm:** `ROLE-A` y `ROLE-B`
- **Usuarios de prueba (contraseña de desarrollo: `1234`):** `user-a`, `user-b`, `admin`

> Si el realm ya existe en el volumen local `postgres_data`, Keycloak omite la importación para no sobrescribir datos locales.

## Comprobaciones

- Frontend: `https://clubes.local/dacs/`
- Keycloak Admin: `https://clubes.local/auth/admin`
- pgAdmin: `http://localhost:5050`
- Ping BFF: `https://clubes.local/api/ping`
- Health BFF: `http://localhost:9001/bff/metrics/health`
