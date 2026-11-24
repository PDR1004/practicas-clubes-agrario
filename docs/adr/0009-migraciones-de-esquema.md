# ADR-0009: Motor de migraciones del esquema

- **Estado**: Propuesta
- **Fecha**: 2026-11-23

## Contexto

El repositorio no tiene migraciones ni configuración de `ddl-auto`. `ms-backend` usará Spring Data JPA sobre
PostgreSQL 16 y el esquema ya está diseñado (`docs/DB-SCHEMA.md`), pero nada define todavía cómo se crea y
evoluciona. Esta decisión hay que tomarla **antes de la primera tabla**: cambiar de estrategia después implica
rehacer el historial.

## Decisión propuesta

1. **Flyway** con migraciones versionadas en
   `backend/dacs-backend/src/main/resources/db/migration/` (`V1__esquema_inicial.sql`, …).
2. `spring.jpa.hibernate.ddl-auto: validate` en **todos** los entornos.
3. El esquema se cambia **solo** con una migración nueva; nunca editando una migración ya aplicada.

## Alternativas consideradas

- **Liquibase:** equivalente en capacidades. Se propone Flyway por menor ceremonia con SQL plano y porque el
  modelo ya está expresado como DDL en `DB-SCHEMA.md`.
- **`ddl-auto: update`:** descartado. No es determinista, no versiona el cambio y produce diferencias entre
  máquinas.
- **Scripts manuales aplicados a mano:** descartado. No es reproducible y no deja historial.

## Consecuencias

- Positivas: historial reproducible, CI puede validar el esquema y `validate` detecta divergencias entre
  entidades y base.
- Negativas: cada cambio de entidad requiere escribir el DDL a mano; el equipo debe conocer Flyway.
- Riesgo si no se decide: el esquema se crea con `update`, las entidades divergen y `validate` ya no se puede
  activar sin una limpieza manual.

## Referencias

- `docs/DB-SCHEMA.md` §7.
- `backend/dacs-backend/Dockerfile`, `backend/dacs-backend/pom.xml` (todavía inexistente).
