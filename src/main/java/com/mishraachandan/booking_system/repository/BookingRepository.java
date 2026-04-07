package com.mishraachandan.booking_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.mishraachandan.booking_system.dto.entity.Booking;
import com.mishraachandan.booking_system.dto.pojo.BookingResponse;
import com.mishraachandan.booking_system.dto.status.BookingStatus;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByResourceId(Long resourceId);
    List<Booking> findByStartTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<Booking> findByEndTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<Booking> findByStatus(BookingStatus status);

    @Query("""
        SELECT new com.mishraachandan.booking_system.dto.pojo.BookingResponse(
            b.id, b.status, b.numberOfTickets, b.notes,
            b.startTime, b.endTime, b.createdAt,
            u.id, u.firstName, u.lastName, u.email,
            s.id, m.title, m.genre, m.durationMinutes,
            sc.name, c.name, ci.name, s.startTime
        )
        FROM Booking b
        JOIN b.user u
        LEFT JOIN b.show s
        LEFT JOIN s.movie m
        LEFT JOIN s.screen sc
        LEFT JOIN sc.cinema c
        LEFT JOIN c.city ci
        WHERE u.id = :userId
        ORDER BY b.createdAt DESC
    """)
    List<BookingResponse> findBookingResponsesByUserId(@Param("userId") Long userId);
}
