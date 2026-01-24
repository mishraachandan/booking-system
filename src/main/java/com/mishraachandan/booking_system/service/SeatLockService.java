package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.Seat;
import com.mishraachandan.booking_system.dto.entity.SeatStatus;
import com.mishraachandan.booking_system.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SeatLockService {

    private static final Logger logger = LoggerFactory.getLogger(SeatLockService.class);

    // Lock timeout in minutes
    private static final int LOCK_TIMEOUT_MINUTES = 5;

    private final SeatRepository seatRepository;

    public SeatLockService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    /**
     * Attempt to lock a seat for a user.
     * 
     * @return true if lock was successful, false otherwise.
     */
    @Transactional
    public boolean lockSeat(Long resourceId, String seatNumber, Long userId) {
        Optional<Seat> seatOpt = seatRepository.findByResourceIdAndSeatNumber(resourceId, seatNumber);

        if (seatOpt.isEmpty()) {
            logger.warn("Seat not found: resourceId={}, seatNumber={}", resourceId, seatNumber);
            return false;
        }

        Seat seat = seatOpt.get();

        // Check if seat is available
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            logger.info("Seat {} is not available. Current status: {}", seatNumber, seat.getStatus());
            return false;
        }

        // Lock the seat
        seat.setStatus(SeatStatus.LOCKED);
        seat.setLockedAt(LocalDateTime.now());
        seat.setLockedByUserId(userId);
        seatRepository.save(seat);

        logger.info("Seat {} locked by user {}", seatNumber, userId);
        return true;
    }

    /**
     * Unlock a seat (e.g., user cancels or booking fails).
     */
    @Transactional
    public void unlockSeat(Long seatId) {
        Optional<Seat> seatOpt = seatRepository.findById(seatId);

        if (seatOpt.isPresent()) {
            Seat seat = seatOpt.get();
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setLockedAt(null);
            seat.setLockedByUserId(null);
            seatRepository.save(seat);
            logger.info("Seat {} unlocked", seatId);
        }
    }

    /**
     * Mark seat as booked (after successful payment).
     */
    @Transactional
    public void markSeatAsBooked(Long seatId) {
        Optional<Seat> seatOpt = seatRepository.findById(seatId);

        if (seatOpt.isPresent()) {
            Seat seat = seatOpt.get();
            seat.setStatus(SeatStatus.BOOKED);
            seat.setLockedAt(null);
            seat.setLockedByUserId(null);
            seatRepository.save(seat);
            logger.info("Seat {} marked as booked", seatId);
        }
    }

    /**
     * Get all available seats for a resource.
     */
    public List<Seat> getAvailableSeats(Long resourceId) {
        return seatRepository.findByResourceIdAndStatus(resourceId, SeatStatus.AVAILABLE);
    }

    /**
     * Scheduled task to release expired seat locks.
     * Runs every 1 minute.
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    @Transactional
    public void releaseExpiredLocks() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(LOCK_TIMEOUT_MINUTES);
        int releasedCount = seatRepository.releaseExpiredLocks(cutoffTime);

        if (releasedCount > 0) {
            logger.info("Released {} expired seat locks", releasedCount);
        }
    }
}
