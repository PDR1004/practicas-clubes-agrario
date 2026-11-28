/**
 * Conexion con el realm `clubes`.
 * KAN-49 mueve estos valores a variables de entorno (.env.development / .env.production).
 */
export const KEYCLOAK_URL = 'https://clubes.local/auth'
export const KEYCLOAK_REALM = 'clubes'
export const KEYCLOAK_CLIENT_ID = 'clubes-fe'
