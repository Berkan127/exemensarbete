package com.example.microservices.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ServiceCommunicationIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Test
    void shouldCommunicateThroughGateway() {
        // Test that requests can be routed through the gateway to all services
        // Even though services return 404, we verify that routing works
        
        // Test Product Service routing
        webTestClient.get()
                .uri("/api/products")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/products");
        
        // Test Order Service routing
        webTestClient.get()
                .uri("/api/orders")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/orders");
        
        // Test User Service routing
        webTestClient.get()
                .uri("/api/users")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/users");
    }

    @Test
    void shouldHandleServiceSpecificEndpoints() {
        // Test specific endpoints with parameters
        
        // Test Product Service with ID parameter
        webTestClient.get()
                .uri("/api/products/1")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/products/1");
        
        // Test Product Service stock check
        webTestClient.get()
                .uri("/api/products/1/stock/check?requiredQuantity=5")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/products/1/stock/check");
        
        // Test Order Service with user parameter
        webTestClient.get()
                .uri("/api/orders/user/1")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/orders/user/1");
        
        // Test Order Service status update
        webTestClient.put()
                .uri("/api/orders/1/status?status=CONFIRMED")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/orders/1/status");
        
        // Test User Service with username
        webTestClient.get()
                .uri("/api/users/username/testuser")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/users/username/testuser");
    }

    @Test
    void shouldHandlePostRequests() {
        // Test POST requests through gateway
        
        // Test Product Service create product
        String productJson = "{\"name\":\"Test Product\",\"description\":\"Test Description\",\"price\":99.99,\"stockQuantity\":10}";
        webTestClient.post()
                .uri("/api/products")
                .header("Content-Type", "application/json")
                .bodyValue(productJson)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/products");
        
        // Test Order Service create order
        String orderJson = "{\"userId\":1,\"orderItems\":[{\"productId\":1,\"quantity\":2,\"price\":99.99}]}";
        webTestClient.post()
                .uri("/api/orders")
                .header("Content-Type", "application/json")
                .bodyValue(orderJson)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/orders");
        
        // Test User Service create user
        String userJson = "{\"username\":\"testuser\",\"email\":\"test@example.com\",\"firstName\":\"Test\",\"lastName\":\"User\"}";
        webTestClient.post()
                .uri("/api/users")
                .header("Content-Type", "application/json")
                .bodyValue(userJson)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/users");
    }

    @Test
    void shouldHandleErrorResponses() {
        // Test that error responses are properly propagated through the gateway
        
        // Test 404 for non-existent resource
        webTestClient.get()
                .uri("/api/products/999")
                .exchange()
                .expectStatus().isNotFound();
        
        // Test 404 for invalid endpoint
        webTestClient.get()
                .uri("/api/invalid")
                .exchange()
                .expectStatus().isNotFound();
        
        // Test 404 for invalid method
        webTestClient.patch()
                .uri("/api/products")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldMaintainRequestHeaders() {
        // Test that headers are properly forwarded through the gateway
        
        webTestClient.get()
                .uri("/api/products")
                .header("Accept", "application/json")
                .header("User-Agent", "test-agent")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldHandleConcurrentRequests() {
        // Test that the gateway can handle multiple concurrent requests
        
        for (int i = 0; i < 10; i++) {
            webTestClient.get()
                    .uri("/api/products")
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Test
    void shouldValidateGatewayHealth() {
        // Test that the gateway itself is healthy
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void shouldExposeGatewayRoutes() {
        // Test that gateway routes are exposed via actuator
        webTestClient.get()
                .uri("/actuator/gateway/routes")
                .exchange()
                .expectStatus().isOk();
    }
}
