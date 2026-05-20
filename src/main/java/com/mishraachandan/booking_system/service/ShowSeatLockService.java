package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.ShowSeat;
import com.mishraachandan.booking_system.dto.entity.SeatStatus;
import com.mishraachandan.booking_system.dto.pojo.ShowSeatResponse;
import com.mishraachandan.booking_system.repository.ShowSeatRepository;
import com.mishraachandan.booking_system.config.SeatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
public class ShowSeatLockService {

    private static final Logger logger = LoggerFactory.getLogger(ShowSeatLockService.class);

    private static final int LOCK_TIMEOUT_MINUTES = 8;

    private final ShowSeatRepository showSeatRepository;
    private final RedisLockService redisLockService;
    private final SeatWebSocketHandler seatWebSocketHandler;

    public ShowSeatLockService(ShowSeatRepository showSeatRepository,
                               RedisLockService redisLockService,
                               SeatWebSocketHandler seatWebSocketHandler) {
        this.showSeatRepository = showSeatRepository;
        this.redisLockService = redisLockService;
        this.seatWebSocketHandler = seatWebSocketHandler;
    }

    /**
     * Lock a single ShowSeat for a user.
     */
    @Transactional
    public boolean lockShowSeat(Long showSeatId, Long userId) {
        // 1. Check Redis lock status first
        if (redisLockService.isLocked(showSeatId)) {
            logger.info("Seat {} is already locked in Redis", showSeatId);
            return false;
        }

        ShowSeat showSeat = showSeatRepository.findById(showSeatId)
                .orElseThrow(() -> new IllegalArgumentException("ShowSeat not found: " + showSeatId));

        // 2. Validate DB status
        if (showSeat.getStatus() != SeatStatus.AVAILABLE) {
            logger.info("ShowSeat {} is not available in DB. Status: {}", showSeatId, showSeat.getStatus());
            return false;
        }

        // 3. Acquire Redis lock
        boolean redisSuccess = redisLockService.lockSeat(showSeatId, userId, Duration.ofMinutes(LOCK_TIMEOUT_MINUTES));
        if (!redisSuccess) {
            return false;
        }

        // 4. Update DB status
        showSeat.setStatus(SeatStatus.LOCKED);
        showSeat.setLockedAt(LocalDateTime.now());
        showSeat.setLockedByUserId(userId);
        showSeatRepository.save(showSeat);

        // 5. Broadcast to WebSockets
        seatWebSocketHandler.broadcastSeatStatus(showSeat.getShow().getId(), showSeatId, "LOCKED", userId);

        logger.info("ShowSeat {} locked in Redis and DB by user {}", showSeatId, userId);
        return true;
    }

    /**
     * Lock multiple ShowSeats for a user atomically.
     */
    @Transactional
    public boolean lockShowSeats(List<Long> showSeatIds, Long userId) {
        // 1. Pre-check all seats in Redis
        for (Long id : showSeatIds) {
            if (redisLockService.isLocked(id)) {
                logger.info("Seat {} is already locked in Redis", id);
                return false;
            }
        }

        List<ShowSeat> showSeats = showSeatRepository.findAllById(showSeatIds);
        if (showSeats.size() != showSeatIds.size()) {
            throw new IllegalArgumentException("Some ShowSeat IDs were not found");
        }

        // 2. Pre-check all seats in DB
        for (ShowSeat ss : showSeats) {
            if (ss.getStatus() != SeatStatus.AVAILABLE) {
                logger.info("ShowSeat {} is not available in DB. Status: {}", ss.getId(), ss.getStatus());
                return false;
            }
        }

        // 3. Acquire all Redis locks
        List<Long> acquiredLocks = new ArrayList<>();
        for (Long id : showSeatIds) {
            boolean success = redisLockService.lockSeat(id, userId, Duration.ofMinutes(LOCK_TIMEOUT_MINUTES));
            if (success) {
                acquiredLocks.add(id);
            } else {
                // Rollback acquired locks in Redis on partial failure
                redisLockService.unlockSeats(acquiredLocks);
                return false;
            }
        }

        // 4. Update DB status
        LocalDateTime now = LocalDateTime.now();
        for (ShowSeat ss : showSeats) {
            ss.setStatus(SeatStatus.LOCKED);
            ss.setLockedAt(now);
            ss.setLockedByUserId(userId);
        }
        showSeatRepository.saveAll(showSeats);

        // 5. Broadcast to WebSockets
        for (ShowSeat ss : showSeats) {
            seatWebSocketHandler.broadcastSeatStatus(ss.getShow().getId(), ss.getId(), "LOCKED", userId);
        }

        logger.info("Locked {} ShowSeats in Redis and DB for user {}", showSeats.size(), userId);
        return true;
    }

