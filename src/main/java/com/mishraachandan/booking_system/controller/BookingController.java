package com.mishraachandan.booking_system.controller;

import com.mishraachandan.booking_system.dto.entity.Booking;
import com.mishraachandan.booking_system.dto.pojo.BookingRequest;
import com.mishraachandan.booking_system.dto.pojo.SeatBookingRequest;
import com.mishraachandan.booking_system.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Place a new booking.
     * In production, userId should come from authenticated user context.
     */
    @PostMapping
    public ResponseEntity<Booking> placeBooking(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody BookingRequest request) {

        Booking booking = bookingService.placeBooking(userId, request);
        return ResponseEntity.ok(booking);
    }

    /**
     * Book specific seats for an event.
     */
    @PostMapping("/seat")
    public ResponseEntity<Booking> bookSeats(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SeatBookingRequest request) {

        Booking booking = bookingService.bookSeats(userId, request);
        return ResponseEntity.ok(booking);
    }

    /**
     * Get all bookings for the authenticated user.
     */
    @GetMapping("/my")
    public ResponseEntity<List<Booking>> getMyBookings(@RequestHeader("X-User-Id") Long userId) {
        List<Booking> bookings = bookingService.getUserBookings(userId);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Cancel a booking.
     */
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long userId) {

        bookingService.cancelBooking(bookingId, userId);
        return ResponseEntity.noContent().build();
    }
}
