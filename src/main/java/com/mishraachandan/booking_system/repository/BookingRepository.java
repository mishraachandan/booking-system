package com.mishraachandan.booking_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mishraachandan.booking_system.dto.entity.Booking;
import com.mishraachandan.booking_system.dto.status.BookingStatus;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByResourceId(Long resourceId);
    List<Booking> findByStartTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<Booking> findByEndTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<Booking> findByStatus(BookingStatus status);
}
