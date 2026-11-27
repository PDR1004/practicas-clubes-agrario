package com.clubes.bff.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@SpringBootTest
class FeignAuthInterceptorIntegrationTest {

    private static HttpServer mockUpstreamServer;
    private static final AtomicReference<String> lastBackendAuthHeader = new AtomicReference<>();
    private static final AtomicReference<String> lastConectorAuthHeader = new AtomicReference<>();

    @Autowired
    private BackendClient backendClient;

    @Autowired
    private ConectorClient conectorClient;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeAll
    static void startMockServer() throws IOException {
        mockUpstreamServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockUpstreamServer.createContext("/backend/ping", exchange -> {
            lastBackendAuthHeader.set(exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            byte[] response = "{\"service\":\"ms-backend\",\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        mockUpstreamServer.createContext("/conector/ping", exchange -> {
            lastConectorAuthHeader.set(exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            byte[] response = "{\"service\":\"ms-conector\",\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        mockUpstreamServer.start();
    }

    @AfterAll
    static void stopMockServer() {
        if (mockUpstreamServer != null) {
            mockUpstreamServer.stop(0);
        }
    }

    @AfterEach
    void cleanupContexts() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        lastBackendAuthHeader.set(null);
        lastConectorAuthHeader.set(null);
    }

    @DynamicPropertySource
    static void configureUpstreamUrls(DynamicPropertyRegistry registry) {
        registry.add("clients.ms-backend.url",
            () -> "http://localhost:" + mockUpstreamServer.getAddress().getPort() + "/backend");
        registry.add("clients.ms-conector.url",
            () -> "http://localhost:" + mockUpstreamServer.getAddress().getPort() + "/conector");
    }

    @Test
    void propagaHeaderAuthorizationDesdeHttpServletRequestHaciaBackendYConector() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-desde-request-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Map<String, Object> backendResp = backendClient.ping();
        Map<String, Object> conectorResp = conectorClient.ping();

        assertThat(backendResp).containsEntry("service", "ms-backend");
        assertThat(conectorResp).containsEntry("service", "ms-conector");
        assertThat(lastBackendAuthHeader.get()).isEqualTo("Bearer token-desde-request-123");
        assertThat(lastConectorAuthHeader.get()).isEqualTo("Bearer token-desde-request-123");
    }

    @Test
    void propagaBearerDesdeJwtAuthenticationTokenEnSecurityContext() {
        Jwt jwt = new Jwt(
            "token-desde-security-context-456",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of("alg", "RS256"),
            Map.of("sub", "user-1", "realm_access", Map.of("roles", List.of("DELEGADO")))
        );
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        backendClient.ping();

        assertThat(lastBackendAuthHeader.get()).isEqualTo("Bearer token-desde-security-context-456");
    }

    @Test
    void sinTokenNoEnviaHeaderAuthorization() {
        backendClient.ping();

        assertThat(lastBackendAuthHeader.get()).isNull();
    }
}
