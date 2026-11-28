import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { BASE_PATH } from '../constants/app.ts'
import { isCanonicalRole, type Role } from '../models/role.ts'
import { initKeycloak, keycloak } from './keycloak.ts'

export type AuthState = {
  /** Sesion autenticada, con el init de Keycloak ya resuelto. */
  authenticated: boolean
  /** Solo las 5 roles canonicas; descarta default-roles-clubes, offline_access y uma_authorization. */
  roles: Role[]
  hasRole: (role: Role) => boolean
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

function readRoles(): Role[] {
  return (keycloak.realmAccess?.roles ?? []).filter(isCanonicalRole)
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false)
  const [authenticated, setAuthenticated] = useState(false)
  const [roles, setRoles] = useState<Role[]>([])

  useEffect(() => {
    // With login-required the browser goes to Keycloak before rendering; the promise
    // resolves already authenticated (or after coming back from the redirect).
    let active = true

    initKeycloak()
      .then((isAuthenticated) => {
        if (!active) return
        setAuthenticated(isAuthenticated)
        setRoles(readRoles())
        setReady(true)
      })
      .catch((error: unknown) => {
        console.error('No se pudo inicializar Keycloak', error)
      })

    return () => {
      active = false
    }
  }, [])

  const hasRole = useCallback((role: Role) => roles.includes(role), [roles])

  const logout = useCallback(() => {
    setAuthenticated(false)
    setRoles([])
    void keycloak.logout({ redirectUri: `${window.location.origin}${BASE_PATH}` })
  }, [])

  const value = useMemo<AuthState>(
    () => ({ authenticated, roles, hasRole, logout }),
    [authenticated, roles, hasRole, logout],
  )

  if (!ready) {
    return <p>Verificando sesion...</p>
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth debe usarse dentro de AuthProvider')
  }
  return context
}
