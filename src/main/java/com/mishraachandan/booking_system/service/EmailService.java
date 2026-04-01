package com.mishraachandan.booking_system.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
}
