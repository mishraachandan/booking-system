package com.mishraachandan.booking_system.repository;

import com.mishraachandan.booking_system.dto.entity.ShowSeat;
import com.mishraachandan.booking_system.dto.entity.SeatStatus;
import com.mishraachandan.booking_system.dto.pojo.ShowSeatResponse;
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
     * Flat DTO query — single SQL join, zero lazy loading, no proxy issues.
     */
    @Query("""
            SELECT new com.mishraachandan.booking_system.dto.pojo.ShowSeatResponse(
                ss.id, ss.price, ss.status, ss.lockedByUserId,
                s.id, s.seatNumber, CAST(s.seatType AS string),
                sh.id, CAST(sh.startTime AS string), CAST(sh.endTime AS string),
                m.title, m.posterUrl, m.genre, m.language, m.durationMinutes,
                scr.name, c.name, c.address, ci.name
            )
            FROM ShowSeat ss
            JOIN ss.seat s
            JOIN ss.show sh
            JOIN sh.movie m
            JOIN sh.screen scr
            JOIN scr.cinema c
            JOIN c.city ci
            WHERE sh.id = :showId
            ORDER BY s.seatNumber
            """)
    List<ShowSeatResponse> findShowSeatResponsesByShowId(@Param("showId") Long showId);

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
