package com.flashexchange.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RateLimiterServiceTest {
    
    @Autowired 
    private RateLimiterService rateLimiterService;

    @Autowired  
    private StringRedisTemplate redisTemplate;

    @Test 
    @DisplayName("Rate limiter test: 5 requests OK, 6th request should be blocked, wait 1 second, then 7th request should be allowed")
    void testTokenBucketLogic() throws InterruptedException {
        String testUser = "user_test_bucket";
        redisTemplate.delete("rate_limit:" + testUser);

        long capacity = 5;
        double refillRatePerSecond = 1.0;

        for (int i = 1;i <= 5;i++) {
            boolean allowed = rateLimiterService.isAllowed(testUser, capacity, refillRatePerSecond);
            assertThat(allowed).isTrue();
            System.out.println("Request " + i + " is allowed");
        }

        boolean sixthRequest = rateLimiterService.isAllowed(testUser, capacity, refillRatePerSecond);
        assertThat(sixthRequest).isFalse();
        System.out.println("Request 6 is blocked as expected");

        System.out.println("Waiting for 1 second to allow token refill...");
        Thread.sleep(1100);

        boolean seventhRequest = rateLimiterService.isAllowed(testUser, capacity, refillRatePerSecond);
        assertThat(seventhRequest).isTrue();
        System.out.println("Request 7 is allowed after waiting for token refill");
    }

    @Test
    @DisplayName("BENCHMARK: Rate limiter performance test")
    void benchmarkRateLimiter() {
        String testUser = "bench_user";
        redisTemplate.delete("rate_limit:" + testUser);

        long capacity = 10000;
        double refillRate = 10000;
        int totalRequests = 1000;

        List<Long> latencies = new ArrayList<>();

        for (int i = 0;i < totalRequests;i++) {
            long start = System.nanoTime();
            rateLimiterService.isAllowed(testUser, capacity, refillRate);
            long end = System.nanoTime();
            latencies.add(end - start);
        }
        Collections.sort(latencies);
        double avgLatencyMs = latencies.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
        double p99LatencyMs = latencies.get((int)(latencies.size() * 0.99)) / 1_000_000.0;
    
        System.out.println("==================================================");
        System.out.println("KẾT QUẢ BENCHMARK RATE LIMITER (1.000 requests):");
        System.out.printf("Thời gian trung bình (Avg Latency): %.3f ms\n", avgLatencyMs);
        System.out.printf("Thời gian 99%% request (P99 Latency): %.3f ms\n", p99LatencyMs);
        System.out.println("==================================================");

        assertThat(avgLatencyMs).isLessThan(5.0);
    }


}
