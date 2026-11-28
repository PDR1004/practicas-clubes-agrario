package com.clubes.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para tests de integracion contra PostgreSQL real.
 *
 * <p>El contenedor es un singleton estatico que se inicia una sola vez por JVM. No se usa
 * {@code @Testcontainers}/{@code @Container} a proposito: con un contenedor gestionado por JUnit
 * el ciclo de vida termina al finalizar cada clase, mientras Spring cachea el contexto entre
 * clases, y el segundo test de integracion encontraria la base apagada.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractPostgresIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    // Evita el discovery HTTP contra Keycloak que dispara el issuer-uri al crear el JwtDecoder.
    @MockitoBean
    JwtDecoder jwtDecoder;
}
