package com.mishraachandan.booking_system.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        String path = request.getRequestURI();
        String endpointGroup = "default";
        int limit = 60;

        if (path.contains("/seats/lock")) {
            endpointGroup = "seat-lock";
            limit = 10;
        } else if (path.contains("/bookings")) {
            endpointGroup = "booking";
            limit = 20;
        } else if (path.contains("/auth/login") || path.contains("/auth/register")) {
            endpointGroup = "auth";
            limit = 5;
        }

        String key = "rate_limit:" + ip + ":" + endpointGroup;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(60));
        }

        if (count != null && count > limit) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests\",\"retryAfter\":60}");
            return false;
        }

        return true;
    }
}
