package com.mishraachandan.booking_system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishraachandan.booking_system.dto.entity.IdempotencyRecord;
import com.mishraachandan.booking_system.repository.IdempotencyRecordRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private static final String REDIS_PREFIX = "idempotency:";
    private final IdempotencyRecordRepository recordRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CachedResponse {
        private int statusCode;
        private String responseBody;
    }

    /**
     * Attempts to acquire an idempotency lock for the given key.
     *
     * @return Optional containing the cached response if already completed,
     *         or Optional.empty() if this is a new request that can proceed.
     * @throws ResponseStatusException with 409 CONFLICT if a request with this key is currently in progress.
     */
    @Transactional
    public Optional<CachedResponse> startOrGet(String key, Long userId, String path, long expireMinutes) {
        String redisKey = REDIS_PREFIX + key;

        // 1. Check Redis first for completed response
        try {
            String cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                if ("PROCESSING".equals(cached)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Concurrent request with Idempotency-Key '" + key + "' is currently in progress. Please retry shortly.");
                }
                CachedResponse response = objectMapper.readValue(cached, CachedResponse.class);
                log.info("Idempotency HIT (Redis) for key: {}", key);
                return Optional.of(response);
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis check failed for idempotency key {}: {}", key, e.getMessage());
        }

        // 2. Check Database record
        Optional<IdempotencyRecord> existingOpt = recordRepository.findByIdempotencyKey(key);
        if (existingOpt.isPresent()) {
            IdempotencyRecord existing = existingOpt.get();
            if (existing.getStatus() == IdempotencyRecord.Status.PROCESSING) {
                // If it was created within the last 5 minutes, consider it still in flight
                if (existing.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(5))) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Concurrent request with Idempotency-Key '" + key + "' is currently in progress.");
                } else {
                    // Stale processing record, update to FAILED and allow re-try
                    existing.setStatus(IdempotencyRecord.Status.FAILED);
                    recordRepository.save(existing);
                }
            } else if (existing.getStatus() == IdempotencyRecord.Status.COMPLETED) {
                log.info("Idempotency HIT (DB) for key: {}", key);
                CachedResponse res = CachedResponse.builder()
                        .statusCode(existing.getResponseStatus() != null ? existing.getResponseStatus() : 200)
                        .responseBody(existing.getResponseBody())
                        .build();
                // Backfill Redis cache
                try {
                    redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(res), Duration.ofMinutes(expireMinutes));
                } catch (Exception e) {
                    log.warn("Could not backfill Redis idempotency cache: {}", e.getMessage());
                }
                return Optional.of(res);
            }
        }

        // 3. Mark as PROCESSING in Redis (5-min lock) and Database
        try {
            redisTemplate.opsForValue().set(redisKey, "PROCESSING", Duration.ofMinutes(5));
        } catch (Exception e) {
            log.warn("Could not set PROCESSING in Redis for key {}: {}", key, e.getMessage());
        }

        IdempotencyRecord newRecord = existingOpt.orElse(IdempotencyRecord.builder()
                .idempotencyKey(key)
                .userId(userId)
                .requestPath(path)
                .build());
        newRecord.setStatus(IdempotencyRecord.Status.PROCESSING);
        newRecord.setExpiresAt(LocalDateTime.now().plusMinutes(expireMinutes));
        recordRepository.save(newRecord);

        return Optional.empty();
    }

    /**
     * Marks the idempotency key as successfully completed and caches the result.
     */
    @Transactional
    public void markCompleted(String key, int statusCode, String responseBody, long expireMinutes) {
        String redisKey = REDIS_PREFIX + key;
        CachedResponse res = CachedResponse.builder()
                .statusCode(statusCode)
                .responseBody(responseBody)
                .build();

        // 1. Update DB
        recordRepository.findByIdempotencyKey(key).ifPresent(record -> {
            record.setStatus(IdempotencyRecord.Status.COMPLETED);
            record.setResponseStatus(statusCode);
            record.setResponseBody(responseBody);
            recordRepository.save(record);
        });

        // 2. Cache in Redis
        try {
            redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(res), Duration.ofMinutes(expireMinutes));
            log.info("Idempotency record marked COMPLETED for key: {}", key);
        } catch (Exception e) {
            log.warn("Could not cache completed idempotency response in Redis: {}", e.getMessage());
        }
    }

    /**
     * Marks the idempotency key as failed if an exception was thrown.
     */
    @Transactional
    public void markFailed(String key) {
        String redisKey = REDIS_PREFIX + key;
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("Could not evict idempotency key from Redis on failure: {}", e.getMessage());
        }

        recordRepository.findByIdempotencyKey(key).ifPresent(record -> {
            record.setStatus(IdempotencyRecord.Status.FAILED);
            recordRepository.save(record);
            log.info("Idempotency record marked FAILED for key: {}", key);
        });
    }
}
