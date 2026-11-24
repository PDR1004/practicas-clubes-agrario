# Club Agrario Rocamora — Contrato de la API (v1)

> Fuente: §9 del informe (arquitectura BFF, error envelope), §7 (roles) y las 19 HU de §3.1.1.
>
> **Estado: BORRADOR.** El informe define la arquitectura, el envelope de errores y los roles, pero **no**
> define endpoints. La lista de §5 es una **propuesta derivada de las historias de usuario** para arrancar el
> desarrollo; no es un contrato validado. Todo endpoint que se implemente sin acuerdo explícito del equipo debe
> quedar marcado como tal en el PR.
>
> Lo que **sí** es normativo acá: la base path, la reescritura de Nginx, el envelope de errores, la validación
> del token y la matriz de acceso por rol.

## 1. Overview

- Arquitectura: REST sobre HTTP con JSON.
- **Puerta de entrada única:** el frontend nunca habla con `ms-backend` ni con `ms-conector`. Todo pasa por el
  BFF a través de Nginx.
- **Base path público:** `/api`. Nginx reescribe `/api/<recurso>` → `/bff/<recurso>` y lo envía a
  `http://bff:9001` (§9.4). El context path interno del BFF es `/bff`.
- **Autenticación:** access token JWT de Keycloak como Bearer (§2).
- **Persistencia:** solo `ms-backend` accede a PostgreSQL.

Convenciones de representación *(propuestas, a confirmar con el equipo)*:

| Concepto | Formato | Ejemplo |
|---|---|---|
| Campos JSON | `camelCase` derivado de columnas `snake_case` | `idFichaje`, `fechaNacimiento` |
| Fechas | `YYYY-MM-DD` | `2010-04-17` |
| Timestamps | ISO 8601 con offset | `2026-11-23T19:30:00-03:00` |
| Dinero | number con dos decimales, sin símbolo | `8000.00` |
| IDs | number entero (el esquema usa PK `integer`) | `42` |
| Enums | tal como en la base, sin traducir | `A_Prestamo`, `Programado`, `Pendiente` |
| Listados | array completo; la paginación se decide más adelante | `[ { ... } ]` |

Los montos los calcula siempre el servidor: el cliente nunca envía importes en operaciones de cobro o
generación de cuotas.

## 2. Autenticación y roles

### 2.1 Token

- El login ocurre contra Keycloak desde el frontend con **Standard Flow + PKCE S256**; el cliente `dacs-fe` es
  público y no tiene secreto (§9.6).
- El frontend envía `Authorization: Bearer <access_token>` en cada request a `/api`.
- El **BFF y `ms-backend` validan el JWT de forma independiente** como OAuth2 Resource Servers
  (`spring.security.oauth2.resourceserver.jwt.issuer-uri`), verificando firma y emisor.
- El BFF propaga el mismo token a `ms-backend` con `FeignAuthInterceptor`.

### 2.2 Claims y alcance

| Claim | Uso |
|---|---|
| `realm_access.roles` | Roles de negocio; se transforman a `ROLE_<ROL>` en Spring Security |
| `id_club` | Tenant del usuario de club (protocol mapper, TTL corto) |
| `id_liga` | Alcance del usuario de liga (protocol mapper) |
| `sub` | Se mapea a `usuario.keycloak_sub` |

- El **rol se lee del token**; el **alcance por categoría no**: se resuelve en cada request contra
  `usuario_categoria` (§9.6 y `adr/0005`).
- El claim `id_club` / `id_liga` es un caché con TTL corto: si un usuario cambia de club, el alcance se corrige
  al renovar el token. La base sigue siendo la fuente de verdad.

### 2.3 Roles

