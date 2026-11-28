package com.clubes.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = TenantContextFilterTest.TestTenantController.class)
@Import({
    SecurityConfig.class,
    TenantContextFilter.class,
    TenantContextResolver.class,
    TenantContextFilterTest.TestTenantController.class
})
class TenantContextFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @RestController
    static class TestTenantController {

        @GetMapping("/test/tenant/current")
        public Map<String, Object> currentTenant() {
            TenantContext ctx = TenantContextHolder.require();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sub", ctx.keycloakSub());
            body.put("idClub", ctx.idClub());
            body.put("idLiga", ctx.idLiga());
            body.put("hasClubScope", ctx.hasClubScope());
            body.put("hasLigaScope", ctx.hasLigaScope());
            return body;
        }

        @GetMapping("/test/tenant/club/{targetClubId}")
        public Map<String, Object> clubScopedEndpoint(@PathVariable Integer targetClubId) {
            TenantContext ctx = TenantContextHolder.require();
            ctx.assertCanAccessClub(targetClubId);
            return Map.of("idClub", targetClubId, "allowed", true);
        }
    }

    @Test
    void resuelveIdClubDesdeClaimDelTokenSinConsultarBase() throws Exception {
        mockMvc.perform(get("/test/tenant/current")
                        .with(jwt().jwt(j -> j
                                        .subject("sub-club-user")
                                        .claim("id_club", 7)
                                        .claim("realm_access", Map.of("roles", List.of("DELEGADO"))))
                                .authorities(new KeycloakRealmRoleConverter())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("sub-club-user"))
                .andExpect(jsonPath("$.idClub").value(7))
                .andExpect(jsonPath("$.idLiga").isEmpty())
                .andExpect(jsonPath("$.hasClubScope").value(true));

        verify(jdbcTemplate, never()).query(any(String.class), any(RowMapper.class), any());
        assertThat(TenantContextHolder.get()).isEmpty();
    }

    @Test
    void resuelveIdLigaDesdeClaimDelTokenSinConsultarBase() throws Exception {
        mockMvc.perform(get("/test/tenant/current")
                        .with(jwt().jwt(j -> j
                                        .subject("sub-liga-user")
                                        .claim("id_liga", 2)
                                        .claim("realm_access", Map.of("roles", List.of("SUPERADMIN"))))
                                .authorities(new KeycloakRealmRoleConverter())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("sub-liga-user"))
                .andExpect(jsonPath("$.idLiga").value(2))
                .andExpect(jsonPath("$.idClub").isEmpty())
                .andExpect(jsonPath("$.hasLigaScope").value(true));

        verify(jdbcTemplate, never()).query(any(String.class), any(RowMapper.class), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void haceFallbackPorKeycloakSubContraLaBaseCuandoFaltanClaims() throws Exception {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("sub-fallback-1")))
                .thenAnswer(invocation -> {
                    RowMapper<Object> mapper = invocation.getArgument(1);
                    ResultSet rs = Mockito.mock(ResultSet.class);
                    when(rs.getObject("id_liga")).thenReturn(null);
                    when(rs.getObject("id_club")).thenReturn(15);
                    when(rs.getBoolean("activo")).thenReturn(true);
                    return List.of(mapper.mapRow(rs, 0));
                });

        mockMvc.perform(get("/test/tenant/current")
                        .with(jwt().jwt(j -> j
                                        .subject("sub-fallback-1")
                                        .claim("realm_access", Map.of("roles", List.of("TESORERO"))))
                                .authorities(new KeycloakRealmRoleConverter())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("sub-fallback-1"))
                .andExpect(jsonPath("$.idClub").value(15))
                .andExpect(jsonPath("$.idLiga").isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void rechazaCon403CuandoElUsuarioEnFallbackEstaInactivo() throws Exception {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("sub-inactivo")))
                .thenAnswer(invocation -> {
                    RowMapper<Object> mapper = invocation.getArgument(1);
                    ResultSet rs = Mockito.mock(ResultSet.class);
                    when(rs.getObject("id_liga")).thenReturn(null);
                    when(rs.getObject("id_club")).thenReturn(15);
                    when(rs.getBoolean("activo")).thenReturn(false);
                    return List.of(mapper.mapRow(rs, 0));
                });

        mockMvc.perform(get("/test/tenant/current")
                        .with(jwt().jwt(j -> j
                                        .subject("sub-inactivo")
                                        .claim("realm_access", Map.of("roles", List.of("DELEGADO"))))
                                .authorities(new KeycloakRealmRoleConverter())))
                .andExpect(status().isForbidden());
    }

    @Test
    void bloqueaCon403AccesoAOtroClubDistintoDelTenantActual() throws Exception {
        mockMvc.perform(get("/test/tenant/club/99")
                        .with(jwt().jwt(j -> j
                                        .subject("sub-club-1")
                                        .claim("id_club", 1)
                                        .claim("realm_access", Map.of("roles", List.of("ADMIN_CLUB"))))
                                .authorities(new KeycloakRealmRoleConverter())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/test/tenant/club/1")
                        .with(jwt().jwt(j -> j
                                        .subject("sub-club-1")
                                        .claim("id_club", 1)
                                        .claim("realm_access", Map.of("roles", List.of("ADMIN_CLUB"))))
                                .authorities(new KeycloakRealmRoleConverter())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idClub").value(1));
    }
}
