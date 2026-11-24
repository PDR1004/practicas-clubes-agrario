# ADR-0004: Tutores globales ligados al jugador, no al fichaje

- **Estado**: Aceptada
- **Fecha**: 2026-11-23

## Contexto

Al mover todo lo administrativo y financiero al fichaje (ADR-0001), surgió la pregunta de si `Jugador_Tutor`
también debía colgar de `Fichaje_Inscripcion` para reforzar la privacidad entre clubes. La intuición es
razonable: los datos de contacto del tutor de un menor no deberían ser visibles para un club ajeno.

## Decisión

1. `Tutor` es **global** (sin `id_club`), con `dni` único.
2. `Jugador_Tutor` cuelga de **`Jugador`**, con vigencia `fecha_desde` / `fecha_hasta` para modelar cambios de
   responsable legal en el tiempo. Máximo dos **vigentes** (`fecha_hasta IS NULL`).
3. La privacidad se resuelve por **autorización**, no por esquema: `ms-backend` solo devuelve tutores cuando el
   jugador tiene un fichaje vigente en el club del usuario autenticado.

## Alternativas consideradas

- **`Jugador_Tutor` colgado del fichaje:** descartada. Rompe HU12: la ficha de pase necesita los datos del
  tutor, y al cerrar el fichaje por la transferencia se perdería el vínculo justo cuando hay que emitirla.
  Además duplica el mismo hecho en cada inscripción y permite que dos clubes tengan datos distintos del mismo
  adulto (un teléfono viejo en uno, el nuevo en otro).
- **`Tutor` con `id_club`:** descartada. Un adulto con hijos en dos clubes generaría dos tutores para la misma
  persona, y al transferirse el jugador el `Jugador_Tutor` seguiría apuntando al tutor del club anterior.

## Consecuencias

Positivas:

- Un solo registro por adulto; los datos se actualizan una vez.
- Los documentos oficiales (fichas de inscripción y de pase) siempre tienen el tutor disponible.

Negativas:

- La privacidad depende de que **toda** consulta de tutor incluya el predicado de tenant. Es una regla de
  aplicación, no una restricción de base: hay que testearla.
- Si se necesita que cada club tenga su propia copia de contacto (por ejemplo, un teléfono distinto por
  institución), hay que agregar una tabla de contacto por fichaje. Hoy no es un requisito.

## Referencias

- `docs/DOMINIO.md` §1.2 y §4.3; `docs/DB-SCHEMA.md` §3.1 y §3.6.
- Informe: §5.1, HU03, HU12, RNF03.
