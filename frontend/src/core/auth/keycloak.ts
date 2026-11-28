import Keycloak from 'keycloak-js'
import { KEYCLOAK_CLIENT_ID, KEYCLOAK_REALM, KEYCLOAK_URL } from '../constants/keycloak.ts'

/**
 * Instancia unica del cliente. keycloak-js admite un solo init por instancia,
 * asi que el provider la comparte en lugar de crear una por render.
 */
export const keycloak = new Keycloak({
  url: KEYCLOAK_URL,
  realm: KEYCLOAK_REALM,
  clientId: KEYCLOAK_CLIENT_ID,
})

let initPromise: Promise<boolean> | null = null

/**
 * Inicializa Keycloak una sola vez, incluso si React monta el efecto dos veces en StrictMode.
 * Con `login-required` el navegador se va a Keycloak antes de que la app se renderice.
 */
export function initKeycloak(): Promise<boolean> {
  initPromise ??= keycloak.init({
    onLoad: 'login-required',
    pkceMethod: 'S256',
    checkLoginIframe: false,
  })
  return initPromise
}
