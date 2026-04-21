package com.example.productservice.performance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LoadTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testConcurrentProductReads() throws InterruptedException {
        int numberOfThreads = 50;
        int requestsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        try {
                            ResponseEntity<String> response = restTemplate.getForEntity("/api/products", String.class);
                            if (response.getStatusCode().is2xxSuccessful()) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        
        int totalRequests = numberOfThreads * requestsPerThread;
        double successRate = (double) successCount.get() / totalRequests * 100;
        
        System.out.println("Total requests: " + totalRequests);
        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Failed requests: " + errorCount.get());
        System.out.println("Success rate: " + successRate + "%");
        
        assertTrue(successRate > 95.0, "Success rate should be above 95%");
    }

    @Test
    void testProductCreationLoad() throws InterruptedException {
        int numberOfThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    String productJson = String.format(
                            "{\"name\":\"Product %d\",\"description\":\"Description %d\",\"price\":99.99,\"stockQuantity\":10}",
                            threadId, threadId
                    );
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<String> entity = new HttpEntity<>(productJson, headers);
                    
                    ResponseEntity<String> response = restTemplate.postForEntity("/api/products", entity, String.class);
                    
                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        
        System.out.println("Product creation - Success: " + successCount.get() + ", Errors: " + errorCount.get());
        
        assertTrue(successCount.get() > 15, "At least 15 out of 20 creations should succeed");
    }

    @Test
    void testCachePerformance() throws InterruptedException {
        // First request to populate cache
        restTemplate.getForEntity("/api/products", String.class);
        
        int numberOfRequests = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfRequests; i++) {
            executor.submit(() -> {
                try {
                    restTemplate.getForEntity("/api/products", String.class);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTimePerRequest = (double) totalTime / numberOfRequests;
        
        System.out.println("Cache performance test:");
        System.out.println("Total time for " + numberOfRequests + " requests: " + totalTime + "ms");
        System.out.println("Average time per request: " + avgTimePerRequest + "ms");
        
        assertTrue(avgTimePerRequest < 50.0, "Average time per request should be less than 50ms with caching");
    }
}
