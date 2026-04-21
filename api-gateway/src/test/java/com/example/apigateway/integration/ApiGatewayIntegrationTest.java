package com.example.apigateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.test.TestRouteConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestRouteConfig.class)
class ApiGatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldRouteToProductService() {
        webTestClient.get()
                .uri("/api/products")
                .exchange()
                .expectStatus().isNotFound() // Service returns 404 but routing works
                .expectBody()
                .jsonPath("$.path").isEqualTo("/products");
    }

    @Test
    void shouldRouteToOrderService() {
        webTestClient.get()
                .uri("/api/orders")
                .exchange()
                .expectStatus().isNotFound() // Service returns 404 but routing works
                .expectBody()
                .jsonPath("$.path").isEqualTo("/orders");
    }

    @Test
    void shouldRouteToUserService() {
        webTestClient.get()
                .uri("/api/users")
                .exchange()
                .expectStatus().isNotFound() // Service returns 404 but routing works
                .expectBody()
                .jsonPath("$.path").isEqualTo("/users");
    }

    @Test
    void shouldHandleCorsHeaders() {
        webTestClient.options()
                .uri("/api/products")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldReturnGatewayHealth() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void shouldReturnGatewayRoutes() {
        webTestClient.get()
                .uri("/actuator/gateway/routes")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldStripApiPrefix() {
        // This test verifies that the StripPrefix=1 filter is working
        // The request to /api/products should be routed to /products on the target service
        webTestClient.get()
                .uri("/api/products/test")
                .exchange()
                .expectStatus().isNotFound() // Service returns 404 but routing works
                .expectBody()
                .jsonPath("$.path").isEqualTo("/products/test");
    }

    @Test
    void shouldHandleInvalidRoute() {
        webTestClient.get()
                .uri("/api/invalid")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldHandleRootPath() {
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isNotFound();
    }
}
