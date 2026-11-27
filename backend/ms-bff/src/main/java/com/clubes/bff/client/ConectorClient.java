package com.clubes.bff.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "ms-conector", url = "${clients.ms-conector.url}")
public interface ConectorClient {

    @GetMapping("/ping")
    Map<String, Object> ping();
}
