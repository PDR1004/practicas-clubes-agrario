# ADR-0005: 5 roles en Keycloak y alcance por categoría en PostgreSQL

- **Estado**: Aceptada
- **Fecha**: 2026-11-23

## Contexto

El informe tenía dos versiones: RF05 y las HU listaban **5 roles separados** (Superadmin, Administrador de
Club, Tesorero, Delegado, Entrenador), mientras que la sección de perfiles agrupaba Administrador y Tesorero en
uno. Además, el alcance real de un Delegado o un Entrenador es **por categoría y por temporada**, no global al
club.

## Decisión

1. **Cinco roles canónicos** en el realm de Keycloak: `SUPERADMIN`, `ADMIN_CLUB`, `TESORERO`, `DELEGADO`,
   `ENTRENADOR`.
2. Keycloak es la **fuente de verdad de autorización**. `Rol` y `Usuario_Rol` en PostgreSQL existen solo para el
   historial de cargos y como catálogo espejo; **no** autorizan.
3. El **alcance por categoría no se modela como rol**: vive en `Usuario_Categoria` (usuario →
   `Club_Temporada_Categoria`) y se resuelve en cada request.
4. En Spring Security los roles se transforman a `ROLE_<ROL>` para usar `hasRole(...)` de forma uniforme en BFF
   y `ms-backend`.
5. `id_club` e `id_liga` viajan como claims del JWT (protocol mappers, TTL corto). El alcance por categoría
   nunca viaja en el token.

## Alternativas consideradas

- **4 roles, agrupando Administrador y Tesorero:** descartada. Las HU distinguen responsabilidades (HU06 es del
  Tesorero; HU01/HU02 del Administrador) y agruparlas obliga a revisar permisos con un `if` de contexto.
- **Roles por categoría en Keycloak** (por ejemplo `DELEGADO_2010`): descartada por explosión combinatoria:
  roles × temporadas × categorías, con sincronización permanente y un realm ilegible.
- **Autorizar leyendo `Usuario_Rol`:** descartada. Sería una segunda fuente de verdad frente al IdP y los
  permisos podrían quedar desincronizados.

## Consecuencias

Positivas:

- Cinco roles estables; el alcance fino se expresa como dato.
- Un cambio de categoría asignada no requiere tocar Keycloak.

Negativas:

- Cada request con alcance por categoría necesita una consulta a `Usuario_Categoria` (cacheable).
- Si el claim `id_club` queda desactualizado por un TTL largo, el usuario podría operar con el club anterior;
  por eso el TTL es corto.
- Hay que sostener un test que verifique que el catálogo `Rol` coincide con los roles del realm (permisos
  fantasma).

## Referencias

- `docs/CONTEXTO.md` §3 y §7.5; `docs/DOMINIO.md` §1.6 y §4.13–4.14.
- `docs/API-CONTRATO.md` §2.
- Informe: RF05, HU14, HU15, §7, §9.6.
