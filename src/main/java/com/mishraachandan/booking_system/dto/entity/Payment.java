package com.mishraachandan.booking_system.dto.entity;

import com.mishraachandan.booking_system.dto.status.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The booking this payment belongs to */
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    /** The user who made the payment */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Amount charged in smallest currency unit (paise for INR) */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    /** Razorpay order ID returned when creating the order */
    @Column(name = "razorpay_order_id", unique = true)
    private String razorpayOrderId;

    /** Razorpay payment ID returned after successful payment */
    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    /** HMAC-SHA256 signature for payment verification */
    @Column(name = "razorpay_signature")
    private String razorpaySignature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.CREATED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
