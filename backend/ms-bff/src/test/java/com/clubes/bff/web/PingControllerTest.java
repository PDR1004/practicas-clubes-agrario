package com.clubes.bff.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clubes.bff.client.BackendClient;
import com.clubes.bff.client.ConectorClient;
import com.clubes.bff.security.SecurityConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PingController.class)
@Import(SecurityConfig.class)
class PingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private BackendClient backendClient;

    @MockitoBean
    private ConectorClient conectorClient;

    @Test
    void pingPropioRespondeSinToken() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("ms-bff"));
    }

    @Test
    void backendpingDelegaEnElBackend() throws Exception {
        given(backendClient.ping()).willReturn(Map.of("service", "ms-backend", "status", "ok"));

        mockMvc.perform(get("/backendping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("ms-backend"));
    }

    @Test
    void conectorpingDelegaEnElConector() throws Exception {
        given(conectorClient.ping()).willReturn(Map.of("service", "ms-conector", "status", "ok"));

        mockMvc.perform(get("/conectorping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("ms-conector"));
    }
}
