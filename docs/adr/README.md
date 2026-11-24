# ADRs — Registro de decisiones de arquitectura

Un **ADR** (Architecture Decision Record) documenta una decisión: el contexto en el que se tomó, qué se
decidió, qué alternativas se evaluaron y qué consecuencias tiene. Sirve para que cualquier integrante entienda
*por qué* el sistema es como es, sin depender de la memoria del equipo ni del historial del chat.

## Formato

- Título `# ADR-000N: Título`
- **Estado**: Aceptada | Propuesta | Reemplazada
- **Fecha**
- **Contexto**
- **Decisión**
- **Alternativas consideradas** (con el motivo del descarte)
- **Consecuencias** (positivas y negativas)
- **Referencias**

Convenciones:

- Nombre de archivo: `NNNN-titulo-en-minusculas-con-guiones.md`.
- Una decisión por archivo; **no se edita el contenido de una decisión pasada**.
- Si una decisión se reemplaza, se crea un ADR nuevo que referencia al anterior, y el anterior pasa a estado
  *Reemplazada*.
- Las decisiones que todavía no se tomaron se registran como **Propuesta**.

## Índice

| Número | Título | Estado | Fecha |
|---|---|---|---|
| [ADR-0001](0001-padron-global-por-liga.md) | Padrón global de jugadores por liga y el fichaje como eje | Aceptada | 2026-11-23 |
| [ADR-0002](0002-temporadas-categorias-nivel-liga.md) | Temporadas y categorías a nivel liga | Aceptada | 2026-11-23 |
| [ADR-0003](0003-vigencia-por-fecha-baja.md) | Vigencia por `fecha_baja IS NULL`, sin banderas booleanas | Aceptada | 2026-11-23 |
| [ADR-0004](0004-tutores-globales.md) | Tutores globales ligados al jugador, no al fichaje | Aceptada | 2026-11-23 |
| [ADR-0005](0005-roles-keycloak-alcance-db.md) | 5 roles en Keycloak y alcance por categoría en PostgreSQL | Aceptada | 2026-11-23 |
| [ADR-0006](0006-arquitectura-bff-microservicios.md) | Arquitectura BFF + microservicios + Nginx | Aceptada | 2026-11-23 |
| [ADR-0007](0007-frontend-react-vite.md) | Frontend React + Vite | Aceptada | 2026-11-23 |
| [ADR-0008](0008-compose-keycloak-db-separada.md) | Docker Compose con base de Keycloak separada | Aceptada | 2026-11-23 |
| [ADR-0009](0009-migraciones-de-esquema.md) | Motor de migraciones del esquema | Propuesta | 2026-11-23 |
| [ADR-0010](0010-notificaciones-cumpleanos-callmebot.md) | Notificaciones de cumpleaños con CallMeBot | Propuesta | 2026-11-23 |
| [ADR-0011](0011-ciclo-de-vida-temporada.md) | Ciclo de vida de temporada | Aceptada | 2026-11-23 |

## Referencias cruzadas

- `docs/CONTEXTO.md` §7 resume las decisiones; este registro tiene el razonamiento.
- `docs/DOMINIO.md` §4 traduce las decisiones a reglas de implementación.
- `docs/DB-SCHEMA.md` §3 materializa las restricciones derivadas de ADR-0001 y ADR-0003.
- `docs/API-CONTRATO.md` §2 deriva la autorización de ADR-0005.
- `docs/INFRA.md` §4 y §5 reflejan ADR-0008.
