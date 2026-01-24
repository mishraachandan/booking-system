package com.mishraachandan.booking_system.repository;

import com.mishraachandan.booking_system.dto.entity.Seat;
import com.mishraachandan.booking_system.dto.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByResourceIdAndStatus(Long resourceId, SeatStatus status);

    Optional<Seat> findByResourceIdAndSeatNumber(Long resourceId, String seatNumber);

    List<Seat> findByResourceId(Long resourceId);

    /**
     * Find all seats that have been locked for longer than the specified time.
     */
    @Query("SELECT s FROM Seat s WHERE s.status = 'LOCKED' AND s.lockedAt < :cutoffTime")
    List<Seat> findExpiredLocks(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Release expired locks in bulk.
     */
    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.lockedAt = null, s.lockedByUserId = null WHERE s.status = 'LOCKED' AND s.lockedAt < :cutoffTime")
    int releaseExpiredLocks(@Param("cutoffTime") LocalDateTime cutoffTime);
}
