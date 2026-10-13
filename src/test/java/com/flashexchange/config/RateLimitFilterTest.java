package com.flashexchange.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest 
@AutoConfigureMockMvc 
public class RateLimitFilterTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired 
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("Test rate limiting for user-based requests, 5 requests should be allowed, 6th should be blocked")
    void testGuestRateLimit() throws Exception {
        redisTemplate.delete("rate_limit:ip:127.0.0.1");

        for (int i = 1;i <= 5;i++) {
            mockMvc.perform(get("/api/v1/orders/depth")).andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/orders/depth"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "1"));

    }


    @Test
    @DisplayName("Test rate limiting for user-based requests, 20 requests should be allowed, 21st should be blocked")
    void testUserRateLimit() throws Exception {
        String user = "12345";
        redisTemplate.delete("rate_limit:user:" + user);

        for (int i = 1;i <= 20;i++) {
            mockMvc.perform(get("/api/v1/orders/depth").header("X-User-Id", user)).andExpect(status().isOk());
        }

    }

}