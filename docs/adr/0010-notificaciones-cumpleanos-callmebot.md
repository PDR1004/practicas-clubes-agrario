# ADR-0010: Notificaciones de cumpleaños con CallMeBot

- **Estado**: Propuesta
- **Fecha**: 2026-11-23

## Contexto

RF07 pide avisar los cumpleaños de jugadores activos. El club ya usa WhatsApp como canal real de comunicación,
y el delegado de cada categoría es quien saluda al grupo. Una notificación que solo vive dentro de la aplicación
depende de que el delegado entre a mirarla.

## Decisión

1. **Canal principal (fuente de verdad):** un job diario `@Scheduled` en `ms-backend` detecta los cumpleaños y
   persiste la notificación en `notificacion` (tipo `Cumpleanios`). Es lo que se muestra en la campanita del
   dashboard, con el saludo institucional redactado.
2. **Acción "Compartir por WhatsApp" en el frontend:** arma un enlace oficial `https://wa.me/?text=…` con el
   mensaje listo. No consume API ni depende de terceros.
3. **Canal complementario (*best-effort*):** `ms-conector` envía un mensaje individual al WhatsApp del delegado
   usando **CallMeBot**, con su **número y API key personal**. `ms-backend` no conoce al proveedor.
4. **Las credenciales nunca salen del servidor**: no viajan al frontend ni al repositorio.
5. **El fallo del canal automático no afecta al principal**: la notificación interna ya está persistida.

## Alternativas consideradas

- **WhatsApp Business API / Meta Cloud API:** descartada para el alcance de la práctica. Requiere verificación
  de negocio, plantillas aprobadas y costos por conversación.
- **Integración directa desde `ms-backend`:** descartada. Acoplaría el dominio a un proveedor externo y
  contradice el rol del conector.
- **Solo notificación interna:** descartada. No resuelve el caso real: el delegado no entra todos los días.
- **Un único número institucional con una API key compartida:** descartada por el propio proveedor: la API
  gratuita no permite enviar a terceros ni a grupos.

## Consecuencias

Positivas:

- El club recibe el aviso por el canal que ya usa, sin que nadie tenga que abrir la aplicación.
- Sustituir CallMeBot por otro proveedor toca solo `ms-conector`.

Negativas:

- **Dependencia de un servicio gratuito orientado a uso personal**: disponibilidad no garantizada y políticas
  de uso que pueden cambiar. Por eso el estado de este ADR es **Propuesta**.
- Cada delegado debe activar CallMeBot y cargar su API key: es un paso operativo manual, y **el modelo de datos
  todavía no tiene dónde guardarla** (ver `DB-SCHEMA.md` §8).
- El mensaje no puede ir directo al grupo: el delegado lo reenvía a mano.

## Validación pendiente

Antes de declararlo solución definitiva, probar el flujo con varios delegados y confirmar que CallMeBot admite
el uso institucional con una API key por persona.

## Referencias

- `docs/CONTEXTO.md` §4 (RF07), §7.9 y §11.14.
- `docs/DOMINIO.md` §4.17; `docs/DB-SCHEMA.md` §8; `docs/API-CONTRATO.md` §5.6 y §7.8.
- Informe: §5.4, RF07, HU13, §9.5.
