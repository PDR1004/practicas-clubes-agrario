package com.clubes.bff.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "ms-backend", url = "${clients.ms-backend.url}")
public interface BackendClient {

    @GetMapping("/ping")
    Map<String, Object> ping();
}
