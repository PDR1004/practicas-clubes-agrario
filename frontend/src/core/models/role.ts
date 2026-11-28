/** Roles canonicas del realm `clubes`. Espejo de KeycloakRealmRoleConverter del backend. */
export const CANONICAL_ROLES = [
  'SUPERADMIN',
  'ADMIN_CLUB',
  'TESORERO',
  'DELEGADO',
  'ENTRENADOR',
] as const

export type Role = (typeof CANONICAL_ROLES)[number]

export function isCanonicalRole(value: string): value is Role {
  return (CANONICAL_ROLES as readonly string[]).includes(value)
}
