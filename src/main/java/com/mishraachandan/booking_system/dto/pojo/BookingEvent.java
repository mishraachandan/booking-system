package com.mishraachandan.booking_system.dto.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingEvent {

    public enum EventType {
        CREATED,
        CONFIRMED,
        EXPIRED,
        CANCELLED
    }

    private Long bookingId;
    private String email;
    private String movieTitle;
    private String resourceName;
    private List<String> seats;
    private BigDecimal amount;
    private EventType eventType;
    private LocalDateTime timestamp;
}
