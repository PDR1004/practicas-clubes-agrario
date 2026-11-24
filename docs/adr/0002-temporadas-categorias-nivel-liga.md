# ADR-0002: Temporadas y categorías a nivel liga

- **Estado**: Aceptada
- **Fecha**: 2026-11-23

## Contexto

La liga define la competencia: las categorías (por año de nacimiento) y las temporadas son comunes a todas las
instituciones afiliadas. Un club participa en algunas categorías y no en otras. Si cada club tuviera su propia
temporada y categoría, la misma "2010" existiría N veces, sin forma de comparar ni consolidar.

Queda además una pregunta abierta del club: si una categoría agrupa uno o dos años de nacimiento (sub 13, por
ejemplo). El modelo no debe requerir una migración cuando se responda.

## Decisión

1. `Temporada` y `Categoria` pertenecen a `Liga` (`id_liga`), no a `Club`.
2. La participación de un club en una categoría de una temporada se modela con
   **`Club_Temporada_Categoria`**, única por `(id_club, id_temporada, id_categoria)`.
3. `Categoria` guarda un **rango inclusivo**: `anio_desde` / `anio_hasta`, con `check (anio_desde <= anio_hasta)`.
   Un año suelto es `2010–2010`; una agrupación es, por ejemplo, `2010–2011`.
4. **No se persiste una "división"** (Infantil / Juvenil / Mayor). La etiqueta depende de la temporada en curso,
   no de la categoría por año de nacimiento; se resuelve con `Categoria.nombre` y, si un reporte necesita
   agrupar, es una vista.

## Alternativas consideradas

- **Temporada y categoría por club:** descartada. Duplica entidades de la liga y hace imposible consolidar
  entre clubes.
- **`Categoria.anio_nacimiento` (un solo año):** descartada. Bloquea la respuesta "agrupa dos años" y obliga a
  migrar el esquema por una pregunta de negocio pendiente.
- **`Categoria.division` como enum (Infantil/Juvenil/Mayor):** descartada. Además de no ser un atributo estable
  (caduca con la temporada), un enum obliga a una migración cada vez que la liga renombre una división.

## Consecuencias

Positivas:

- Una sola definición de categoría y temporada para toda la liga.
- El rango de años cubre ambos escenarios sin migración futura.

Negativas:

- Todo dato deportivo necesita dos saltos: club → `club_temporada_categoria` → categoría/temporada.
- La etiqueta de división no está en la base: los reportes que la necesiten dependen de una regla de
  presentación.

## Referencias

- `docs/CONTEXTO.md` §7.2; `docs/DOMINIO.md` §1.1 y §4.6–4.7.
- `docs/DB-SCHEMA.md` §3.1.
- Informe: §5.2, RF06.
