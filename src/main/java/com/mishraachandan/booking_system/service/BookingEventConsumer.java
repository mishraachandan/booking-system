package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.pojo.BookingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BookingEventConsumer {

    private final EmailService emailService;

    public BookingEventConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "booking-events", groupId = "booking-group")
    public void consumeBookingEvent(BookingEvent event) {
        log.info("Received BookingEvent from Kafka: {} for bookingId: {}", event.getEventType(), event.getBookingId());
        
        if (event.getEmail() == null || event.getEmail().isBlank()) {
            log.warn("BookingEvent for bookingId {} has no email, skipping notifications.", event.getBookingId());
            return;
        }

        try {
            switch (event.getEventType()) {
                case CONFIRMED:
                    emailService.sendBookingConfirmationEmail(
                            event.getEmail(),
                            event.getBookingId(),
                            event.getMovieTitle(),
                            event.getResourceName(),
                            event.getSeats(),
                            event.getAmount()
                    );
                    break;
                case EXPIRED:
                case CANCELLED:
                    emailService.sendBookingExpiredEmail(
                            event.getEmail(),
                            event.getBookingId()
                    );
                    break;
                default:
                    log.debug("BookingEvent type {} has no consumer actions defined.", event.getEventType());
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to process consumed BookingEvent for bookingId: {}", event.getBookingId(), e);
        }
    }
}
