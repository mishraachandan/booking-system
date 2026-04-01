package com.mishraachandan.booking_system.dto.pojo;

import com.mishraachandan.booking_system.dto.entity.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Flat DTO for the seat selection screen.
 * No Hibernate proxies, no circular references.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowSeatResponse {
    // ShowSeat fields
    private Long showSeatId;
    private BigDecimal price;
    private SeatStatus status;
    private Long lockedByUserId;

    // Seat fields
    private Long seatId;
    private String seatNumber;
    private String seatType;

    // Show fields
    private Long showId;
    private String startTime;
    private String endTime;

    // Movie fields
    private String movieTitle;
    private String moviePosterUrl;
    private String movieGenre;
    private String movieLanguage;
    private Integer movieDurationMinutes;

    // Screen / Cinema fields
    private String screenName;
    private String cinemaName;
    private String cinemaAddress;
    private String cityName;
}
