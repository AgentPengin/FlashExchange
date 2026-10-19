package com.flashexchange.config;

import com.flashexchange.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component 
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {
    // Implementation of the rate limit filter
    private final RateLimiterService rateLimiterService;

    public RateLimitFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, java.io.IOException {
        String userId = request.getHeader("X-User-Id");
        String clientIp = request.getRemoteAddr();

        String rateLimitKey;
        long capacity = 5;
        double refillRate;

        if (userId != null && !userId.isBlank()) {
            rateLimitKey = "user:" + userId;
            capacity = 20;
            refillRate = 10.0;
        } else {
            rateLimitKey = "ip:" + clientIp;
            capacity = 5;
            refillRate = 2.0;
        }

        boolean allowed = rateLimiterService.isAllowed(rateLimitKey, capacity, refillRate);
        if (allowed) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "1");
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write("""
                {
                    "error": "Too Many Requests",
                    "status": 429,
                    "message": "You have exceeded the rate limit. Please try again later."
                }   
            """);
        }
    }

    @Override 
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/ws");
    }

}
