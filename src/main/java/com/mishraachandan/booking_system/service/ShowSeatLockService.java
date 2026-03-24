package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.ShowSeat;
import com.mishraachandan.booking_system.dto.entity.SeatStatus;
import com.mishraachandan.booking_system.repository.ShowSeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing temporary seat locks on ShowSeats.
 * A user locks seats while selecting, then confirms/pays within a TTL window.
 */
@Service
public class ShowSeatLockService {

    private static final Logger logger = LoggerFactory.getLogger(ShowSeatLockService.class);

    // Lock timeout in minutes
    private static final int LOCK_TIMEOUT_MINUTES = 8;

    private final ShowSeatRepository showSeatRepository;

    public ShowSeatLockService(ShowSeatRepository showSeatRepository) {
        this.showSeatRepository = showSeatRepository;
    }

    /**
     * Lock a single ShowSeat for a user.
     *
     * @return true if lock was successful, false otherwise.
     */
    @Transactional
    public boolean lockShowSeat(Long showSeatId, Long userId) {
        ShowSeat showSeat = showSeatRepository.findById(showSeatId)
                .orElseThrow(() -> new IllegalArgumentException("ShowSeat not found: " + showSeatId));

        if (showSeat.getStatus() != SeatStatus.AVAILABLE) {
            logger.info("ShowSeat {} is not available. Current status: {}", showSeatId, showSeat.getStatus());
            return false;
        }

        showSeat.setStatus(SeatStatus.LOCKED);
        showSeat.setLockedAt(LocalDateTime.now());
        showSeat.setLockedByUserId(userId);
        showSeatRepository.save(showSeat);

        logger.info("ShowSeat {} locked by user {}", showSeatId, userId);
        return true;
    }

    /**
     * Lock multiple ShowSeats for a user atomically.
     * If any seat is not available, none are locked.
     */
    @Transactional
    public boolean lockShowSeats(List<Long> showSeatIds, Long userId) {
        List<ShowSeat> showSeats = showSeatRepository.findAllById(showSeatIds);

        if (showSeats.size() != showSeatIds.size()) {
            throw new IllegalArgumentException("Some ShowSeat IDs were not found");
        }

        // Check all are available first
        for (ShowSeat ss : showSeats) {
            if (ss.getStatus() != SeatStatus.AVAILABLE) {
                logger.info("ShowSeat {} is not available. Current status: {}", ss.getId(), ss.getStatus());
                return false;
            }
        }

        // Lock all
        LocalDateTime now = LocalDateTime.now();
        for (ShowSeat ss : showSeats) {
            ss.setStatus(SeatStatus.LOCKED);
            ss.setLockedAt(now);
            ss.setLockedByUserId(userId);
        }
        showSeatRepository.saveAll(showSeats);

        logger.info("Locked {} ShowSeats for user {}", showSeats.size(), userId);
        return true;
    }

    /**
     * Unlock a ShowSeat (e.g., user deselects or booking fails).
     */
    @Transactional
    public void unlockShowSeat(Long showSeatId) {
        showSeatRepository.findById(showSeatId).ifPresent(ss -> {
            ss.setStatus(SeatStatus.AVAILABLE);
            ss.setLockedAt(null);
            ss.setLockedByUserId(null);
            showSeatRepository.save(ss);
            logger.info("ShowSeat {} unlocked", showSeatId);
        });
    }

    /**
     * Mark ShowSeat as booked (after successful payment/confirmation).
     */
    @Transactional
    public void markShowSeatAsBooked(Long showSeatId) {
        showSeatRepository.findById(showSeatId).ifPresent(ss -> {
            ss.setStatus(SeatStatus.BOOKED);
            ss.setLockedAt(null);
            ss.setLockedByUserId(null);
            showSeatRepository.save(ss);
            logger.info("ShowSeat {} marked as booked", showSeatId);
        });
    }

    /**
     * Get all available ShowSeats for a specific show.
     */
    public List<ShowSeat> getAvailableShowSeats(Long showId) {
        return showSeatRepository.findByShowIdAndStatus(showId, SeatStatus.AVAILABLE);
    }

    /**
     * Get all ShowSeats for a specific show (any status).
     */
    public List<ShowSeat> getAllShowSeats(Long showId) {
        return showSeatRepository.findByShowId(showId);
    }

    /**
     * Scheduled task to release expired seat locks.
     * Runs every 1 minute.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredLocks() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(LOCK_TIMEOUT_MINUTES);
        int releasedCount = showSeatRepository.releaseExpiredLocks(cutoffTime);

        if (releasedCount > 0) {
            logger.info("Released {} expired ShowSeat locks", releasedCount);
        }
    }
}
