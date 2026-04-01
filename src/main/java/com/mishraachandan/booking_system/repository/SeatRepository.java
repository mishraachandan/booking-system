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

    List<Seat> findByScreenId(Long screenId);

    Optional<Seat> findByScreenIdAndSeatNumber(Long screenId, String seatNumber);
}
