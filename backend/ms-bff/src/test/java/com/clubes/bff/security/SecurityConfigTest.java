package com.clubes.bff.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigTest.TestSecurityController.class)
@Import({SecurityConfig.class, SecurityConfigTest.TestSecurityController.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @RestController
    static class TestSecurityController {
        @GetMapping("/test/protected")
        public String protectedEndpoint() {
            return "ok";
        }

        @PreAuthorize("hasRole('ADMIN_CLUB')")
        @GetMapping("/test/admin-only")
        public String adminOnly() {
            return "admin-ok";
        }
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conRolInsuficienteDevuelve403() throws Exception {
        mockMvc.perform(get("/test/admin-only")
                        .with(jwt().jwt(j -> j.claim("realm_access", Map.of("roles", List.of("ENTRENADOR"))))
                                .authorities(new KeycloakRealmRoleConverter())))
                .andExpect(status().isForbidden());
    }

    @Test
    void conRolValidoDevuelve200() throws Exception {
        mockMvc.perform(get("/test/admin-only")
                        .with(jwt().jwt(j -> j.claim("realm_access", Map.of("roles", List.of("ADMIN_CLUB"))))
                                .authorities(new KeycloakRealmRoleConverter())))
                .andExpect(status().isOk());
    }
}