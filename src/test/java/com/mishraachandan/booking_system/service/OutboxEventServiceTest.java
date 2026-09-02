package com.mishraachandan.booking_system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishraachandan.booking_system.dto.entity.*;
import com.mishraachandan.booking_system.dto.pojo.BookingEvent;
import com.mishraachandan.booking_system.repository.OutboxEventRepository;
import com.mishraachandan.booking_system.repository.ShowSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ShowSeatRepository showSeatRepository;

    private ObjectMapper objectMapper;
    private OutboxEventService outboxEventService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        outboxEventService = new OutboxEventService(outboxEventRepository, showSeatRepository, objectMapper);
    }

    @Test
    void testRecordBookingEvent_Confirmed_ShouldPersistPendingOutboxRecord() throws Exception {
        User user = User.builder().id(10L).email("user@test.com").build();
        Movie movie = Movie.builder().id(1L).title("Inception").build();
        Show show = Show.builder().id(5L).movie(movie).build();
        Booking booking = Booking.builder()
                .id(100L)
                .user(user)
                .show(show)
                .build();

        Seat seat = Seat.builder().seatNumber("A1").build();
        ShowSeat showSeat = ShowSeat.builder().seat(seat).price(BigDecimal.valueOf(350)).build();
        when(showSeatRepository.findByBookingId(100L)).thenReturn(List.of(showSeat));

        outboxEventService.recordBookingEvent(booking, BookingEvent.EventType.CONFIRMED);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertEquals("BOOKING", saved.getAggregateType());
        assertEquals("100", saved.getAggregateId());
        assertEquals("CONFIRMED", saved.getEventType());
        assertEquals(OutboxEvent.Status.PENDING, saved.getStatus());
        assertEquals(0, saved.getRetryCount());
        assertNotNull(saved.getPayload());
        assertTrue(saved.getPayload().contains("Inception"));
        assertTrue(saved.getPayload().contains("user@test.com"));
    }
}
