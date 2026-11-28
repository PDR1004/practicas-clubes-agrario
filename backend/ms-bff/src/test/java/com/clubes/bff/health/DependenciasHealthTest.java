package com.clubes.bff.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.clubes.bff.client.BackendClient;
import com.clubes.bff.client.ConectorClient;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

class DependenciasHealthTest {

    private final BackendClient backendClient = mock(BackendClient.class);
    private final ConectorClient conectorClient = mock(ConectorClient.class);
    private final CompositeHealthContributor dependencias =
            new DependenciasHealthConfiguration().dependencias(backendClient, conectorClient);

    @Test
    void upCuandoLasDependenciasResponden() {
        given(backendClient.ping()).willReturn(Map.of("service", "ms-backend", "status", "ok"));
        given(conectorClient.ping()).willReturn(Map.of("service", "ms-conector", "status", "ok"));

        assertThat(salud("backend").getStatus()).isEqualTo(Status.UP);
        assertThat(salud("conector").getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void downCuandoUnaDependenciaFalla() {
        given(backendClient.ping()).willThrow(new RuntimeException("backend caido"));
        given(conectorClient.ping()).willReturn(Map.of("service", "ms-conector", "status", "ok"));

        Health backend = salud("backend");

        assertThat(backend.getStatus()).isEqualTo(Status.DOWN);
        assertThat(backend.getDetails()).containsKey("error");
    }

    private Health salud(String nombre) {
        return ((HealthIndicator) dependencias.getContributor(nombre)).health();
    }
}
