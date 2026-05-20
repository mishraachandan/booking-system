package com.mishraachandan.booking_system.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends OTP email asynchronously so it never blocks the HTTP request thread.
     * If SMTP is not configured, the exception is caught and logged — registration still succeeds.
     */
    @Async
    public void sendOtpEmail(String to, String otp) {
        log.info("Sending OTP to email: {}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@bookmyshow.local");
            message.setTo(to);
            message.setSubject("Your OTP for BookMyShow Registration");
            message.setText(
                "Hello!\n\n" +
                "Your OTP for account verification is: " + otp + "\n\n" +
                "This OTP is valid for 5 minutes.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "— BookMyShow Team"
            );
            mailSender.send(message);
            log.info("OTP email sent successfully to {}", to);
        } catch (Exception e) {
            // Log OTP to console so developer can verify accounts without real email during dev
            log.warn("Email send failed for {} — OTP is: {} (check logs for manual verification)", to, otp);
        }
    }

    /**
     * Sends booking confirmation email asynchronously.
     */
    @Async
    public void sendBookingConfirmationEmail(String to, Long bookingId, String movieTitle, String resourceName,
                                             List<String> seats, java.math.BigDecimal amount) {
        log.info("Sending booking confirmation email to: {}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@bookmyshow.local");
            message.setTo(to);
            message.setSubject("Booking Confirmed! - Booking #" + bookingId);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Hello!\n\n")
              .append("Your booking #").append(bookingId).append(" has been successfully confirmed!\n\n");
            
            if (movieTitle != null) {
                sb.append("Movie: ").append(movieTitle).append("\n")
                  .append("Seats: ").append(String.join(", ", seats)).append("\n");
            } else if (resourceName != null) {
                sb.append("Event/Resource: ").append(resourceName).append("\n");
            }
            
            sb.append("Total Amount Paid: ₹").append(amount).append("\n\n")
              .append("Scan this QR Code at entry to check-in:\n")
              .append("https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=BOOKING-").append(bookingId).append("\n\n")
              .append("Thank you for booking with us!\n\n")
              .append("— BookMyShow Team");
              
            message.setText(sb.toString());
            mailSender.send(message);
            log.info("Booking confirmation email sent successfully to {}", to);
        } catch (Exception e) {
            log.warn("Booking confirmation email send failed for {} — Booking ID: {}", to, bookingId, e);
        }
    }

    /**
     * Sends booking expired/cancelled email asynchronously.
     */
    @Async
    public void sendBookingExpiredEmail(String to, Long bookingId) {
        log.info("Sending booking expired notification email to: {}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@bookmyshow.local");
            message.setTo(to);
            message.setSubject("Booking Expired - Booking #" + bookingId);
            message.setText(
                "Hello!\n\n" +
                "Your booking #" + bookingId + " has expired because payment was not completed within the 10-minute window.\n\n" +
                "Any locked seats have been released back to general availability. Please try booking again if you wish to complete your purchase.\n\n" +
                "— BookMyShow Team"
            );
            mailSender.send(message);
            log.info("Booking expired email sent successfully to {}", to);
        } catch (Exception e) {
            log.warn("Booking expired email send failed for {} — Booking ID: {}", to, bookingId, e);
        }
    }
}
