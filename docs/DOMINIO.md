# Club Agrario Rocamora — Modelo de dominio

> Fuente: §8 (ERD) y §5 (decisiones de diseño) del informe técnico.
> Este documento describe las **entidades y sus invariantes**. El mapeo físico a tablas, columnas,
> índices y SQL está en `DB-SCHEMA.md`.
> Los nombres de entidades y atributos se mantienen en el español del informe: son también los nombres
> de las tablas.

## 1. Entidades por módulo

### 1.1 Liga, club y participación deportiva

| Entidad | Atributos | Notas |
|---|---|---|
| **Liga** | `id_liga`, `nombre`, `provincia`, `activa` | Tenant raíz del sistema |
| **Club** | `id_club`, `id_liga`, `nombre`, `cuit`, `domicilio`, `escudo_url`, `color_primario`, `color_secundario`, `activo` | `cuit` único. Es el tenant operativo: todo dato de negocio se aísla por `id_club` |
| **Temporada** | `id_temporada`, `id_liga`, `anio`, `fecha_inicio`, `fecha_fin`, `activa`, `fecha_cierre` | Pertenece a la **liga**, no al club. `activa` = única temporada abierta por liga; `fecha_cierre` distingue *planificada* de *cerrada* |
| **Categoria** | `id_categoria`, `id_liga`, `anio_desde`, `anio_hasta`, `nombre` | Rango inclusivo de años de nacimiento. `anio_desde <= anio_hasta` |
| **Club_Temporada_Categoria** | `id_club_temporada_categoria`, `id_club`, `id_temporada`, `id_categoria`, `activa` | Participación concreta de un club en una categoría de una temporada. **Es el ancla deportiva del sistema** |

### 1.2 Padrón global y tutores

| Entidad | Atributos | Notas |
|---|---|---|
| **Jugador** | `id_jugador`, `id_liga`, `dni`, `nombre`, `apellido`, `fecha_nacimiento`, `lugar_nacimiento`, `domicilio`, `foto_url`, `fecha_apto_medico` | **Global por liga**: identidad deportiva de la persona. No tiene `id_club` |
| **Tutor** | `id_tutor`, `dni`, `nombre`, `apellido`, `telefono`, `domicilio` | **Global**: un adulto puede tener varios hijos, incluso en clubes distintos |
| **Jugador_Tutor** | `id_jugador`, `id_tutor`, `vinculo`, `fecha_desde`, `fecha_hasta` | Historial de responsables legales. Máximo 2 **vigentes** (`fecha_hasta IS NULL`) |

### 1.3 Fichaje y sus derivados (ámbito club)

| Entidad | Atributos | Notas |
|---|---|---|
| **Grupo_Familiar** | `id_grupo`, `id_club`, `apellido_familia`, `observaciones` | Agrupación administrativa **del club** para la política de hermanos |
| **Fichaje_Inscripcion** | `id_fichaje`, `id_club_temporada_categoria`, `id_jugador`, `id_grupo`, `tipo_fichaje`, `porcentaje_beca`, `consentimiento_imagen`, `nro_carnet_liga`, `fecha_alta`, `fecha_baja` | **Entidad central del modelo.** Todo dato administrativo, financiero y legal cuelga de acá |
| **Estado_Jugador** | `id_estado_jugador`, `id_fichaje`, `id_usuario`, `fecha_hora`, `valor`, `motivo` | Bitácora **append-only** de estados del fichaje. No es la fuente de vigencia |
| **Pase** | `id_pase`, `id_fichaje`, `id_club_cedente`, `id_club_cesionario`, `club_cedente_nombre`, `club_cesionario_nombre`, `fecha_inicio`, `fecha_fin` | Registro de la **transferencia real**. Opcional por fichaje (0..1) |
| **Seguro** | `id_seguro`, `id_fichaje`, `fecha_alta`, `fecha_vencimiento`, `nro_poliza` | Historial de pólizas IAPSER por fichaje. El estado "Asegurado SÍ/NO" es **derivado**, no una columna |

### 1.4 Partidos, convocatoria y estadísticas

