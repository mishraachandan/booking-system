package com.mishraachandan.booking_system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        try {
            // Test connection on startup
            if (redisTemplate.getConnectionFactory() != null) {
                redisTemplate.getConnectionFactory().getConnection().ping();
                log.info("Redis connection test successful. Redis is active.");
            }
        } catch (Exception e) {
            log.warn("Redis connection test failed on startup: {}. Fallback logic enabled.", e.getMessage());
        }
    }

    public boolean lockSeat(Long seatId, Long userId, Duration duration) {
        try {
            String key = "lock:showseat:" + seatId;
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(userId), duration);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.warn("Redis lock command failed (falling back to DB check/lock): {}", e.getMessage());
            return true; // Fallback to DB check
        }
    }

    public void unlockSeat(Long seatId) {
        try {
            redisTemplate.delete("lock:showseat:" + seatId);
        } catch (Exception e) {
            log.warn("Redis delete command failed for seatId {}: {}", seatId, e.getMessage());
        }
    }

    public void unlockSeats(List<Long> seatIds) {
        try {
            List<String> keys = seatIds.stream().map(id -> "lock:showseat:" + id).toList();
            redisTemplate.delete(keys);
        } catch (Exception e) {
            log.warn("Redis delete keys failed: {}", e.getMessage());
        }
    }

    public boolean isLocked(Long seatId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey("lock:showseat:" + seatId));
        } catch (Exception e) {
            return false;
        }
    }
}
