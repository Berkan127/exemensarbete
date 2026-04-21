package com.example.productservice.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;

@Component
public class ExternalServiceHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        boolean orderServiceAvailable = checkService("order-service", 8083);
        boolean userServiceAvailable = checkService("user-service", 8081);
        boolean gatewayAvailable = checkService("api-gateway", 8080);

        if (orderServiceAvailable && userServiceAvailable && gatewayAvailable) {
            return Health.up()
                    .withDetail("order-service", "Available")
                    .withDetail("user-service", "Available")
                    .withDetail("api-gateway", "Available")
                    .build();
        } else {
            return Health.down()
                    .withDetail("order-service", orderServiceAvailable ? "Available" : "Unavailable")
                    .withDetail("user-service", userServiceAvailable ? "Available" : "Unavailable")
                    .withDetail("api-gateway", gatewayAvailable ? "Available" : "Unavailable")
                    .build();
        }
    }

    private boolean checkService(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