| Rol Keycloak | Autoridad Spring | Alcance |
|---|---|---|
| `SUPERADMIN` | `ROLE_SUPERADMIN` | Global: ligas, clubes, temporadas, categorías, usuarios administradores |
| `ADMIN_CLUB` | `ROLE_ADMIN_CLUB` | Su club: padrón, configuración, usuarios, exportaciones |
| `TESORERO` | `ROLE_TESORERO` | Su club: cuotas, pagos, grupos familiares, morosidad, métricas |
| `DELEGADO` | `ROLE_DELEGADO` | Sus categorías: jugadores, tutores, fichajes, seguros, fixture, PDF, estadísticas |
| `ENTRENADOR` | `ROLE_ENTRENADOR` | Sus categorías: consulta y convocatorias |

## 3. Formato de errores

El BFF estandariza todas las respuestas de error (§9.5):

```json
{
  "status": 409,
  "code": "bff_409",
  "description": "Conflict",
  "message": "El jugador ya tiene un fichaje vigente en este club"
}
```

| HTTP | `code` típico | Cuándo |
|---|---|---|
| 400 | `bff_400` | Request inválido: campos faltantes, formato, enum fuera de rango |
| 401 | `bff_401` | Token ausente, malformado, expirado o emisor inválido |
| 403 | `bff_403` | Rol insuficiente, usuario inactivo o acceso a otro club |
| 404 | `bff_404` | Recurso inexistente |
| 409 | `bff_409` | Choque de unicidad: CUIT, DNI+liga, `(id_fichaje, periodo)`, dorsal, recibo, **temporada activa duplicada** |
| 422 | `bff_422` | Regla de negocio violada: RN-01 a RN-16, transición inválida |
| 502 | `bff_502` | `ms-backend` o `ms-conector` no responden |

Regla para el agente: **ningún servicio devuelve su envelope crudo al frontend**; el BFF traduce.

## 4. Matriz de acceso por rol

Propuesta derivada del alcance de §7. `Sí` = permitido; `—` = 403.

| Módulo | SUPERADMIN | ADMIN_CLUB | TESORERO | DELEGADO | ENTRENADOR |
|---|---|---|---|---|---|
| Ligas y clubes (alta, edición) | Sí | — | — | — | — |
| Configuración institucional del club | — | Sí | — | — | — |
| Temporadas y categorías de liga (alta, apertura, cierre) | Sí | — | — | — | — |
| Participación `club_temporada_categoria` | Sí | Sí | — | — | — |
| Renovación de planteles | — | Sí | — | — | — |
| Usuarios y asignación de roles/categorías | Sí | Sí | — | — | — |
| Padrón: alta/edición de jugadores y tutores | Sí | Sí | — | Sí | — |
| Consulta de padrón | Sí | Sí | Sí | Sí | Sí |
| Fichajes y pases | Sí | Sí | — | Sí | — |
| Seguros IAPSER | Sí | Sí | — | Sí | — |
| Bajas lógicas | Sí | Sí | — | Sí | — |
| Cuotas y pagos | Sí | Sí | Sí | — | — |
| Grupos familiares y becas | Sí | Sí | Sí | — | — |
| Fixture | Sí | Sí | — | Sí | — |
| Convocatoria | Sí | Sí | — | Sí | Sí |
| Estadísticas post-partido | Sí | Sí | — | Sí | — |
| Exportaciones PDF / XLSX | Sí | Sí | — | Sí | — |
| Notificaciones propias | Sí | Sí | Sí | Sí | Sí |

> `Sí` en SUPERADMIN es global; en el resto, **siempre acotado al propio club y a las categorías asignadas**.
> El aislamiento no se implementa con esta tabla: se implementa con el predicado de tenant en cada consulta
> (RNF03).

## 5. Endpoints propuestos (borrador, no validado)

Agrupados por módulo, con la HU que los origina. Los nombres son convención propuesta.

### 5.1 Estructura institucional (HU01, HU02, RF06)

