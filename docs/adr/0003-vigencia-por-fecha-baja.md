# ADR-0003: Vigencia por `fecha_baja IS NULL`, sin banderas booleanas

- **Estado**: Aceptada
- **Fecha**: 2026-11-23

## Contexto

El modelo original de fichajes tenía `is_actual` (boolean) **y** `fecha_baja` (date) para representar lo mismo.
Eso es doble fuente de verdad: tarde o temprano quedan dos fichajes "actuales" del mismo jugador, o ninguno, y
el padrón miente. El mismo patrón aparecía en otros lugares (estado del jugador con historia + bandera).

## Decisión

1. La vigencia de un fichaje es exactamente **`fecha_baja IS NULL`**. Se elimina `is_actual`.
2. La base garantiza "un solo fichaje vigente por jugador" con un **índice único parcial**:

   ```sql
   CREATE UNIQUE INDEX uq_fichaje_vigente
     ON fichaje_inscripcion (id_jugador)
     WHERE fecha_baja IS NULL;
   ```

3. `Estado_Jugador` es **bitácora de auditoría** (quién, cuándo, motivo); no se usa como predicado de padrón.
4. Se aplica la misma idea en cuotas: `Cuota.estado` es la columna canónica y `Estado_Cuota` es la bitácora;
   ambos se escriben en la misma transacción.
5. Invariante transaccional: registrar `Estado_Jugador` con `valor = 'Inactivo'` debe escribir
   `fichaje_inscripcion.fecha_baja` en el mismo commit.

## Alternativas consideradas

- **Mantener `is_actual`:** descartada. La base no puede impedir estados incoherentes y el filtro depende de
  que dos escrituras se hagan siempre juntas.
- **Derivar la vigencia solo de `Estado_Jugador`:** descartada. Obliga a un lateral join en cada consulta del
  padrón y deja la unicidad sin garantía en la base.

## Consecuencias

Positivas:

- La base impide el estado inválido por diseño, no por disciplina.
- Consultas de padrón con un predicado simple e indexado.

Negativas:

- Un índice único parcial no es portable a otros motores (no es requisito hoy).
- La invariante de baja lógica sigue siendo responsabilidad de la aplicación: la base no puede forzar que un
  `Inactivo` cierre el fichaje.

## Referencias

- `docs/DOMINIO.md` §4.2; `docs/DB-SCHEMA.md` §3.2 y §3.6.
- Informe: §8.1.2, §8.1.11, HU18.
