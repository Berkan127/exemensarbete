package com.example.apigateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter implements GatewayFilter {

    private static final int RATE_LIMIT = 100; // requests per minute
    private static final long RATE_LIMIT_WINDOW_MS = 60_000; // 1 minute

    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // Get client IP for rate limiting
        String clientIp = getClientIp(request);
        
        // Check rate limit
        if (isRateLimited(clientIp)) {
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().add("X-RateLimit-Limit", String.valueOf(RATE_LIMIT));
            response.getHeaders().add("X-RateLimit-Remaining", "0");
            response.getHeaders().add("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + RATE_LIMIT_WINDOW_MS));
            return response.setComplete();
        }

        // Add rate limit headers
        RateLimitInfo info = rateLimitMap.get(clientIp);
        int remaining = RATE_LIMIT - info.requestCount.get();
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(RATE_LIMIT));
        response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(Math.max(0, remaining)));
        response.getHeaders().add("X-RateLimit-Reset", String.valueOf(info.windowStart + RATE_LIMIT_WINDOW_MS));

        return chain.filter(exchange);
    }

    private boolean isRateLimited(String clientIp) {
        long currentTime = System.currentTimeMillis();
        RateLimitInfo info = rateLimitMap.computeIfAbsent(clientIp, 
                k -> new RateLimitInfo(currentTime));

        // Reset window if expired
        if (currentTime - info.windowStart > RATE_LIMIT_WINDOW_MS) {
            info.windowStart = currentTime;
            info.requestCount.set(0);
        }

        // Check if rate limit exceeded
        if (info.requestCount.get() >= RATE_LIMIT) {
            return true;
        }

        // Increment request count
        info.requestCount.incrementAndGet();
        return false;
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null ? 
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    private static class RateLimitInfo {
        final AtomicInteger requestCount = new AtomicInteger(0);
        volatile long windowStart;

        RateLimitInfo(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