| Método | Ruta | Descripción |
|---|---|---|
| `GET/POST` | `/ligas` | Listado y alta de ligas (SUPERADMIN) |
| `GET/POST` | `/clubes` | Listado y alta de clubes afiliados; valida CUIT único |
| `GET/PATCH` | `/clubes/{id}` | Detalle y personalización (escudo, colores, domicilio) |
| `GET/POST` | `/temporadas` | Listado y alta de temporadas (estado planificada) |
| `POST` | `/temporadas/{id}/abrir` | Apertura con unicidad por liga; 409 si ya hay una activa |
| `POST` | `/temporadas/{id}/cerrar` | Cierre: `fecha_cierre`, suspende los partidos `Programado`, bloquea altas del ciclo y no se puede deshacer (409) |
| `GET/POST` | `/categorias` | Categorías de la liga (`anio_desde`, `anio_hasta`) |
| `GET/POST` | `/participaciones` | `club_temporada_categoria` de la temporada activa |
| `POST` | `/participaciones/renovacion/preview` | Previsualización de la renovación de planteles (sin escribir) |
| `POST` | `/participaciones/renovacion` | Ejecuta la renovación (solo fichajes `Propio`); idempotente y con resumen de exclusiones |

### 5.2 Usuarios y roles (HU14, HU15, RF05)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/usuarios` | Listado con filtros por rol y estado |
| `POST` | `/usuarios` | Alta de usuario vinculado a club o liga |
| `PATCH` | `/usuarios/{id}` | Edición de datos y estado |
| `PATCH` | `/usuarios/{id}/rol` | Cambio de rol con historial en `usuario_rol` |
| `GET/PUT` | `/usuarios/{id}/categorias` | Asignación operativa por `club_temporada_categoria` |
| `GET` | `/auth/me` | Usuario, rol vigente y alcance resuelto |

### 5.3 Padrón, tutores y fichajes (HU03, HU04, HU05, HU18, RF01)

| Método | Ruta | Descripción |
|---|---|---|
| `GET/POST` | `/jugadores` | Padrón de la liga; alta con tutores |
| `GET/PATCH` | `/jugadores/{id}` | Legajo |
| `GET` | `/jugadores/{id}/fichajes` | Historial de fichajes |
| `POST` | `/jugadores/{id}/fichajes` | Nuevo fichaje; cierra el anterior en la misma transacción |
| `POST` | `/fichajes/{id}/pase` | Registro de pase (Club Cedente/Cesionario) |
| `GET/POST` | `/fichajes/{id}/seguros` | Pólizas IAPSER del fichaje |
| `POST` | `/fichajes/{id}/baja` | Baja lógica con motivo y auditoría |
| `GET/POST` | `/tutores` | Tutores globales |
| `GET` | `/jugadores/{id}/tutores` | Vínculos vigentes e historial |

### 5.4 Finanzas (HU06, HU07, HU17, RF02)

| Método | Ruta | Descripción |
|---|---|---|
| `GET/PUT` | `/valores-cuota` | Tarifa vigente e historial |
| `POST` | `/cuotas/generar` | Generación masiva por período (idempotente) |
| `GET` | `/cuotas` | Listado con filtros por estado, período y categoría |
| `POST` | `/pagos` | Registro de cobro (1..N cuotas, completo) |
| `POST` | `/pagos/{id}/anular` | Anulación con motivo |
| `GET` | `/grupos-familiares` | Agrupaciones y becas |
| `GET` | `/reportes/recaudacion` | Consolidado mensual por categoría e institucional |

### 5.5 Partidos y documentación (HU08, HU09, HU10, HU16, HU19, RF03, RF04)

| Método | Ruta | Descripción |
|---|---|---|
| `GET/POST` | `/partidos` | Fixture por participación |
| `PATCH` | `/partidos/{id}` | Reprogramación o suspensión |
| `GET/POST` | `/partidos/{id}/convocatoria` | Convocatoria con dorsal; admite categorías inferiores |
| `POST` | `/partidos/{id}/cambios` | Sustituciones (máx. 7) |
| `PATCH` | `/partidos/{id}/resultado` | Goles, tarjetas y estado del partido |
| `GET` | `/partidos/{id}/planilla` | PDF de planilla de partido |
| `GET` | `/listas-buena-fe` | PDF por categoría y temporada |
| `GET` | `/exportaciones/iapser` | `.xlsx` de nómina |
| `GET` | `/fichas/inscripcion-libre` | PDF de ficha libre |
| `GET` | `/fichas/pase-prestamo` | PDF de ficha de pase |

