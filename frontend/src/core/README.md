# `core/` — infraestructura transversal del frontend

Estructura definida en KAN-45. Cada carpeta la completa una tarea distinta del Bloque 0:

| Carpeta | Contenido | Tarea |
|---|---|---|
| `constants/` | Constantes compartidas (nombre de la app, base del SPA) | KAN-45 |
| `api/` | `baseApi` e interceptores HTTP (`authInterceptor`, `errorInterceptor`) | KAN-47 |
| `guards/` | `ProtectedRoute`: sesión y roles, redirección a `/unauthorized` | KAN-48 |
| `models/` | Tipos compartidos: roles canónicos y envelope de error de la API | KAN-46, KAN-47 |

Las carpetas vacías llevan `.gitkeep` para que existan en el repositorio hasta que su tarea las complete.

Los módulos funcionales (una carpeta por RF/HU) van en `src/modules/`, y no son parte del Bloque 0.
