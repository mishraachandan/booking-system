package com.mishraachandan.booking_system.controller;

import com.mishraachandan.booking_system.config.AuthenticatedUser;
import com.mishraachandan.booking_system.dto.entity.Booking;
import com.mishraachandan.booking_system.dto.entity.Payment;
import com.mishraachandan.booking_system.dto.pojo.BookingResponse;
import com.mishraachandan.booking_system.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Step 1 — Create a Razorpay order for a booking.
     * Returns: { razorpayOrderId, amount (paise), currency, keyId, bookingId }
     *
     * POST /api/payments/create-order
     * Body: { "bookingId": 123 }
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody Map<String, Long> body) {

        Long bookingId = body.get("bookingId");
        if (bookingId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "bookingId is required"));
        }

        try {
            Map<String, Object> orderDetails = paymentService.createOrder(bookingId, principal.getUserId());
            return ResponseEntity.ok(orderDetails);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Step 2 — Verify Razorpay payment signature and confirm the booking.
     *
     * POST /api/payments/verify
     * Body: { "razorpayOrderId", "razorpayPaymentId", "razorpaySignature" }
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> body) {

        String razorpayOrderId = body.get("razorpayOrderId");
        String razorpayPaymentId = body.get("razorpayPaymentId");
        String razorpaySignature = body.get("razorpaySignature");

        if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "razorpayOrderId, razorpayPaymentId and razorpaySignature are all required"));
        }

        try {
            Booking confirmed = paymentService.verifyAndConfirm(razorpayOrderId, razorpayPaymentId, razorpaySignature);
            return ResponseEntity.ok(BookingResponse.fromBooking(confirmed));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(400).body(Map.of("error", "Payment verification failed. Please try again."));
        }
    }

    /**
     * Get the latest payment record for a booking.
     * GET /api/payments/booking/{bookingId}
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<?> getPaymentStatus(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        try {
            Payment payment = paymentService.getLatestPaymentForBooking(bookingId);
            return ResponseEntity.ok(Map.of(
                    "paymentId", payment.getId(),
                    "bookingId", payment.getBookingId(),
                    "status", payment.getStatus(),
                    "amount", payment.getAmount(),
                    "razorpayOrderId", payment.getRazorpayOrderId() != null ? payment.getRazorpayOrderId() : "",
                    "razorpayPaymentId", payment.getRazorpayPaymentId() != null ? payment.getRazorpayPaymentId() : "",
                    "createdAt", payment.getCreatedAt()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
