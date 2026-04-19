package com.mishraachandan.booking_system.service;

import com.mishraachandan.booking_system.dto.entity.Booking;
import com.mishraachandan.booking_system.dto.entity.Payment;
import com.mishraachandan.booking_system.dto.status.BookingStatus;
import com.mishraachandan.booking_system.dto.status.PaymentStatus;
import com.mishraachandan.booking_system.repository.BookingRepository;
import com.mishraachandan.booking_system.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${razorpay.key-id:RAZORPAY_KEY_NOT_SET}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:RAZORPAY_SECRET_NOT_SET}")
    private String razorpayKeySecret;

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          BookingService bookingService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    // ─── Create Razorpay Order ────────────────────────────────────────────────────

    /**
     * Creates a Razorpay order for a booking and persists a Payment record (CREATED status).
     *
     * @return Map with: razorpayOrderId, amount (paise), currency, keyId, bookingId
     */
    @Transactional
    public Map<String, Object> createOrder(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new SecurityException("User not authorized for this booking");
        }
        if (booking.getStatus() != BookingStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("Booking is not awaiting payment. Status: " + booking.getStatus());
        }

        // Calculate total amount from booked ShowSeats
        BigDecimal totalAmount = calculateBookingAmount(booking);

        // Check if keys are configured (detect defaults and placeholders)
        if (razorpayKeyId.equals("RAZORPAY_KEY_NOT_SET")
                || razorpayKeyId.contains("REPLACE_ME")
                || razorpayKeySecret.contains("REPLACE_ME")) {
            String dummyOrderId = "order_dummy_" + System.currentTimeMillis();
            Payment payment = Payment.builder()
                    .bookingId(bookingId)
                    .userId(userId)
                    .amount(totalAmount)
                    .currency("INR")
                    .razorpayOrderId(dummyOrderId)
                    .status(PaymentStatus.CREATED)
                    .build();
            paymentRepository.save(payment);
            
            logger.info("Created DUMMY order {} for booking {} (₹{})", dummyOrderId, bookingId, totalAmount);
            return Map.of(
                    "razorpayOrderId", dummyOrderId,
                    "amount", totalAmount.multiply(BigDecimal.valueOf(100)).intValue(),
                    "currency", "INR",
                    "keyId", razorpayKeyId,
                    "bookingId", bookingId
            );
        }
        // Razorpay expects amount in paise (smallest unit)
        int amountInPaise = totalAmount.multiply(BigDecimal.valueOf(100)).intValue();

        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "booking_" + bookingId);
            orderRequest.put("notes", new JSONObject()
                    .put("bookingId", bookingId)
                    .put("userId", userId));

            Order razorpayOrder = client.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            // Persist payment record
            Payment payment = Payment.builder()
                    .bookingId(bookingId)
                    .userId(userId)
                    .amount(totalAmount)
                    .currency("INR")
                    .razorpayOrderId(razorpayOrderId)
                    .status(PaymentStatus.CREATED)
                    .build();
            paymentRepository.save(payment);

            logger.info("Razorpay order {} created for booking {} (₹{})", razorpayOrderId, bookingId, totalAmount);

            return Map.of(
                    "razorpayOrderId", razorpayOrderId,
                    "amount", amountInPaise,
                    "currency", "INR",
                    "keyId", razorpayKeyId,
                    "bookingId", bookingId
            );

        } catch (RazorpayException e) {
            logger.error("Failed to create Razorpay order for booking {}: {}", bookingId, e.getMessage());
            throw new RuntimeException("Payment gateway error: " + e.getMessage(), e);
        }
    }

    // ─── Verify Payment ───────────────────────────────────────────────────────────

    /**
     * Verifies the Razorpay payment signature (HMAC-SHA256).
     * On success: confirms the booking.
     * On failure: marks payment FAILED (seats remain BOOKED until 10-min expiry).
     *
     * @return the confirmed Booking on success
     */
    @Transactional
    public Booking verifyAndConfirm(String razorpayOrderId,
                                     String razorpayPaymentId,
                                     String razorpaySignature,
                                     Long userId) {

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for order: " + razorpayOrderId));

        // Only the user who initiated the payment may verify it. Without this
        // check any authenticated user could submit another user's Razorpay
        // order/payment/signature triple (e.g. observed in client logs) and
        // trigger booking confirmation on their behalf.
        if (payment.getUserId() == null || !payment.getUserId().equals(userId)) {
            logger.warn("User {} attempted to verify payment for order {} owned by {}",
                    userId, razorpayOrderId, payment.getUserId());
            throw new SecurityException("User not authorized for this payment");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            // Idempotent — return the already-confirmed booking. Initialize
            // lazy associations so the subsequent BookingResponse.fromBooking
            // mapping does not trip LazyInitializationException with OSIV off.
            return bookingService.findBookingInitialized(payment.getBookingId());
        }

        boolean valid = verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);

        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            logger.warn("Payment signature verification FAILED for order {}", razorpayOrderId);
            throw new SecurityException("Payment signature verification failed");
        }

        // Signature valid — update payment record
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setRazorpaySignature(razorpaySignature);
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        // Confirm the booking
        Booking confirmed = bookingService.confirmBooking(payment.getBookingId());

        logger.info("Payment {} verified. Booking {} confirmed.", razorpayPaymentId, payment.getBookingId());
        return confirmed;
    }

    // ─── Get Payment Status ───────────────────────────────────────────────────────

    public Payment getLatestPaymentForBooking(Long bookingId) {
        return paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("No payment found for booking: " + bookingId));
    }

    // ─── Internal Helpers ─────────────────────────────────────────────────────────

    /**
     * Verifies Razorpay payment signature: HMAC-SHA256(orderId + "|" + paymentId, keySecret)
     */
    private boolean verifySignature(String orderId, String paymentId, String signature) {
        if (razorpayKeyId.equals("RAZORPAY_KEY_NOT_SET")
                || razorpayKeyId.contains("REPLACE_ME")
                || razorpayKeySecret.contains("REPLACE_ME")) {
            return true;
        }
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] computed = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            // Razorpay returns the signature as a lowercase hex string. Decode it
            // to bytes and compare in constant time to avoid a timing side-channel
            // that would let an attacker probe the signature one byte at a time.
            byte[] provided;
            try {
                provided = HexFormat.of().parseHex(signature == null ? "" : signature.toLowerCase());
            } catch (IllegalArgumentException badHex) {
                return false;
            }
            return MessageDigest.isEqual(computed, provided);
        } catch (Exception e) {
            logger.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Computes total booking amount from the show's seat prices.
     * Falls back to a stored total if seats can't be resolved.
     */
    private BigDecimal calculateBookingAmount(Booking booking) {
        if (booking.getShow() == null) {
            // Generic event booking — price not tracked at seat level
            return BigDecimal.valueOf(500); // default placeholder
        }
        // Sum prices of BOOKED seats linked to this booking
        return bookingRepository.findTotalAmountForBooking(booking.getId())
                .orElse(BigDecimal.ZERO);
    }
}
