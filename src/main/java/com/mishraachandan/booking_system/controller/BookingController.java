package com.mishraachandan.booking_system.controller;

import com.mishraachandan.booking_system.config.AuthenticatedUser;
import com.mishraachandan.booking_system.dto.entity.Booking;
import com.mishraachandan.booking_system.dto.pojo.BookingRequest;
import com.mishraachandan.booking_system.dto.pojo.BookingResponse;
import com.mishraachandan.booking_system.dto.pojo.ShowSeatBookingRequest;
import com.mishraachandan.booking_system.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
     * Place a new generic booking (non-seated events).
     * userId is extracted from the JWT via @AuthenticationPrincipal.
     */
    @PostMapping
    public ResponseEntity<BookingResponse> placeBooking(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody BookingRequest request) {

        Booking booking = bookingService.placeBooking(principal.getUserId(), request);
        return ResponseEntity.ok(BookingResponse.fromBooking(booking));
    }

    /**
     * Book specific ShowSeats for a show.
     * Seats must be locked first via /api/v1/shows/{showId}/seats/lock.
     */
    @PostMapping("/show-seats")
    public ResponseEntity<BookingResponse> bookShowSeats(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ShowSeatBookingRequest request) {

        Booking booking = bookingService.bookShowSeats(principal.getUserId(), request);
        return ResponseEntity.ok(BookingResponse.fromBooking(booking));
    }

    /**
     * Confirm a booking after payment.
     * Only the booking's owner may call this. Payment-gateway signature
     * verification is handled by {@code /api/payments/verify} which invokes
     * the underlying booking state change.
     */
    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        Booking booking = bookingService.confirmBookingForUser(bookingId, principal.getUserId());
        return ResponseEntity.ok(BookingResponse.fromBooking(booking));
    }

    /**
     * Get all bookings for the authenticated user (flat DTO — no lazy loading).
     */
    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal AuthenticatedUser principal) {

        List<BookingResponse> bookings = bookingService.getUserBookingsFlat(principal.getUserId());
        return ResponseEntity.ok(bookings);
    }

    /**
     * Cancel a booking.
     */
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        bookingService.cancelBooking(bookingId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