    /**
     * Unlock a ShowSeat (e.g., user deselects or booking fails).
     */
    @Transactional
    public void unlockShowSeat(Long showSeatId) {
        redisLockService.unlockSeat(showSeatId);
        showSeatRepository.findById(showSeatId).ifPresent(ss -> {
            ss.setStatus(SeatStatus.AVAILABLE);
            ss.setLockedAt(null);
            ss.setLockedByUserId(null);
            showSeatRepository.save(ss);
            seatWebSocketHandler.broadcastSeatStatus(ss.getShow().getId(), ss.getId(), "AVAILABLE", null);
            logger.info("ShowSeat {} unlocked in Redis and DB", showSeatId);
        });
    }

    /**
     * Mark ShowSeat as booked (after successful payment/confirmation).
     */
    @Transactional
    public void markShowSeatAsBooked(Long showSeatId) {
        redisLockService.unlockSeat(showSeatId);
        showSeatRepository.findById(showSeatId).ifPresent(ss -> {
            ss.setStatus(SeatStatus.BOOKED);
            ss.setLockedAt(null);
            ss.setLockedByUserId(null);
            showSeatRepository.save(ss);
            seatWebSocketHandler.broadcastSeatStatus(ss.getShow().getId(), ss.getId(), "BOOKED", null);
            logger.info("ShowSeat {} marked as booked", showSeatId);
        });
    }

    /**
     * Get all available ShowSeats for a specific show.
     */
    @Transactional(readOnly = true)
    public List<ShowSeat> getAvailableShowSeats(Long showId) {
        return showSeatRepository.findByShowIdAndStatus(showId, SeatStatus.AVAILABLE);
    }

    /**
     * Get all ShowSeats for a specific show (any status).
     */
    @Transactional(readOnly = true)
    public List<ShowSeat> getAllShowSeats(Long showId) {
        return showSeatRepository.findByShowId(showId);
    }

    /**
     * Get all ShowSeats for a show as a flat, serialization-safe DTO list.
     */
    @Transactional(readOnly = true)
    public List<ShowSeatResponse> getAllShowSeatResponses(Long showId) {
        return showSeatRepository.findShowSeatResponsesByShowId(showId);
    }

    /**
     * Scheduled task to release expired seat locks.
     * Evaluates both DB state and Redis lock existence.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredLocks() {
        // Query locked seats from DB
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(LOCK_TIMEOUT_MINUTES);
        List<ShowSeat> dbLockedSeats = showSeatRepository.findExpiredLocks(cutoffTime);

        int releasedCount = 0;
        for (ShowSeat ss : dbLockedSeats) {
            // If the Redis lock key is gone, heal/release DB status
            if (!redisLockService.isLocked(ss.getId())) {
                ss.setStatus(SeatStatus.AVAILABLE);
                ss.setLockedAt(null);
                ss.setLockedByUserId(null);
                showSeatRepository.save(ss);
                seatWebSocketHandler.broadcastSeatStatus(ss.getShow().getId(), ss.getId(), "AVAILABLE", null);
                releasedCount++;
            }
        }

        if (releasedCount > 0) {
            logger.info("Released {} expired seat locks using Redis TTL check", releasedCount);
        }
    }
}