### 5.6 Notificaciones y diagnóstico (HU13, RF07)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/notificaciones` | Avisos del usuario |
| `PATCH` | `/notificaciones/{id}/leida` | Marcar como leída |
| `GET` | `/api/ping` | Ping del BFF |
| `GET` | `/api/backendping` | Ping en cascada a `ms-backend` |
| `GET` | `/api/conectorping` | Ping en cascada a `ms-conector` |
| `GET` | `/api/metrics/health` | Actuator (`/bff/metrics/health`) |

Notas de RF07:

- El aviso interno lo genera el **job diario de `ms-backend`**; no hay endpoint para dispararlo desde el cliente.
- **"Compartir por WhatsApp" es del frontend**: arma un enlace `https://wa.me/?text=…` con el saludo ya redactado. No consume API.
- El **envío automático** al WhatsApp del delegado lo ejecuta `ms-conector` (CallMeBot) como canal complementario
  *best-effort*: su fallo no revierte la notificación interna ni se reporta al usuario final.

## 6. Cobertura de requerimientos

| RF | HUs | Endpoints propuestos |
|---|---|---|
| RF01 | HU03, HU04, HU05, HU18 | §5.3 |
| RF02 | HU06, HU07, HU17 | §5.4 |
| RF03 | HU08, HU11, HU12, HU19 | §5.5 (PDF/XLSX) |
| RF04 | HU09, HU10, HU16 | §5.5 |
| RF05 | HU14, HU15 | §5.2 |
| RF06 | HU01, HU02, HU20, HU21 | §5.1 |
| RF07 | HU13 | §5.6 |

## 7. Supuestos y pendientes

1. **La lista de endpoints es una propuesta**, no un contrato acordado. Hasta que el equipo la valide, cada
   endpoint implementado se documenta en el PR.
2. **No hay versionado en URL.** Los cambios incompatibles se registran en este documento.
3. **Paginación y ordenamiento** sin definir; se resuelven cuando aparezca el primer listado grande.
4. **Idempotencia de `POST /cuotas/generar`** depende de `unique (id_fichaje, periodo)`; la respuesta debe
   informar cuántas se crearon y cuántas se omitieron.
5. **Los PDF y XLSX** dependen de layouts oficiales que todavía no se conocen (preguntas 9 y 3 del
   `CONTEXTO.md` §11). El mapeo de columnas IAPSER va aislado y parametrizado.
6. **El claim `id_club`** requiere configurar protocol mappers en Keycloak; mientras no exista, el BFF resuelve
   el alcance por `keycloak_sub` contra la base.
7. **Errores de negocio**: la distinción 409 (unicidad) vs 422 (regla de negocio) es convención de este
   documento; si el equipo prefiere otro mapeo, se actualiza acá primero.
8. **Credenciales de CallMeBot sin modelo**: el envío automático necesita el número y la API key personal de cada
   delegado, pero el esquema no tiene dónde guardarlos (`DB-SCHEMA.md` §8). Sin esa decisión, el endpoint de
   notificaciones funciona pero el canal WhatsApp queda sin implementar.
9. **Apertura de temporada** debe responder 409 y no 422 cuando ya existe una activa: es un choque de unicidad
   respaldado por `uq_temporada_activa_liga`. Lo mismo para **reabrir** una temporada cerrada.
10. **Cierre de temporada** bloquea altas en otros módulos (partidos, fichajes, cuotas), **suspende** los partidos
    `Programado` y **no** bloquea `Pago` ni anulaciones sobre cuotas ya emitidas. Ese comportamiento no es un
    endpoint: es una validación dentro de cada caso de uso de escritura.
11. **Renovación de planteles** solo procesa fichajes `Propio`; los `A_Prestamo` y `Libre` salen en el resumen de
    exclusiones para gestión manual. Un préstamo no se renueva solo.