| Entidad | Atributos | Notas |
|---|---|---|
| **Partido** | `id_partido`, `id_club_temporada_categoria`, `fecha_hora`, `rival`, `condicion`, `cancha`, `estado`, `gol_local`, `gol_visitante` | `rival` es texto libre (ver §4.8) |
| **Convocatoria_Partido** | `id_partido`, `id_jugador`, `dorsal`, `goles`, `tarjetas_amarillas`, `tarjeta_roja` | Único por `(id_partido, id_jugador)` y por `(id_partido, dorsal)` |
| **Cambio_Partido** | `id_cambio`, `id_partido`, `id_jugador_sale`, `id_jugador_entra`, `minuto` | Máximo 7 por partido (RN-07) |

### 1.5 Finanzas

| Entidad | Atributos | Notas |
|---|---|---|
| **Pago** | `id_pago`, `id_club`, `id_usuario_cobro`, `nro_recibo`, `fecha_pago`, `medio_pago`, `monto_total`, `estado`, `motivo_anulacion` | Un pago cubre 1..N cuotas (cobro acumulado). `nro_recibo` único por club |
| **Cuota** | `id_cuota`, `id_club`, `id_jugador`, `id_fichaje`, `id_valor_cuota`, `id_pago`, `periodo`, `fecha_vencimiento`, `monto_base`, `descuento_aplicado`, `estado` | El vínculo canónico es `id_fichaje`; `id_club` e `id_jugador` son redundancia intencional |
| **Valor_Cuota** | `id_valor_cuota`, `id_club`, `precio`, `fecha_vigencia` | Tarifas versionadas por club |
| **Estado_Cuota** | `id_estado_cuota`, `id_cuota`, `fecha_hora`, `valor_estado` | Bitácora de **auditoría**; `Cuota.estado` es el estado vigente |

### 1.6 Seguridad, notificaciones y roles

| Entidad | Atributos | Notas |
|---|---|---|
| **Usuario** | `id_usuario`, `id_liga`, `id_club`, `keycloak_sub`, `username`, `email`, `nombre`, `apellido`, `telefono`, `dni`, `activo` | `keycloak_sub` es el vínculo con el IdP. Alcance excluyente liga **o** club |
| **Usuario_Rol** | `id_usuario`, `id_rol`, `fecha_inicio`, `fecha_fin` | Historial de cargos. `fecha_fin` nula = cargo vigente |
| **Usuario_Categoria** | `id_usuario`, `id_club_temporada_categoria`, `rol_en_categoria` | Alcance operativo por categoría y temporada (DELEGADO / ENTRENADOR) |
| **Rol** | `id_rol`, `nombre` | Catálogo **espejo** de los roles de Keycloak, solo para el historial |
| **Notificacion** | `id_notificacion`, `id_club`, `id_usuario`, `id_jugador`, `tipo_notificacion`, `titulo`, `mensaje`, `fecha_creacion`, `leida` | Avisos internos del dashboard |

## 2. Enumeradores

| Enum | Valores | Dónde se usa |
|---|---|---|
| `Tipo_Fichaje` | `Propio`, `A_Prestamo`, `Libre` | `Fichaje_Inscripcion.tipo_fichaje` |
| `Partido_Condicion` | `Local`, `Visitante` | `Partido.condicion` |
| `Partido_Estado` | `Programado`, `Jugado`, `Suspendido` | `Partido.estado` |
| `Estado_Cuota_Valor` | `Pendiente`, `Pagada`, `Adeudada`, `Anulada` | `Cuota.estado`, `Estado_Cuota.valor_estado` |
| `Tipo_Notificacion` | `Cumpleanios`, `Sistema` | `Notificacion.tipo_notificacion` |
| `Tipo_Vinculo` | `Padre`, `Madre`, `Tutor` | `Jugador_Tutor.vinculo` |
| `Medio_Pago` | `Efectivo`, `Transferencia` | `Pago.medio_pago` |

Estados **no** modelados como enum del informe pero exigidos por §8.1 (ver `DB-SCHEMA.md` §3):

- `Estado_Jugador.valor`: `Activo` | `Inactivo`.
- `Pago.estado`: `Registrado` | `Anulado`.
- Roles: `SUPERADMIN`, `ADMIN_CLUB`, `TESORERO`, `DELEGADO`, `ENTRENADOR`.

## 3. Relaciones clave

- **Liga** 1→N **Club**, **Temporada**, **Categoria**, **Jugador**, **Usuario**.
- **Club** y **Temporada** y **Categoria** se cruzan en **Club_Temporada_Categoria**, que es lo que consumen
  **Fichaje_Inscripcion**, **Partido** y **Usuario_Categoria**.
