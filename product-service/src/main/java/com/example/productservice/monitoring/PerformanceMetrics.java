package com.example.productservice.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class PerformanceMetrics {

    private final Counter productCreateCounter;
    private final Counter productReadCounter;
    private final Counter productUpdateCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Timer productFetchTimer;
    private final Timer databaseQueryTimer;

    public PerformanceMetrics(MeterRegistry meterRegistry) {
        this.productCreateCounter = Counter.builder("product.create.count")
                .description("Total number of products created")
                .register(meterRegistry);
        
        this.productReadCounter = Counter.builder("product.read.count")
                .description("Total number of products read")
                .register(meterRegistry);
        
        this.productUpdateCounter = Counter.builder("product.update.count")
                .description("Total number of products updated")
                .register(meterRegistry);
        
        this.cacheHitCounter = Counter.builder("cache.hit.count")
                .description("Total number of cache hits")
                .register(meterRegistry);
        
        this.cacheMissCounter = Counter.builder("cache.miss.count")
                .description("Total number of cache misses")
                .register(meterRegistry);
        
        this.productFetchTimer = Timer.builder("product.fetch.time")
                .description("Time taken to fetch products")
                .register(meterRegistry);
        
        this.databaseQueryTimer = Timer.builder("database.query.time")
                .description("Time taken for database queries")
                .register(meterRegistry);
    }

    public void incrementProductCreate() {
        productCreateCounter.increment();
    }

    public void incrementProductRead() {
        productReadCounter.increment();
    }

    public void incrementProductUpdate() {
        productUpdateCounter.increment();
    }

    public void incrementCacheHit() {
        cacheHitCounter.increment();
    }

    public void incrementCacheMiss() {
        cacheMissCounter.increment();
    }

    public Timer.Sample startProductFetchTimer() {
        return Timer.start(meterRegistry -> productFetchTimer);
    }

    public Timer.Sample startDatabaseQueryTimer() {
        return Timer.start(meterRegistry -> databaseQueryTimer);
    }
}
