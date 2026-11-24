# Club Agrario Rocamora — Documento de contexto del proyecto

> Fuente: `informe_t_cnico_club_agrario_rocamora.md` (Práctica Supervisada, 200 horas — UTN FR Concepción del Uruguay).
> Tutor / Contraparte: Adrián Tournour.
> Equipo: Juan Cruz, Agustín Ponce Perez, Ezequiel Martino, Pablo Racigh.
> Institución: Club Agrario Rocamora.
>
> Este archivo es el contexto de referencia para todo el equipo y para el agente cuando trabaja en el repo.
> **Convención de idioma:** la documentación de equipo está en español neutro/profesional y los nombres de
> tablas, columnas y entidades del dominio siguen el español del informe (cátedra). El código de
> infraestructura (librerías, frameworks, configuraciones) mantiene la convención de su tecnología, en inglés.
> El detalle de cada área vive en los documentos hermanos: `DOMINIO.md`, `DB-SCHEMA.md`, `API-CONTRATO.md`,
> `INFRA.md` y `adr/`.

## 1. Problema y organización

El Club Agrario Rocamora gestiona hoy lo deportivo, lo financiero y lo administrativo de forma
**descentralizada e informalizada**:

- Planillas de Google Drive personales por delegado → riesgo de pérdida de información ante cada recambio.
- Cuadernos y conversaciones de WhatsApp para inscripciones, comprobantes de transferencia y avisos.
- Archivos sueltos en Word o imágenes para documentación oficial.

Dolores concretos que el sistema debe atacar:

1. **Sin padrón unificado** de deportistas.
2. **Sin control de cobranza** de cuotas sociales ni seguimiento de morosidad global e inter-categorías.
3. **Inconsistencias y demoras** al presentar la nómina de asegurados ante IAPSER (Instituto del Seguro de
   Entre Ríos).
4. **Sin trazabilidad del seguro** de los jugadores que se incorporan a mitad de temporada.
5. **Carga manual repetitiva** de planillas de partido y listas de buena fe exigidas por la liga local.

Contexto organizativo relevante: el club compite dentro de una **liga** con categorías y temporadas comunes,
y cada institución afiliada administra sus propios jugadores, cuotas y equipos. El sistema se concibe como
**multi-club**: la liga administra la competencia y cada club su operación.

## 2. Objetivo y alcance

**Objetivo general:** desarrollar una aplicación web responsive / **PWA** que centralice la administración de
jugadores, el control de cuotas sociales, el fixture semanal y la generación automatizada de documentación
oficial del club.

**Alcance inicial (in scope):**

- Padrón general de jugadores con legajo digital (datos personales, foto, estado registral).
- Seguimiento continuo del estado de asegurabilidad de cada jugador.
- Control de pagos de cuotas con reglas de negocio (descuento por hermanos, meses con tarifas diferenciales).
- Generador y exportador de nóminas oficiales (Excel IAPSER y planillas PDF de la liga).
- Calendario y fixture manual de partidos por fin de semana.
- Roles con accesos diferenciados (Administrador de Club, Tesorero, Delegado y Entrenador, más administración
  global de Liga).

**Fuera de alcance (out of scope):**

- Aplicación nativa en Google Play / App Store (evitar costos de publicación).
- Integración con pasarelas de pago automatizadas (Mercado Pago, etc.).
- Portal público general o acceso directo para padres/tutores en la primera etapa.

> **Criterio de alcance para el agente:** si una funcionalidad no aparece en el alcance ni en una historia de
> usuario, no se implementa "por las dudas". Se propone y se decide, no se asume.

## 3. Roles

| Rol (identificador Keycloak) | Alcance | Entorno principal |
|---|---|---|
| **SUPERADMIN** | Acceso global: alta y gestión de ligas, clubes afiliados, temporadas y categorías comunes, usuarios administradores de club y supervisión general. | Escritorio |
| **ADMIN_CLUB** | Administración integral de su institución: personalización visual, alta de usuarios y asignación de cargos/categorías, participación en categorías/temporadas, visibilidad total del padrón y exportación de nóminas IAPSER. | Escritorio |
| **TESORERO** | Gestión financiera exclusiva de su club: valores de cuota, generación mensual de períodos, registro y anulación de pagos, grupos familiares/becas, morosidad y métricas de recaudación. | Escritorio |
| **DELEGADO** | Gestión operativa de sus categorías asignadas: alta y legajo de jugadores, tutores, fichajes/pases, seguros, fixture semanal, emisión de PDF y carga de estadísticas. | Móvil (en cancha) / Escritorio |
| **ENTRENADOR** | Consulta de nóminas, vigencia de seguros y contactos de sus categorías; armado de convocatorias. Sin acceso a cuotas ni tesorería. | Móvil |

