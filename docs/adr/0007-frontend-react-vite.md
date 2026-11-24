# ADR-0007: Frontend React + Vite

- **Estado**: Aceptada
- **Fecha**: 2026-11-23

## Contexto

El informe describe el frontend como **React** (SPA/PWA, `React.lazy`, `Suspense`, `AuthContext`, `keycloak-js`,
`.env.development`), pero el andamiaje del repositorio venía de otro proyecto y estaba preparado para
**Angular**: el `Dockerfile` copiaba `dist/dacs-fe/browser/` (salida del builder de Angular), el CI usaba
`--configuration=production` y `.gitignore` ignoraba `**/.angular/`. Además el realm de Keycloak permitía
`redirectUris` con `localhost:4200`, el puerto del dev server de Angular.

Había que alinear el andamiaje con el documento o el documento con el andamiaje.

## Decisión

**React + Vite** como stack del frontend, y se ajusta el andamiaje para que coincida:

1. `frontend/Dockerfile` compila con `npm run build -- --base=/dacs/` y copia `dist/` (Vite) a `/opt/dacs/`.
2. El CI corre `npm run build` sin flags de Angular.
3. `.gitignore` y `.dockerignore` ignoran `.vite`, no `.angular`.
4. El realm usa `http://localhost:5173/*` (puerto de Vite) en `redirectUris` y `webOrigins`.

## Alternativas consideradas

- **Quedarse con Angular:** habría implicado reescribir la sección 9.3 del informe y el resto de la
  documentación de arquitectura. El documento está cerrado y aprobado por la cátedra; el andamiaje no tenía
  código de aplicación, así que adaptarlo era más barato y no destructivo.
- **React con otro bundler (CRA, webpack):** descartado. Vite es el estándar actual y el informe ya lo menciona
  como bundler de referencia.

## Consecuencias

Positivas:

- Documento y código quedan alineados; no hay dos verdades.
- Build más rápido y configuración mínima.

Negativas:

- El `Dockerfile` depende del contrato de salida de Vite (`dist/`) y de que el `base` sea `/dacs/`; si se cambia
  la ruta del SPA en Nginx, hay que cambiarla en los dos lugares.
- El paquete `keycloak-js` se integra a mano (no hay un `keycloak-angular` equivalente a cargo).

## Referencias

- `docs/INFRA.md` §3 y §4; `docs/API-CONTRATO.md` §2.1.
- Informe: §9.3, §9.6.
