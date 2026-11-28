import { render, screen } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { createElement } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mockState = vi.hoisted(() => ({
  initOptions: undefined as Record<string, unknown> | undefined,
  realmRoles: [] as string[],
  logoutOptions: undefined as Record<string, unknown> | undefined,
}))

vi.mock('keycloak-js', () => ({
  default: class {
    init(options: Record<string, unknown>) {
      mockState.initOptions = options
      return Promise.resolve(true)
    }

    logout(options: Record<string, unknown>) {
      mockState.logoutOptions = options
      return Promise.resolve()
    }

    get realmAccess() {
      return { roles: mockState.realmRoles }
    }
  },
}))

type AuthModule = typeof import('./AuthContext.tsx')

async function mountAuth() {
  vi.resetModules()
  const { AuthProvider, useAuth }: AuthModule = await import('./AuthContext.tsx')

  function Probe() {
    const { authenticated, roles, hasRole, logout } = useAuth()
    return createElement(
      'div',
      null,
      createElement('span', { 'data-testid': 'authenticated' }, String(authenticated)),
      createElement('span', { 'data-testid': 'roles' }, roles.join(',')),
      createElement('span', { 'data-testid': 'is-delegado' }, String(hasRole('DELEGADO'))),
      createElement('button', { type: 'button', onClick: logout }, 'Salir'),
    )
  }

  render(createElement(AuthProvider, null, createElement(Probe)))
}

describe('AuthProvider', () => {
  beforeEach(() => {
    mockState.initOptions = undefined
    mockState.logoutOptions = undefined
    mockState.realmRoles = ['ADMIN_CLUB', 'default-roles-clubes', 'offline_access']
  })

  it('inicializa Keycloak con login-required y PKCE S256', async () => {
    await mountAuth()

    expect(await screen.findByTestId('authenticated')).toHaveTextContent('true')
    expect(mockState.initOptions).toMatchObject({
      onLoad: 'login-required',
      pkceMethod: 'S256',
    })
  })

  it('expone solo las roles canonicas y responde hasRole', async () => {
    await mountAuth()

    expect(await screen.findByTestId('roles')).toHaveTextContent('ADMIN_CLUB')
    expect(screen.getByTestId('roles')).not.toHaveTextContent('default-roles-clubes')
    expect(screen.getByTestId('roles')).not.toHaveTextContent('offline_access')
    expect(screen.getByTestId('is-delegado')).toHaveTextContent('false')
  })

  it('hasRole reconoce la rol cuando la tiene', async () => {
    mockState.realmRoles = ['DELEGADO']
    await mountAuth()

    expect(await screen.findByTestId('is-delegado')).toHaveTextContent('true')
  })

  it('logout limpia la sesion y vuelve a la base del SPA', async () => {
    await mountAuth()
    expect(await screen.findByTestId('authenticated')).toHaveTextContent('true')

    await userEvent.click(screen.getByRole('button', { name: 'Salir' }))

    expect(screen.getByTestId('authenticated')).toHaveTextContent('false')
    expect(screen.getByTestId('roles').textContent).toBe('')
    expect(mockState.logoutOptions).toMatchObject({
      redirectUri: `${window.location.origin}/clubes/`,
    })
  })
})