El **alcance operativo por categoría no es un rol de Keycloak**: se resuelve en PostgreSQL con
`Usuario_Categoria` sobre `Club_Temporada_Categoria` (ver `DOMINIO.md` §4 y `adr/0005`).

## 4. Requerimientos funcionales (resumen)

| RF | Alcance | HU asociadas |
|---|---|---|
| **RF01** Padrón y legajo | Datos personales, tutores, estado de fichaje, historial de altas/bajas, cobertura IAPSER y bajas lógicas. | HU03, HU04, HU05, HU18 |
| **RF02** Financiero y cuotas | Cobros completos y acumulados, recibo con anulación auditada, descuento por grupo familiar, becas, tarifas excepcionales y métricas por categoría e institucional. | HU06, HU07, HU17 |
| **RF03** Documentación oficial | Exportación `.xlsx` IAPSER, Lista de Buena Fe, Planilla de Partido y Fichas de Inscripción Libre / Pase a Préstamo en PDF. | HU08, HU11, HU12, HU19 |
| **RF04** Fixture y estadísticas | Carga manual del fixture, convocatoria con categorías inferiores, carga post-partido (goles, tarjetas, cambios). | HU09, HU10, HU16 |
| **RF05** Usuarios y accesos | Autenticación vía Keycloak, 5 roles canónicos, permisos por módulo y operación, historial de cargos y asignación por categoría. | HU14, HU15 |
| **RF06** Multi-club / liga | Alta de instituciones (CUIT único), entorno aislado por club, personalización institucional, temporadas y categorías de liga, participación de clubes y renovación de planteles. | HU01, HU02, HU20, HU21 |
| **RF07** Alertas y notificaciones | Notificación interna de cumpleaños con saludo institucional y acción "Compartir por WhatsApp", más aviso automático complementario al WhatsApp individual del Delegado vía `ms-conector`. | HU13 |

El detalle completo de las 21 historias de usuario y sus criterios de aceptación está en el informe fuente.
La matriz de trazabilidad HU → entidades está reproducida en `DOMINIO.md` §5.

## 5. Requerimientos no funcionales

- **RNF01 — Usabilidad (Mobile-First):** operaciones de campo (consulta de seguros, asistencia, convocatorias,
  estadísticas post-partido) fluidas en pantallas pequeñas y con conectividad intermitente (PWA).
- **RNF02 — Compatibilidad (Desktop):** vistas expandidas para tesorería (filtros masivos de cuotas),
  configuración del club y reportes gerenciales.
- **RNF03 — Seguridad y aislamiento:** aislamiento **lógico** estricto multi-tenant. Un usuario solo accede a
  los datos de su propio club. `foto_url` y datos de menores restringidos a roles autorizados de la institución.
  El `consentimiento_imagen` se gestiona **por fichaje** y no se traslada al cambiar de club. Los datos de
  tutores globales solo se consultan desde el club donde el menor tiene fichaje vigente.
- **RNF04 — Despliegue PWA:** instalable desde el navegador; sin tiendas de aplicaciones.

## 6. Reglas de negocio

Extraídas de los criterios de aceptación de las HU y de §8.1 del informe. Son **invariantes**: si el código las
contradice, el código está mal.

