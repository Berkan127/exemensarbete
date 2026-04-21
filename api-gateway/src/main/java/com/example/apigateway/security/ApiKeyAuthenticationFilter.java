package com.example.apigateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
public class ApiKeyAuthenticationFilter implements GatewayFilter {

    private static final Set<String> VALID_API_KEYS = Set.of(
            "api-key-123456789",
            "api-key-987654321",
            "api-key-admin-2024"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // Skip API key check for auth endpoints
        String path = request.getURI().getPath();
        if (path.startsWith("/auth/") || path.startsWith("/actuator/")) {
            return chain.filter(exchange);
        }

        // Check for API key in header
        String apiKey = request.getHeaders().getFirst("X-API-Key");
        if (apiKey == null) {
            apiKey = request.getQueryParams().getFirst("api_key");
        }

        if (apiKey == null || !VALID_API_KEYS.contains(apiKey)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("WWW-Authenticate", "ApiKey");
            return response.setComplete();
        }

        // Add API key info to headers
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-Api-Key-Valid", "true")
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }
}
