package com.mishraachandan.booking_system.dto.status;

public enum PaymentStatus {
    CREATED,   // Razorpay order created, waiting for user to pay
    SUCCESS,   // Payment verified and booking confirmed
    FAILED     // Payment failed or could not be verified
}
