import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mockAuth = vi.hoisted(() => ({
  authenticated: true,
  token: undefined as string | undefined,
  updateToken: vi.fn(),
  login: vi.fn(),
}))

vi.mock('../auth/keycloak.ts', () => ({
  keycloak: {
    get authenticated() {
      return mockAuth.authenticated
    },
    get token() {
      return mockAuth.token
    },
    updateToken: mockAuth.updateToken,
    login: mockAuth.login,
  },
}))

function requestConfig(): InternalAxiosRequestConfig {
  return { headers: new AxiosHeaders() } as InternalAxiosRequestConfig
}

describe('baseApi', () => {
  beforeEach(() => {
    mockAuth.authenticated = true
    mockAuth.token = 'token-abc'
    mockAuth.updateToken.mockReset().mockResolvedValue(false)
    mockAuth.login.mockReset()
  })

  it('usa la base publica /api', async () => {
    const { API_BASE_URL, baseApi } = await import('./client.ts')

    expect(API_BASE_URL).toBe('/api')
    expect(baseApi.defaults.baseURL).toBe('/api')
  })
})

describe('withBearerToken', () => {
  it('adjunta el Bearer cuando hay token', async () => {
    const { withBearerToken } = await import('./client.ts')

    const config = withBearerToken(requestConfig(), 'token-abc')

    expect(config.headers.get('Authorization')).toBe('Bearer token-abc')
  })

  it('no agrega el header cuando no hay token', async () => {
    const { withBearerToken } = await import('./client.ts')

    const config = withBearerToken(requestConfig(), undefined)

    expect(config.headers.has('Authorization')).toBe(false)
  })
})

describe('authInterceptor', () => {
  beforeEach(() => {
    mockAuth.authenticated = true
    mockAuth.token = 'token-abc'
    mockAuth.updateToken.mockReset().mockResolvedValue(false)
    mockAuth.login.mockReset()
  })

  it('renueva el token si esta por vencer y adjunta el Bearer', async () => {
    const { authInterceptor, TOKEN_MIN_VALIDITY_SECONDS } = await import('./client.ts')

    const config = await authInterceptor(requestConfig())

    expect(mockAuth.updateToken).toHaveBeenCalledWith(TOKEN_MIN_VALIDITY_SECONDS)
    expect(config.headers.get('Authorization')).toBe('Bearer token-abc')
  })

  it('no toca la peticion si no hay sesion', async () => {
    mockAuth.authenticated = false
    const { authInterceptor } = await import('./client.ts')

    const config = await authInterceptor(requestConfig())

    expect(mockAuth.updateToken).not.toHaveBeenCalled()
    expect(config.headers.has('Authorization')).toBe(false)
  })

  it('vuelve a Keycloak si el refresh falla', async () => {
    mockAuth.updateToken.mockRejectedValue(new Error('expired'))
    const { authInterceptor } = await import('./client.ts')

    const config = await authInterceptor(requestConfig())

    expect(mockAuth.login).toHaveBeenCalledOnce()
    expect(config.headers.has('Authorization')).toBe(false)
  })
})

describe('toApiError', () => {
  it('normaliza el envelope del BFF', async () => {
    const { toApiError } = await import('./client.ts')

    const error = new AxiosError('Conflict', 'ERR_BAD_REQUEST', undefined, undefined, {
      status: 409,
      statusText: 'Conflict',
      headers: {},
      config: requestConfig(),
      data: {
        status: 409,
        code: 'bff_409',
        description: 'Conflict',
        message: 'El jugador ya tiene un fichaje vigente en este club',
      },
    })

    const apiError = toApiError(error)

    expect(apiError).toBeInstanceOf(Error)
    expect(apiError).toMatchObject({
      name: 'ApiError',
      status: 409,
      code: 'bff_409',
      description: 'Conflict',
      message: 'El jugador ya tiene un fichaje vigente en este club',
    })
  })

  it('marca como inesperado un codigo fuera del contrato', async () => {
    const { toApiError } = await import('./client.ts')
    const { UNEXPECTED_ERROR_CODE } = await import('../models/api-error.ts')

    const error = new AxiosError('Teapot', 'ERR_BAD_RESPONSE', undefined, undefined, {
      status: 418,
      statusText: 'I am a teapot',
      headers: {},
      config: requestConfig(),
      data: { status: 418, code: 'algo_raro', description: 'Teapot', message: 'boom' },
    })

    const apiError = toApiError(error)

    expect(apiError.code).toBe(UNEXPECTED_ERROR_CODE)
    expect(apiError.status).toBe(418)
    expect(apiError.message).toBe('boom')
  })

  it('sintetiza un envelope cuando la respuesta no lo trae (Nginx / 5xx sin cuerpo)', async () => {
    const { toApiError } = await import('./client.ts')

    const error = new AxiosError('Bad Gateway', 'ERR_BAD_RESPONSE', undefined, undefined, {
      status: 502,
      statusText: 'Bad Gateway',
      headers: {},
      config: requestConfig(),
      data: '<html>502 Bad Gateway</html>',
    })

    const apiError = toApiError(error, 'El servicio no responde')

    expect(apiError.status).toBe(502)
    expect(apiError.message).toBe('El servicio no responde')
    expect(apiError.description).toBe('502')
  })

  it('sintetiza un envelope cuando no hay respuesta (red caida)', async () => {
    const { toApiError } = await import('./client.ts')

    const apiError = toApiError(new AxiosError('Network Error', 'ERR_NETWORK'))

    expect(apiError.status).toBe(0)
    expect(apiError.description).toBe('Network Error')
  })
})
