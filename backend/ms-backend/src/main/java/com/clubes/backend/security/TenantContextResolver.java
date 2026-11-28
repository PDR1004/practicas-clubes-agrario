package com.clubes.backend.security;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resuelve el {@link TenantContext} del usuario autenticado a partir de los claims {@code id_club}
 * e {@code id_liga} del JWT, con fallback por {@code keycloak_sub} contra la tabla {@code usuario}.
 */
@Component
public class TenantContextResolver {

    private static final String SQL_FIND_TENANT_BY_SUB =
            "SELECT id_liga, id_club, activo FROM usuario WHERE keycloak_sub = ?";

    private final JdbcTemplate jdbcTemplate;

    public TenantContextResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TenantContext resolve(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        String sub = jwt.getSubject();
        Set<String> roles = extractRoles(authentication);

        Integer idLiga = extractIntegerClaim(jwt, "id_liga");
        Integer idClub = extractIntegerClaim(jwt, "id_club");

        if (idLiga != null && idClub != null) {
            throw new AccessDeniedException(
                    "Alcance institucional invalido: el token contiene id_liga e id_club simultaneamente");
        }

        if (idLiga == null && idClub == null && StringUtils.hasText(sub)) {
            DbTenantRecord dbRecord = findInDatabase(sub);
            if (dbRecord != null) {
                if (!dbRecord.activo()) {
                    throw new AccessDeniedException("El usuario se encuentra inactivo");
                }
                idLiga = dbRecord.idLiga();
                idClub = dbRecord.idClub();
            }
        }

        return new TenantContext(sub, idLiga, idClub, roles);
    }

    private DbTenantRecord findInDatabase(String keycloakSub) {
        List<DbTenantRecord> results = jdbcTemplate.query(
                SQL_FIND_TENANT_BY_SUB,
                (rs, rowNum) -> new DbTenantRecord(
                        (Integer) rs.getObject("id_liga"),
                        (Integer) rs.getObject("id_club"),
                        rs.getBoolean("activo")
                ),
                keycloakSub
        );
        return results.isEmpty() ? null : results.getFirst();
    }

    private static Integer extractIntegerClaim(Jwt jwt, String claimName) {
        Object raw = jwt.getClaim(claimName);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String str && StringUtils.hasText(str)) {
            try {
                return Integer.valueOf(str.trim());
            } catch (NumberFormatException ex) {
                throw new AccessDeniedException("Claim " + claimName + " no tiene formato numerico valido");
            }
        }
        throw new AccessDeniedException("Claim " + claimName + " tiene un tipo no soportado");
    }

    private static Set<String> extractRoles(JwtAuthenticationToken authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
    }

    private record DbTenantRecord(Integer idLiga, Integer idClub, boolean activo) {
    }
}
