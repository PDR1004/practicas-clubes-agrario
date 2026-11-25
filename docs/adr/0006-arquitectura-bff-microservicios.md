# ADR-0006: Arquitectura BFF + microservicios + Nginx

- **Estado**: Aceptada
- **Fecha**: 2026-11-23

## Contexto

El sistema sirve a dos tipos de usuario con necesidades distintas (gestión de escritorio para tesorería y
administración; operación móvil en cancha para delegados y entrenadores) y debe aislar la integración con
servicios externos (IAPSER, mensajería, APIs de la liga). El frontend no debe conocer la topología interna.

## Decisión

1. **Nginx** como puerta de entrada única: termina TLS, sirve el SPA en `/clubes/`, y rutea `/auth/` y `/api/`.
2. **BFF** (`/bff`, :9001) como única API consumida por el frontend. Orquesta llamadas internas con OpenFeign,
   adapta DTO y **estandariza el envelope de error** `{ status, code, description, message }`. Sin persistencia.
3. **`ms-backend`** (`/backend`, :9003) con la lógica de negocio y la persistencia JPA sobre PostgreSQL.
4. **`ms-conector`** (`/conector`, :9002) como adaptador hacia APIs externas. Sin persistencia.
5. **Keycloak** como `Resource Server` validado en **ambas** capas (BFF y backend), con propagación del token
   vía `FeignAuthInterceptor`.
6. Mismo origen para SPA, API e IdP: elimina CORS en despliegue.

## Alternativas consideradas

- **Monolito único con el frontend consumiendo directo:** descartada para este alcance. La separación BFF /
  dominio / conector es un requisito de la cátedra y del informe, y aísla el acoplamiento con APIs externas.
- **API Gateway de aplicación (Spring Cloud Gateway):** descartada. Nginx ya resuelve TLS y ruteo; sumar otro
  salto no aporta en este alcance.
- **Frontend hablando directo con `ms-backend`:** descartada. Duplicaría CORS, expondría la topología y
  obligaría al frontend a conocer dos contratos.

## Consecuencias

Positivas:

- El frontend tiene un solo contrato y un solo origen.
- La validación del token en dos capas impide que un salto interno opere sin permisos.

Negativas:

- Tres despliegues que versionar y mantener sincronizados.
- Riesgo clásico del BFF: duplicar reglas de negocio. **Regla del equipo:** el BFF solo orquesta y adapta; la
  decisión de negocio vive en `ms-backend`.
- Un salto de red extra por request.

## Referencias

- `docs/CONTEXTO.md` §8; `docs/INFRA.md` §1–§3.
- `docs/API-CONTRATO.md` §1 y §3.
- Informe: §9.1–§9.5.
