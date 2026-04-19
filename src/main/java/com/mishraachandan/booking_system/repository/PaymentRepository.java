package com.mishraachandan.booking_system.repository;

import com.mishraachandan.booking_system.dto.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Get all payments for a booking (there may be retries) */
    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    /** Lookup by Razorpay order ID (used during verify callback) */
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    /** Get the latest payment for a booking */
    Optional<Payment> findTopByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
