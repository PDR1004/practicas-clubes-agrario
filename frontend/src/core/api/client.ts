import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { keycloak } from '../auth/keycloak.ts'
import {
  ApiError,
  isApiErrorBody,
  UNEXPECTED_ERROR_CODE,
} from '../models/api-error.ts'

/**
 * Base publica del contrato: Nginx reescribe /api/* a /bff/* y desde ahi al BFF
 * (docs/API-CONTRATO.md §1). El navegador nunca conoce la URL interna del servicio.
 */
export const API_BASE_URL = '/api'

/** Margen de seguridad para renovar el access token antes de que venza. */
export const TOKEN_MIN_VALIDITY_SECONDS = 30

export const baseApi = axios.create({
  baseURL: API_BASE_URL,
})

/** Aplica el token a la peticion. Aislado del cliente para poder probarlo sin red. */
export function withBearerToken(
  config: InternalAxiosRequestConfig,
  token: string | undefined,
): InternalAxiosRequestConfig {
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
}

/**
 * Adjunta el access token renovandolo si esta por vencer. Si el refresh falla, la
 * sesion ya no es recuperable sin volver a Keycloak.
 */
export async function authInterceptor(
  config: InternalAxiosRequestConfig,
): Promise<InternalAxiosRequestConfig> {
  if (!keycloak.authenticated) {
    return config
  }

  try {
    await keycloak.updateToken(TOKEN_MIN_VALIDITY_SECONDS)
  } catch {
    await keycloak.login()
    return config
  }

  return withBearerToken(config, keycloak.token)
}

/**
 * Convierte cualquier fallo en un ApiError con la forma del contrato. Si la respuesta no
 * trae el envelope (Nginx, red caida, 5xx sin cuerpo) se sintetiza uno equivalente.
 */
export function toApiError(
  error: unknown,
  fallbackMessage = 'No se pudo completar la peticion',
): ApiError {
  const axiosError = error as AxiosError<unknown>
  const response = axiosError.response

  if (response && isApiErrorBody(response.data)) {
    return new ApiError(response.data)
  }

  const status = response?.status ?? 0
  return new ApiError({
    status,
    code: UNEXPECTED_ERROR_CODE,
    description: status === 0 ? 'Network Error' : String(status),
    message: fallbackMessage,
  })
}

baseApi.interceptors.request.use(authInterceptor)

baseApi.interceptors.response.use(
  (response) => response,
  (error: unknown) => Promise.reject(toApiError(error)),
)
