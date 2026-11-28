package com.clubes.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Red de seguridad del esquema: si V1 se rompe o pierde un indice parcial, este test falla.
 */
class EsquemaInicialTest extends AbstractPostgresIT {

    private static final int TABLAS_ESPERADAS = 25;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void flywayAplicaElEsquemaInicial() {
        Integer tablas = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                  and table_type = 'BASE TABLE'
                  and table_name <> 'flyway_schema_history'
                """, Integer.class);

        assertThat(tablas).isEqualTo(TABLAS_ESPERADAS);
    }

    @Test
    void existenLosIndicesParcialesDelEsquema() {
        assertThat(existeIndice("uq_temporada_activa_liga")).isTrue();
        assertThat(existeIndice("uq_fichaje_vigente")).isTrue();
        assertThat(existeIndice("uq_usuario_rol_vigente")).isTrue();
    }

    private boolean existeIndice(String nombre) {
        Integer cantidad = jdbcTemplate.queryForObject("""
                select count(*)
                from pg_indexes
                where schemaname = 'public'
                  and indexname = ?
                """, Integer.class, nombre);

        return cantidad != null && cantidad > 0;
    }
}