- **Jugador** 1→N **Fichaje_Inscripcion** (historial entre clubes) y 1→N **Jugador_Tutor**.
- **Fichaje_Inscripcion** 1→N **Estado_Jugador**, 1→N **Seguro**, 1→N **Cuota**, 1→0..1 **Pase**.
- **Cuota** N→1 **Fichaje_Inscripcion** y N→0..1 **Pago** (un pago agrupa varias cuotas).
- **Partido** 1→N **Convocatoria_Partido** y 1→N **Cambio_Partido**.
- **Usuario** 1→N **Usuario_Rol** (historial de cargos) y 1→N **Usuario_Categoria** (alcance por categoría).

## 4. Notas de arquitectura para el agente

Estas son las aclaraciones que evitan errores de implementación. Si una decisión no está acá ni en `adr/`,
**no la inventes**: preguntá.

1. **El fichaje es el eje, no el jugador.** `Jugador` es una identidad deportiva de la liga; todo lo que tiene
   dueño institucional (beca, grupo familiar, carnet, consentimiento de imagen, estado, seguro, cuotas) cuelga
   de `Fichaje_Inscripcion`. Un jugador que cambia de club genera un **fichaje nuevo**, no reutiliza el anterior.
2. **`fecha_baja IS NULL` es el único predicado de vigencia.** No existe `is_actual` ni bandera equivalente.
   `Estado_Jugador` es auditoría (quién, cuándo, por qué); no se usa para filtrar el padrón activo.
3. **`Jugador_Tutor` cuelga de `Jugador`, no de `Fichaje_Inscripcion`.** Moverlo al fichaje rompería HU12: la
   ficha de pase necesita los datos del tutor, y al cerrar el fichaje se perdería el vínculo. La privacidad
   entre clubes se resuelve con **autorización** (verificar fichaje vigente en el club), no moviendo la tabla.
4. **`Cuota.id_club` e `id_jugador` son redundancia intencional**, documentada. El vínculo que manda para
   trazabilidad y métricas por categoría es `id_fichaje`. No los uses como fuente de verdad del tenant.
5. **`tipo_fichaje` vive en el fichaje; `Pase` solo registra la transferencia real.** Una inscripción `Libre` o
   `Propio` no necesita fila en `Pase`.
6. **No existe una "división" estática.** La etiqueta Infantil/Juvenil/Mayor depende de la temporada en curso y
   se resuelve con `Categoria.nombre`; no se persiste.
7. **`Categoria` usa rango de años**, no un año suelto: `anio_desde`/`anio_hasta` soportan tanto categorías de un
   año como agrupaciones de dos, sin migración futura.
8. **`Partido.rival` es texto libre** porque el alcance no incluye gestión centralizada de encuentros.
   Consecuencia aceptada: no se puede calcular tabla de posiciones ni correlacionar el mismo partido entre clubes.
9. **`Convocatoria_Partido` referencia `id_jugador`, no `id_fichaje`.** Es correcto: HU09 permite convocar
   jugadores de categorías inferiores, que no comparten el `Club_Temporada_Categoria` del partido. La validación
   de que el jugador pertenece al club del partido es una invariante de aplicación (RN-12 de §8.1), no una FK.
10. **`Cambio_Partido` no tiene `id_club`**: hereda el tenant vía `Partido`. La validación de que ambos
    jugadores estén convocados también es de aplicación.
11. **`Notificacion` no guarda categoría**: los destinatarios (Delegado de la categoría, Administrador) se
    resuelven al generar la notificación, consultando `Usuario_Categoria`.
12. **`Pase` no tiene estado.** Si la liga exige aprobación, hay que agregar una columna y un flujo; hoy es una
    pregunta abierta (§11 del `CONTEXTO.md`). No lo agregues preventivamente.
13. **`Rol` no autoriza.** Keycloak es la fuente de verdad de autorización; `Rol` y `Usuario_Rol` existen para el
    historial de cargos y para el catálogo espejo. Ninguna decisión de permisos se toma leyendo `Rol`.
14. **El alcance por categoría no va a Keycloak.** Se resuelve con `Usuario_Categoria` en cada request; meterlo
    como rol del IdP produciría una explosión combinatoria (ver `adr/0005`).
