package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.Booking;
import com.mishraachandan.booking_system.dto.entity.ShowSeat;
import com.mishraachandan.booking_system.dto.pojo.BookingEvent;
import com.mishraachandan.booking_system.dto.pojo.BookingEvent.EventType;
import com.mishraachandan.booking_system.repository.ShowSeatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookingEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ShowSeatRepository showSeatRepository;

    public BookingEventProducer(KafkaTemplate<String, Object> kafkaTemplate, ShowSeatRepository showSeatRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.showSeatRepository = showSeatRepository;
    }

    public void publishEvent(Booking booking, EventType eventType) {
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

            log.info("Publishing BookingEvent to Kafka: {} for bookingId {}", eventType, booking.getId());
            kafkaTemplate.send("booking-events", "booking-" + booking.getId(), event);
        } catch (Exception e) {
            log.error("Failed to publish BookingEvent to Kafka for bookingId: {}", booking.getId(), e);
        }
    }
}
