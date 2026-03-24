package com.mishraachandan.booking_system.repository;

import com.mishraachandan.booking_system.dto.entity.ShowSeat;
import com.mishraachandan.booking_system.dto.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowId(Long showId);

    List<ShowSeat> findByShowIdAndStatus(Long showId, SeatStatus status);

    /**
     * Find show seats that have been locked past the cutoff time.
     */
    @Query("SELECT ss FROM ShowSeat ss WHERE ss.status = 'LOCKED' AND ss.lockedAt < :cutoffTime")
    List<ShowSeat> findExpiredLocks(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Release expired locks in bulk.
     */
    @Modifying
    @Query("UPDATE ShowSeat ss SET ss.status = 'AVAILABLE', ss.lockedAt = null, ss.lockedByUserId = null " +
            "WHERE ss.status = 'LOCKED' AND ss.lockedAt < :cutoffTime")
    int releaseExpiredLocks(@Param("cutoffTime") LocalDateTime cutoffTime);
}