15. **La temporada tiene tres estados, no dos.** `activa = true` es la única temporada abierta de la liga;
    `activa = false` + `fecha_cierre IS NULL` es una temporada *planificada*; `activa = false` +
    `fecha_cierre IS NOT NULL` es una temporada *cerrada*. Cerrar no borra nada: suspende los partidos aún
    `Programado`, impide crear nuevos `Partido`, `Fichaje_Inscripcion` y `Cuota` de ese ciclo, **permite seguir
    cobrando y anulando pagos** de cuotas ya emitidas, **no cierra los fichajes vigentes** y **no se puede
    deshacer** (409). Ver `adr/0011`.
16. **La renovación de planteles es un caso de uso, no un efecto de abrir la temporada.** Aplica **solo a
    `tipo_fichaje = Propio`**, conserva `id_grupo` y `porcentaje_beca`, exige previsualización, corre en una
    transacción atómica y es idempotente. Los fichajes `A_Prestamo` / `Libre`, los jugadores sin categoría
    habilitada y los que quedan fuera de rango se informan para revisión manual, sin abortar el lote.
17. **El rango etario de una categoría es inmutable cuando ya tiene historia.** Si la categoría tiene
    participaciones en temporadas cerradas, solo se puede editar el nombre: cambiar `anio_desde`/`anio_hasta`
    reescribiría a qué categoría perteneció cada jugador.
18. **CallMeBot no tiene dónde guardar sus credenciales.** §5.4 exige una API key personal por delegado
    almacenada en el servidor, pero el ERD no tiene columna ni tabla para eso (`Usuario` solo tiene `telefono`).
    **Hueco abierto:** definir si va como `usuario.whatsapp_api_key` cifrada o en una tabla `canal_whatsapp`.
    Hasta entonces, no implementar el envío automático asumiendo un almacenamiento inexistente.

## 5. Trazabilidad historia de usuario → entidades

| HU | Entidades |
|---|---|
| HU01 Alta de institución | `liga`, `club`, `usuario`, `rol`, `usuario_rol` |
| HU02 Personalización | `club` |
| HU03 Registro de jugador y tutores | `jugador`, `tutor`, `jugador_tutor`, `grupo_familiar`, `club_temporada_categoria`, `fichaje_inscripcion`, `estado_jugador` |
| HU04 Fichaje y pase | `fichaje_inscripcion`, `pase`, `club_temporada_categoria`, `jugador` |
| HU05 Seguro IAPSER | `seguro`, `fichaje_inscripcion` |
| HU06 Cobro de cuota | `pago`, `cuota`, `estado_cuota`, `usuario` |
| HU07 Descuentos | `grupo_familiar`, `jugador_tutor`, `fichaje_inscripcion`, `cuota` |
| HU08 Planilla de partido | `partido`, `club_temporada_categoria`, `convocatoria_partido`, `jugador`, `usuario_categoria`, `club` |
| HU09 Convocatoria | `partido`, `convocatoria_partido`, `fichaje_inscripcion`, `seguro` |
| HU10 Estadísticas | `partido`, `convocatoria_partido`, `cambio_partido` |
| HU11 Exportación IAPSER | `jugador`, `fichaje_inscripcion`, `club_temporada_categoria`, `seguro` |
| HU12 Fichas de trámites | `jugador`, `tutor`, `jugador_tutor`, `fichaje_inscripcion`, `pase`, `club` |
| HU13 Alertas | `notificacion`, `jugador`, `usuario`, `usuario_categoria` |
| HU14 Ingreso vía Keycloak | `usuario`, `club`, `liga` |
| HU15 Gestión de usuarios y roles | `usuario`, `usuario_categoria`, `club_temporada_categoria`, `rol`, `usuario_rol` |
| HU16 Fixture | `partido`, `club_temporada_categoria` |
| HU17 Generación de cuotas | `cuota`, `valor_cuota`, `estado_cuota`, `fichaje_inscripcion` |
| HU18 Baja lógica | `jugador`, `fichaje_inscripcion`, `estado_jugador`, `usuario` |
| HU19 Lista de Buena Fe | `club`, `temporada`, `categoria`, `club_temporada_categoria`, `fichaje_inscripcion`, `jugador`, `seguro` |
| HU20 Gestión de temporadas y categorías | `liga`, `temporada`, `categoria`, `club_temporada_categoria` |
| HU21 Participación y renovación de planteles | `club`, `temporada`, `categoria`, `club_temporada_categoria`, `fichaje_inscripcion`, `jugador` |
