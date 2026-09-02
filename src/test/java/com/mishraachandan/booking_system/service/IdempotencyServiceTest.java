package com.mishraachandan.booking_system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishraachandan.booking_system.dto.entity.IdempotencyRecord;
import com.mishraachandan.booking_system.repository.IdempotencyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRecordRepository recordRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        idempotencyService = new IdempotencyService(recordRepository, redisTemplate, objectMapper);
    }

    @Test
    void testStartOrGet_NewRequest_ShouldReturnEmptyAndMarkProcessing() {
        String key = "test-key-123";
        when(valueOperations.get("idempotency:" + key)).thenReturn(null);
        when(recordRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());

        Optional<IdempotencyService.CachedResponse> result =
                idempotencyService.startOrGet(key, 1L, "/api/bookings", 60);

        assertTrue(result.isEmpty());
        verify(valueOperations).set(eq("idempotency:" + key), eq("PROCESSING"), any());
        verify(recordRepository).save(any(IdempotencyRecord.class));
    }

    @Test
    void testStartOrGet_ConcurrentProcessing_ShouldThrowConflict() {
        String key = "concurrent-key";
        when(valueOperations.get("idempotency:" + key)).thenReturn("PROCESSING");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                idempotencyService.startOrGet(key, 1L, "/api/bookings", 60));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void testStartOrGet_CompletedInRedis_ShouldReturnCachedResponse() throws Exception {
        String key = "completed-key";
        IdempotencyService.CachedResponse cached = IdempotencyService.CachedResponse.builder()
                .statusCode(200)
                .responseBody("{\"bookingId\":99}")
                .build();
        when(valueOperations.get("idempotency:" + key)).thenReturn(objectMapper.writeValueAsString(cached));

        Optional<IdempotencyService.CachedResponse> result =
                idempotencyService.startOrGet(key, 1L, "/api/bookings", 60);

        assertTrue(result.isPresent());
        assertEquals(200, result.get().getStatusCode());
        assertEquals("{\"bookingId\":99}", result.get().getResponseBody());
        verify(recordRepository, never()).findByIdempotencyKey(any());
    }

    @Test
    void testMarkCompleted_ShouldUpdateDbAndRedis() {
        String key = "complete-key";
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(key)
                .status(IdempotencyRecord.Status.PROCESSING)
                .build();
        when(recordRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(record));

        idempotencyService.markCompleted(key, 201, "{\"status\":\"OK\"}", 60);

        assertEquals(IdempotencyRecord.Status.COMPLETED, record.getStatus());
        assertEquals(201, record.getResponseStatus());
        assertEquals("{\"status\":\"OK\"}", record.getResponseBody());
        verify(recordRepository).save(record);
        verify(valueOperations).set(eq("idempotency:" + key), anyString(), any());
    }

    @Test
    void testMarkFailed_ShouldEvictRedisAndSetFailedInDb() {
        String key = "fail-key";
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(key)
                .status(IdempotencyRecord.Status.PROCESSING)
                .build();
        when(recordRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(record));

        idempotencyService.markFailed(key);

        verify(redisTemplate).delete("idempotency:" + key);
        assertEquals(IdempotencyRecord.Status.FAILED, record.getStatus());
        verify(recordRepository).save(record);
    }
}