| ID | Regla | Origen |
|---|---|---|
| RN-01 | Un jugador solo puede tener **un fichaje vigente** (`fecha_baja IS NULL`) en la liga. | HU04, §8.1.2 |
| RN-02 | Cada jugador tiene entre **1 y 2 tutores vigentes** (`fecha_hasta IS NULL`); el tercero se rechaza. | HU03 CA2 |
| RN-03 | Los pagos de cuota son **completos**; no se admiten pagos fraccionados. | HU06 CA2 |
| RN-04 | Se admiten **cobros acumulados** de varios períodos en un mismo pago. | HU06 CA3 |
| RN-05 | El **segundo hermano** del mismo grupo familiar abona el 50 % del valor base de la cuota. | HU07 |
| RN-06 | La generación de cuotas es **idempotente** por `(id_fichaje, periodo)`. | HU17 CA2 |
| RN-07 | Máximo **7 sustituciones** por partido. | HU10 CA2 |
| RN-08 | Solo se sustituyen jugadores **convocados** en ese partido. | HU10 CA1 |
| RN-09 | La convocatoria admite la categoría del partido y las **inmediatamente inferiores**. | HU09 |
| RN-10 | El partido debe pertenecer a una participación activa `Club_Temporada_Categoria`. | HU16 CA1 |
| RN-11 | Un jugador y un dorsal son **únicos por partido**. | §8.1.5 |
| RN-12 | La baja lógica cierra el fichaje y registra auditoría **en la misma transacción**. | HU18 CA1 |
| RN-13 | Un seguro vencido marca al jugador como no habilitado y alerta al convocar. | HU05 CA2 |
| RN-14 | El **CUIT** es único por club. | HU01 CA2 |
| RN-15 | Un usuario pertenece a **una liga o a un club**, nunca a ambos ni a ninguno. | §8.1.8 |
| RN-16 | Un usuario inactivo o sin el rol requerido no accede. | HU14 CA2 |
| RN-17 | Solo puede haber **una temporada activa por liga** (`uq_temporada_activa_liga`). | HU20 CA2, §8.1.3 |
| RN-18 | Cerrar una temporada registra `fecha_cierre`, pasa a `Suspendido` los partidos aún `Programado` y la vuelve **no reabrible** (409). | HU20 CA3 |
| RN-19 | La anulación de un pago es **transaccional**: el pago pasa a `Anulado` conservando el recibo, y las cuotas vuelven a `Pendiente` o `Adeudada` según su vencimiento. | HU06 CA4 |
| RN-20 | La renovación de planteles aplica solo a fichajes `Propio`, exige **previsualización**, es **idempotente** y no aborta el lote por exclusiones. | HU21 CA2 y CA3 |
| RN-21 | El club debe habilitar sus categorías en la temporada activa antes de fichar, programar partidos o asignar cuerpo técnico. | HU21 CA1 |
| RN-22 | El cierre **no impide cobrar**: se siguen registrando `Pago` y anulaciones sobre cuotas ya emitidas, y **no** cierra los fichajes vigentes (los cierran HU21 o HU18). | HU20 CA3 |
| RN-23 | No se puede editar `anio_desde` / `anio_hasta` de una categoría que ya tenga participaciones en temporadas cerradas. | HU20 CA1 |

## 7. Decisiones de diseño vigentes

Resumidas acá; el razonamiento completo y las alternativas descartadas están en `adr/`.

1. **Padrón global por liga:** `Jugador.id_liga` con `UNIQUE (dni, id_liga)`. Todo lo administrativo, financiero
   y legal cuelga de `Fichaje_Inscripcion`, no del jugador. → `adr/0001`
2. **Temporadas y categorías a nivel liga** con `Club_Temporada_Categoria` como participación. → `adr/0002`
3. **Vigencia por `fecha_baja IS NULL`**, sin banderas booleanas redundantes. → `adr/0003`
4. **Tutores globales** ligados al jugador (`Jugador_Tutor` con vigencia), no al fichaje. → `adr/0004`
5. **5 roles en Keycloak** + alcance por categoría en PostgreSQL. → `adr/0005`
6. **BFF + microservicios + Nginx** como arquitectura de aplicación. → `adr/0006`
7. **React + Vite** como stack del frontend. → `adr/0007`
8. **Docker Compose con `keycloak_db` separada** de `dacs_db`. → `adr/0008`
9. **Notificaciones de cumpleaños con CallMeBot** encapsuladas en `ms-conector`. → `adr/0010`
10. **Ciclo de vida de temporada** con `fecha_cierre` y una única temporada activa por liga. → `adr/0011`

## 8. Arquitectura (resumen)

Tres capas de aplicación sobre una infraestructura contenerizada:

```
Navegador ──HTTPS──► Nginx ──► /dacs/  Frontend React (PWA)
                       ├────► /auth/  Keycloak (OIDC, PKCE S256)
                       └────► /api/   BFF :9001 ──► ms-backend :9003 ──► PostgreSQL 16
                                                   └─► ms-conector :9002 ──► APIs externas
```

