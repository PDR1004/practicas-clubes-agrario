package com.clubes.bff.web;

import com.clubes.bff.client.BackendClient;
import com.clubes.bff.client.ConectorClient;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    private final BackendClient backendClient;
    private final ConectorClient conectorClient;

    public PingController(BackendClient backendClient, ConectorClient conectorClient) {
        this.backendClient = backendClient;
        this.conectorClient = conectorClient;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "ms-bff");
        body.put("status", "ok");
        body.put("timestamp", Instant.now().toString());
        return body;
    }

    @GetMapping("/backendping")
    public Map<String, Object> backendPing() {
        return backendClient.ping();
    }

    @GetMapping("/conectorping")
    public Map<String, Object> conectorPing() {
        return conectorClient.ping();
    }
}
