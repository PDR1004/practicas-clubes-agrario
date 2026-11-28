package com.clubes.bff.health;

import com.clubes.bff.client.BackendClient;
import com.clubes.bff.client.ConectorClient;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Aporta al health del BFF el estado de sus dependencias usando los pings y no el
 * /actuator/health de cada servicio, para no encadenar health checks recursivos.
 */
@Configuration
public class DependenciasHealthConfiguration {

    @Bean("dependencias")
    public CompositeHealthContributor dependencias(BackendClient backendClient, ConectorClient conectorClient) {
        return CompositeHealthContributor.fromMap(Map.of(
                "backend", indicador(backendClient::ping),
                "conector", indicador(conectorClient::ping)));
    }

    private static HealthIndicator indicador(Supplier<?> ping) {
        return () -> {
            try {
                ping.get();
                return Health.up().build();
            } catch (Exception e) {
                return Health.down(e).build();
            }
        };
    }
}
