/**
 * Envelope de error que estandariza el BFF (docs/API-CONTRATO.md §3).
 * Ningun servicio devuelve su envelope crudo: el BFF traduce todo a esta forma.
 */
export type ApiErrorBody = {
  status: number
  code: string
  description: string
  message: string
}

/** Codigos documentados en el contrato. */
export const API_ERROR_CODES = [
  'bff_400',
  'bff_401',
  'bff_403',
  'bff_404',
  'bff_409',
  'bff_422',
  'bff_502',
] as const

export type DocumentedApiErrorCode = (typeof API_ERROR_CODES)[number]

/** Fallos que no llegan con el envelope: red caida, HTML de Nginx, 5xx sin cuerpo. */
export const UNEXPECTED_ERROR_CODE = 'bff_unexpected'

export type ApiErrorCode = DocumentedApiErrorCode | typeof UNEXPECTED_ERROR_CODE

export function isDocumentedApiErrorCode(value: string): value is DocumentedApiErrorCode {
  return (API_ERROR_CODES as readonly string[]).includes(value)
}

export function isApiErrorBody(value: unknown): value is ApiErrorBody {
  if (typeof value !== 'object' || value === null) {
    return false
  }
  const candidate = value as Record<string, unknown>
  return (
    typeof candidate.status === 'number' &&
    typeof candidate.code === 'string' &&
    typeof candidate.description === 'string' &&
    typeof candidate.message === 'string'
  )
}

/**
 * Error normalizado de la API. `message` queda utilizable para mostrar al usuario y
 * `code`/`status` permiten decidir sin parsear strings.
 */
export class ApiError extends Error {
  readonly status: number
  readonly code: ApiErrorCode
  readonly description: string

  constructor(body: ApiErrorBody) {
    super(body.message)
    this.name = 'ApiError'
    this.status = body.status
    this.code = isDocumentedApiErrorCode(body.code) ? body.code : UNEXPECTED_ERROR_CODE
    this.description = body.description
  }
}
