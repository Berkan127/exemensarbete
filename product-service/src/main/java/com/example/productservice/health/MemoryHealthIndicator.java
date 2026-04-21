package com.example.productservice.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

@Component
public class MemoryHealthIndicator implements HealthIndicator {

    private static final double MAX_MEMORY_USAGE_PERCENTAGE = 0.9;

    @Override
    public Health health() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        double usedPercentage = (double) heapUsage.getUsed() / heapUsage.getMax();
        
        if (usedPercentage > MAX_MEMORY_USAGE_PERCENTAGE) {
            return Health.down()
                    .withDetail("used", heapUsage.getUsed())
                    .withDetail("max", heapUsage.getMax())
                    .withDetail("usage_percentage", String.format("%.2f%%", usedPercentage * 100))
                    .withDetail("status", "Memory usage too high")
                    .build();
        }
        
        return Health.up()
                .withDetail("used", heapUsage.getUsed())
                .withDetail("max", heapUsage.getMax())
                .withDetail("usage_percentage", String.format("%.2f%%", usedPercentage * 100))
                .build();
    }
}
