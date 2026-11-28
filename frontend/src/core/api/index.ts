export { API_BASE_URL, authInterceptor, baseApi, toApiError, withBearerToken } from './client.ts'
export {
  ApiError,
  API_ERROR_CODES,
  UNEXPECTED_ERROR_CODE,
  isApiErrorBody,
  isDocumentedApiErrorCode,
} from '../models/api-error.ts'
export type { ApiErrorBody, ApiErrorCode, DocumentedApiErrorCode } from '../models/api-error.ts'
