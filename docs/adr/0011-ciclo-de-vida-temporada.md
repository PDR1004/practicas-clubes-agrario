# ADR-0011: Ciclo de vida de temporada

- **Estado**: Aceptada
- **Fecha**: 2026-11-23

## Contexto

HU20 introduce el gobierno del ciclo deportivo: definir categorías, abrir una temporada y cerrarla. Antes de
esta historia, `Temporada` solo tenía `activa` (boolean), lo que alcanza para decir "está abierta" pero no para
distinguir una temporada **planificada** de una **cerrada**. Además, cerrar una temporada no puede habilitar
escrituras posteriores en partidos, fichajes o cuotas de ese ciclo.

## Decisión

1. **Una sola temporada activa por liga**, impuesta por la base:

   ```sql
   CREATE UNIQUE INDEX uq_temporada_activa_liga ON temporada (id_liga) WHERE activa;
   ```

2. **`fecha_cierre` (date, nullable)** completa el estado sin agregar un enum:

   | `activa` | `fecha_cierre` | Estado |
   |---|---|---|
   | `true` | `NULL` | Activa |
   | `false` | `NULL` | Planificada |
   | `false` | fecha | Cerrada |

3. **Coherencia garantizada:** `CHECK (NOT (activa AND fecha_cierre IS NOT NULL))`.
4. **El cierre no borra**: conserva participaciones, fichajes, partidos y cuotas del ciclo.
5. **El bloqueo de escritura es invariante de aplicación**: toda creación de `partido`, `fichaje_inscripcion` o
   `cuota` resuelve la temporada vía `club_temporada_categoria` y rechaza si la temporada está cerrada. No se
   puede expresar como constraint porque cruza varias tablas.
6. **Actores separados:** el `SUPERADMIN` abre y cierra la temporada de la liga; el `ADMIN_CLUB` habilita las
   categorías de su club y ejecuta la renovación de planteles (HU21).

## Alternativas consideradas

- **Enum `estado_temporada` (Planificada/Activa/Finalizada):** descartada por ahora. Aporta lo mismo que
  `activa` + `fecha_cierre` y agrega una migración de tipo y mapeos extra. Si aparecen más estados (por ejemplo,
  "suspendida"), se evalúa.
- **`activa` solo:** descartada. No distingue una temporada planificada de una cerrada, y el cierre quedaría sin
  registro temporal.
- **Unicidad a nivel club:** descartada. `Temporada` pertenece a la liga (`adr/0002`); el club participa a través
  de `Club_Temporada_Categoria`.
- **Bloqueo por trigger en base:** descartada. Exigiría replicar en SQL la lógica de resolución de temporada y
  haría los errores más difíciles de traducir a respuestas de negocio.

## Consecuencias

Positivas:

- Un estado de temporada inequívoco y auditable (`fecha_cierre`).
- La base impide tener dos temporadas activas por liga.
- Abrir una temporada no toca los fichajes de nadie: la renovación es un caso de uso aparte, con previsualización.

Negativas:

- El bloqueo depende de que **cada** caso de uso de escritura valide la temporada; es fácil olvidar uno. Debe
  cubrirse con tests.
- El índice asume **un ciclo anual por liga**. Si la liga corre Apertura y Clausura, hace falta un nivel
  `torneo` o relajar la restricción (pregunta abierta 13 del `CONTEXTO.md`).

## Enmienda (2026-11-23)

La revisión 3 del informe cerró los bordes de HU20/HU21. Esta enmienda los incorpora sin reescribir la decisión
original:

1. **El cierre tiene efectos concretos**, en la misma transacción: pasa a `Suspendido` los partidos que sigan en
   `Programado`, registra `fecha_cierre` y deja la temporada inmutable (reabrir → 409).
2. **Cobrar sigue permitido** después del cierre: `Pago` y las anulaciones sobre cuotas ya emitidas no se bloquean.
   Sin esto, la deuda histórica sería incobrable.
3. **El cierre no cierra fichajes**: los `Fichaje_Inscripcion` vigentes siguen abiertos hasta la renovación de
   planteles (HU21) o una baja lógica (HU18).
4. **La renovación solo aplica a `tipo_fichaje = Propio`**; `A_Prestamo` y `Libre` se informan como exclusión
   para gestión manual, porque un préstamo no se renueva solo.
5. **El rango etario de una categoría es inmutable** si ya tiene participaciones en temporadas cerradas.

## Referencias

- `docs/CONTEXTO.md` §6 (RN-17 a RN-23), §7.10 y §11.13.
- `docs/DOMINIO.md` §1.1 y §4.15–4.16; `docs/DB-SCHEMA.md` §3.1 y §3.6.
- `docs/API-CONTRATO.md` §5.1 y §7.9–7.10.
- Informe: HU20, HU21, §8.1.3.
