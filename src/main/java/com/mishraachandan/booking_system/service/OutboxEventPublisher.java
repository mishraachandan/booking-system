package com.mishraachandan.booking_system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishraachandan.booking_system.dto.entity.OutboxEvent;
import com.mishraachandan.booking_system.dto.pojo.BookingEvent;
import com.mishraachandan.booking_system.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background worker that continuously polls the outbox_events table for PENDING
 * events, publishes them to Kafka with delivery callbacks, and updates the outbox status.
 *
 * Implements the guaranteed at-least-once delivery guarantee of the Transactional Outbox Pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${outbox.batch-size:50}")
    private int batchSize;

    @Value("${outbox.max-retries:5}")
    private int maxRetries;

    private static final String TOPIC_BOOKING_EVENTS = "booking-events";
    private static final String TOPIC_BOOKING_EVENTS_DLQ = "booking-events-dlq";

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(
                OutboxEvent.Status.PENDING, PageRequest.of(0, batchSize));

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Outbox publisher found {} pending events to dispatch", pendingEvents.size());

        for (OutboxEvent outboxEvent : pendingEvents) {
            dispatchSingleEvent(outboxEvent);
        }
    }

    private void dispatchSingleEvent(OutboxEvent outboxEvent) {
        try {
            Object eventPayload;
            if ("BOOKING".equalsIgnoreCase(outboxEvent.getAggregateType())) {
                eventPayload = objectMapper.readValue(outboxEvent.getPayload(), BookingEvent.class);
            } else {
                eventPayload = outboxEvent.getPayload();
            }

            String messageKey = outboxEvent.getAggregateType().toLowerCase() + "-" + outboxEvent.getAggregateId();

            kafkaTemplate.send(TOPIC_BOOKING_EVENTS, messageKey, eventPayload)
                    .whenComplete((sendResult, throwable) -> {
                        if (throwable == null) {
                            outboxEvent.setStatus(OutboxEvent.Status.PROCESSED);
                            outboxEvent.setProcessedAt(LocalDateTime.now());
                            outboxEventRepository.save(outboxEvent);
                            log.info("Successfully published Outbox event #{} [aggregate: {}] to Kafka",
                                    outboxEvent.getId(), messageKey);
                        } else {
                            handlePublishFailure(outboxEvent, throwable);
                        }
                    });

        } catch (Exception ex) {
            log.error("Error preparing Outbox event #{} for Kafka: {}", outboxEvent.getId(), ex.getMessage());
            handlePublishFailure(outboxEvent, ex);
        }
    }

    private void handlePublishFailure(OutboxEvent outboxEvent, Throwable throwable) {
        int currentRetries = outboxEvent.getRetryCount() + 1;
        outboxEvent.setRetryCount(currentRetries);
        outboxEvent.setLastError(throwable != null ? throwable.getMessage() : "Unknown error");

        if (currentRetries >= maxRetries) {
            outboxEvent.setStatus(OutboxEvent.Status.FAILED);
            log.error("Outbox event #{} reached max retries ({}/{}). Routing to DLQ: {}",
                    outboxEvent.getId(), currentRetries, maxRetries, TOPIC_BOOKING_EVENTS_DLQ);
            try {
                kafkaTemplate.send(TOPIC_BOOKING_EVENTS_DLQ, "dlq-" + outboxEvent.getAggregateId(), outboxEvent.getPayload());
            } catch (Exception dlqEx) {
                log.error("Failed to send Outbox event #{} to DLQ: {}", outboxEvent.getId(), dlqEx.getMessage());
            }
        } else {
            log.warn("Failed to publish Outbox event #{} (attempt {}/{}). Will retry: {}",
                    outboxEvent.getId(), currentRetries, maxRetries, throwable != null ? throwable.getMessage() : "Unknown");
        }

        outboxEventRepository.save(outboxEvent);
    }
}
