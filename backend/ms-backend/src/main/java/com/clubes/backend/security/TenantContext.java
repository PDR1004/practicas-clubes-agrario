package com.clubes.backend.security;

import java.util.Objects;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;

/**
 * Contexto de alcance multi-tenant resuelto para el usuario autenticado en el request actual.
 */
public record TenantContext(
        String keycloakSub,
        Integer idLiga,
        Integer idClub,
        Set<String> roles
) {

    public TenantContext {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean hasClubScope() {
        return idClub != null;
    }

    public boolean hasLigaScope() {
        return idLiga != null;
    }

    public boolean isSuperAdmin() {
        return roles.contains("SUPERADMIN") || roles.contains("ROLE_SUPERADMIN");
    }

    public Integer requireIdClub() {
        if (idClub == null) {
            throw new AccessDeniedException("El usuario autenticado no posee alcance de club (id_club)");
        }
        return idClub;
    }

    public Integer requireIdLiga() {
        if (idLiga == null) {
            throw new AccessDeniedException("El usuario autenticado no posee alcance de liga (id_liga)");
        }
        return idLiga;
    }

    public boolean canAccessClub(Integer targetIdClub) {
        if (targetIdClub == null) {
            return false;
        }
        return isSuperAdmin() || Objects.equals(this.idClub, targetIdClub);
    }

    public void assertCanAccessClub(Integer targetIdClub) {
        if (!canAccessClub(targetIdClub)) {
            throw new AccessDeniedException("Acceso denegado a recursos de otro club");
        }
    }
}