- **Frontend:** SPA React (PWA), servida por Nginx en `/dacs/`.
- **BFF:** única API consumida por el frontend; orquesta, adapta DTO y estandariza errores. Sin persistencia.
- **ms-backend:** lógica de negocio y persistencia JPA sobre PostgreSQL. Ejecuta el job diario (`@Scheduled`) de cumpleaños que persiste las notificaciones internas.
- **ms-conector:** integración con servicios externos, incluido el envío de WhatsApp vía CallMeBot. Sin persistencia.
- **Keycloak:** identidad y emisión de JWT; roles de negocio.

El detalle operativo (puertos, contenedores, certificados, cómo levantar) está en `INFRA.md`. Los principios,
la separación de responsabilidades y el flujo de una petición autenticada están en `adr/0006`.

## 9. Cómo correr (dev)

Requisitos: Docker Desktop con contenedores Linux, OpenSSL en el PATH.

```powershell
Set-Location .\infra
Copy-Item .env.example .env      # y reemplazar las contraseñas
docker compose config --quiet
docker compose up -d postgres pgadmin keycloak
```

Antes la primera vez hay que configurar el dominio local y el certificado: ver `INFRA.md` §6. Sin el
certificado en `infra/nginx/ssl/`, el servicio `web` y las imágenes Java no construyen.

Accesos: `https://clubes.local/dacs/` (frontend), `https://clubes.local/auth/admin` (Keycloak),
`http://localhost:5050` (pgAdmin), `https://clubes.local/api/ping` (BFF).

> **Estado actual del repositorio:** existe el andamiaje de infraestructura (Compose, Nginx, Keycloak,
> PostgreSQL, pgAdmin, Dockerfiles, CI). **Todavía no hay código de aplicación**: `frontend/` no tiene
> `package.json` y los tres servicios backend no tienen `pom.xml` ni `src/`. Hasta que eso exista,
> `docker compose up -d --build` falla en los cuatro servicios de aplicación.

## 10. Convenciones del equipo

- **Commits** en español con Conventional Commits y jerga técnica en inglés. Sin `Co-Authored-By`.
- **Documentación** en español neutro/profesional; **identificadores del dominio** en español (según el informe).
- **Los cambios de reglas del club** (tarifas, categorías, temporadas) deben ser **configurables**, no
  hardcodeados.
- **Una decisión de arquitectura se registra en un ADR**, no en un comentario de código ni en el chat.
- **Las preguntas abiertas no se responden por suposición.** Ver §11.

## 11. Preguntas abiertas (no suponer)

Bloquean criterios, no la arquitectura. Toda implementación que dependa de ellas debe quedar encapsulada y
parametrizada hasta que se respondan.

1. ¿Las categorías son de un año de nacimiento o agrupan dos (sub 13)? ¿Hay tolerancia para nacidos a fin de año?
2. ¿Cuántos jugadores de categoría inferior se convocan por partido? ¿Se puede jugar en la superior y en la
   propia el mismo fin de semana?
3. ¿Cuántos cambios se permiten y hay reingreso en infantiles? ¿El límite de 7 aplica a todas las categorías?
   ¿Máximo de jugadores por planilla?
4. ¿Cómo funciona el pase de un menor: quién lo aprueba, qué firma el tutor, existe "venta" en estas categorías?
5. ¿Qué pasa cuando vence un préstamo?
6. ¿La liga exige apto médico anual? ¿Quién lo controla? ¿`fecha_apto_medico` vence por temporada o por año
   calendario?
7. ¿Cuándo vence la cuota y cuándo pasa a "Adeudada"? ¿Se inhabilita al jugador moroso?
8. ¿Cómo define el club el descuento por hermanos con tres o más? ¿Qué otras becas o exenciones existen?
9. ¿Cuál es el **formato exacto** de la planilla IAPSER y con qué frecuencia se envía? (requiere el layout oficial)
10. ¿La cobertura IAPSER se administra por fichaje (`id_fichaje`) o a nivel persona?
11. ¿Quién define el "segundo hermano" que recibe el 50 %? (menor, último registrado o elección manual)
12. ¿La deuda bloquea la emisión de fichas de pase?
13. ¿La liga organiza un **único ciclo anual** por temporada o disputa torneos semestrales/paralelos
    (Apertura y Clausura) que exijan más de una temporada activa a la vez? Condiciona la restricción
    `uq_temporada_activa_liga`.
14. ¿CallMeBot admite el uso institucional con una API key por delegado? La adopción definitiva queda sujeta a
    una prueba piloto con varios delegados (ver `adr/0010`).

El reglamento de la liga y los formularios oficiales deberían conseguirse y agregarse como anexo del informe.
