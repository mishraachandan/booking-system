package com.mishraachandan.booking_system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishraachandan.booking_system.dto.entity.Booking;
import com.mishraachandan.booking_system.dto.entity.OutboxEvent;
import com.mishraachandan.booking_system.dto.entity.ShowSeat;
import com.mishraachandan.booking_system.dto.pojo.BookingEvent;
import com.mishraachandan.booking_system.dto.pojo.BookingEvent.EventType;
import com.mishraachandan.booking_system.repository.OutboxEventRepository;
import com.mishraachandan.booking_system.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for persisting domain events into the outbox_events table
 * inside the same database transaction as the business operation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ObjectMapper objectMapper;

    /**
     * Persists a BookingEvent into the transactional outbox table.
     * Guaranteed to commit or roll back with the active database transaction.
     */
    @Transactional
    public void recordBookingEvent(Booking booking, EventType eventType) {
        try {
            List<ShowSeat> seats = Collections.emptyList();
            BigDecimal amount = BigDecimal.ZERO;
            String movieTitle = null;

            if (booking.getShow() != null) {
                seats = showSeatRepository.findByBookingId(booking.getId());
                amount = seats.stream()
                        .map(ShowSeat::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (booking.getShow().getMovie() != null) {
                    movieTitle = booking.getShow().getMovie().getTitle();
                }
            } else if (booking.getResource() != null) {
                amount = booking.getResource().getPrice() != null ?
                        booking.getResource().getPrice().multiply(BigDecimal.valueOf(booking.getNumberOfTickets())) :
                        BigDecimal.ZERO;
            }

            List<String> seatNumbers = seats.stream()
                    .map(ss -> ss.getSeat().getSeatNumber())
                    .collect(Collectors.toList());

            BookingEvent event = BookingEvent.builder()
                    .bookingId(booking.getId())
                    .email(booking.getUser() != null ? booking.getUser().getEmail() : null)
                    .movieTitle(movieTitle)
                    .resourceName(booking.getResource() != null ? booking.getResource().getName() : null)
                    .seats(seatNumbers)
                    .amount(amount)
                    .eventType(eventType)
                    .timestamp(LocalDateTime.now())
                    .build();

            String payloadJson = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("BOOKING")
                    .aggregateId(String.valueOf(booking.getId()))
                    .eventType(eventType.name())
                    .payload(payloadJson)
                    .status(OutboxEvent.Status.PENDING)
                    .retryCount(0)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Recorded Transactional Outbox event [{} - {}] for booking #{}",
                    outboxEvent.getAggregateType(), outboxEvent.getEventType(), booking.getId());

        } catch (Exception e) {
            log.error("Failed to save Outbox event for booking #{}: {}", booking.getId(), e.getMessage(), e);
            throw new RuntimeException("Could not persist outbox event", e);
        }
    }
}
