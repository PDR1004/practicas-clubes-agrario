# ADR-0001: Padrón global de jugadores por liga y el fichaje como eje

- **Estado**: Aceptada
- **Fecha**: 2026-11-23

## Contexto

RF01 y HU04 exigen que un jugador conserve su **historial deportivo** al cambiar de club, pero que su
**historial financiero, de becas, consentimientos y estados previos** quede aislado y oculto para la nueva
institución. Si el jugador pertenece a un club, cualquiera de las dos cosas se rompe: o se duplica la persona
en cada club (perdiendo el historial deportivo global), o se comparte todo (violando el aislamiento).

## Decisión

1. `Jugador` es una identidad **global de la liga**: `id_liga` + `UNIQUE (dni, id_liga)`. No tiene `id_club`.
2. Todo dato con dueño institucional cuelga de **`Fichaje_Inscripcion`**: `id_grupo`, `porcentaje_beca`,
   `consentimiento_imagen`, `nro_carnet_liga`, `tipo_fichaje`, y por FK `Estado_Jugador`, `Seguro` y `Cuota`.
3. El fichaje apunta a `Club_Temporada_Categoria`, no directamente al club: así el encuadre deportivo
   (club + temporada + categoría) queda en una sola referencia.
4. Un cambio de club crea un **fichaje nuevo** y cierra el anterior con `fecha_baja`.

## Alternativas consideradas

- **Jugador atado a un club (cada club su copia):** descartada. Es más simple, pero elimina el historial
  deportivo global exigido por RF01 y obliga a rehacer HU04 CA2. El ahorro de complejidad no compensa perder
  un requerimiento explícito.
- **Jugador global sin `id_liga`:** descartada. La plataforma es multi-liga (RF06): sin `id_liga`, "global"
  queda indefinido y el mismo DNI no puede pertenecer a dos ligas.

## Consecuencias

Positivas:

- El historial deportivo sobrevive al cambio de club; el financiero no se filtra.
- El aislamiento multi-tenant tiene un único punto de control: el fichaje.

Negativas:

- Toda consulta de padrón necesita el join por fichaje; un `findAll()` ingenuo sobre `Jugador` filtraría
  jugadores de toda la liga. Requiere predicado de tenant disciplinado en `ms-backend`.
- `Cuota` lleva `id_club` e `id_jugador` redundantes por rendimiento, con riesgo de desincronización si no se
  escriben siempre desde el fichaje.

## Referencias

- `docs/CONTEXTO.md` §7.1; `docs/DOMINIO.md` §1.2, §1.3 y §4.1–4.4.
- `docs/DB-SCHEMA.md` §3.1 y §3.2.
- Informe: §5.1, RF01, HU04 CA2.
