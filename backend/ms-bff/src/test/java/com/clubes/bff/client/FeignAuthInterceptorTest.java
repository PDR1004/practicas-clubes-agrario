package com.clubes.bff.client;

import static org.assertj.core.api.Assertions.assertThat;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

class FeignAuthInterceptorTest {

    private final FeignAuthInterceptor interceptor = new FeignAuthInterceptor();

    @AfterEach
    void limpiarContextos() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void sinAutenticacionOmiteElHeaderSinFallar() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey("Authorization");
    }
}
