package com.mishraachandan.booking_system.controller;

import com.mishraachandan.booking_system.config.InMemoryRateLimiter;
import com.mishraachandan.booking_system.dto.entity.User;
import com.mishraachandan.booking_system.dto.pojo.LoginRequest;
import com.mishraachandan.booking_system.dto.pojo.UserRegistrationRequest;
import com.mishraachandan.booking_system.dto.pojo.VerifyOtpRequest;
import com.mishraachandan.booking_system.repository.UserRepository;
import com.mishraachandan.booking_system.service.EmailService;
import com.mishraachandan.util.HelperUtility;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /** Cryptographically strong RNG for OTP generation. */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** OTP is a 6-digit numeric code. */
    private static final int OTP_LENGTH = 6;

    /** OTP validity window. */
    private static final Duration OTP_TTL = Duration.ofMinutes(5);

    /** Rate-limit policy: max 5 attempts per 15 minutes per key. */
    private static final int AUTH_MAX_ATTEMPTS = 5;
    private static final Duration AUTH_WINDOW = Duration.ofMinutes(15);

    /** Generic, non-enumerating response for bad credentials / bad OTP. */
    private static final String GENERIC_AUTH_ERROR = "Invalid credentials";
    private static final String GENERIC_OTP_ERROR = "Invalid or expired OTP";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InMemoryRateLimiter rateLimiter;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRegistrationRequest userrequest) {
        String passwordValidationMessage = HelperUtility.getPasswordValidationMessage(userrequest.getPassword());
        if (!passwordValidationMessage.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(passwordValidationMessage);
        }

        String otp = generateOtp();
        String otpHash = passwordEncoder.encode(otp);

        Optional<User> existingUser = userRepository.findByEmail(userrequest.getEmail());
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.isEnabled()) {
                // Do NOT leak that this email is already verified. Respond generically so
                // attackers cannot enumerate registered accounts via /register.
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body("If your details are valid, we have sent a verification code to your email.");
            }
            // Unverified account — refresh the OTP as well as any profile fields
            // the caller supplied this time. Without re-saving the password hash
            // a user who re-registers with a different password would never be
            // able to log in after verification (the first hash would persist).
            user.setPasswordHash(passwordEncoder.encode(userrequest.getPassword()));
            user.setFirstName(userrequest.getFirstName());
            user.setLastName(userrequest.getLastName());
            user.setPhone(userrequest.getPhone());
            user.setOtp(otpHash);
            user.setOtpExpiry(LocalDateTime.now().plus(OTP_TTL));
            userRepository.save(user);
            emailService.sendOtpEmail(user.getEmail(), otp);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("If your details are valid, we have sent a verification code to your email.");
        }

        User user = User.builder()
                .email(userrequest.getEmail())
                .passwordHash(passwordEncoder.encode(userrequest.getPassword()))
                .firstName(userrequest.getFirstName())
                .lastName(userrequest.getLastName())
                .phone(userrequest.getPhone())
                .enabled(false)
                .otp(otpHash)
                .otpExpiry(LocalDateTime.now().plus(OTP_TTL))
                .build();

        try {
            userRepository.save(user);
            emailService.sendOtpEmail(user.getEmail(), otp);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("If your details are valid, we have sent a verification code to your email.");
        } catch (Exception e) {
            log.error("Registration failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Sorry, something went wrong during registration");
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@Valid @RequestBody VerifyOtpRequest verifyRequest,
                                            HttpServletRequest servletRequest) {
        if (!rateLimiter.tryAcquire("verify-otp:" + clientKey(servletRequest, verifyRequest.getEmail()),
                AUTH_MAX_ATTEMPTS, AUTH_WINDOW)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many verification attempts. Please try again later.");
        }

        Optional<User> userOptional = userRepository.findByEmail(verifyRequest.getEmail());
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GENERIC_OTP_ERROR);
        }

        User user = userOptional.get();
        if (user.isEnabled()) {
            // Already verified — tell the caller generically.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GENERIC_OTP_ERROR);
        }

        if (user.getOtp() == null
                || user.getOtpExpiry() == null
                || user.getOtpExpiry().isBefore(LocalDateTime.now())
                || !constantTimeMatches(verifyRequest.getOtp(), user.getOtp())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GENERIC_OTP_ERROR);
        }

        user.setEnabled(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok("User verified successfully. You can now login.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletRequest servletRequest) {
        if (!rateLimiter.tryAcquire("login:" + clientKey(servletRequest, loginRequest.getEmail()),
                AUTH_MAX_ATTEMPTS, AUTH_WINDOW)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many login attempts. Please try again later.");
        }

        Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());

        // Always run a bcrypt comparison to keep response timing uniform between
        // "user does not exist" and "wrong password" — prevents user enumeration.
        final String dummyHash = "$2a$10$7EqJtq98hPqEX7fNZaFWoO7k5p7YdV6/oX8Y8gkZQWgK4/8X/F9Qi";
        String hashToCheck = userOptional.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordOk = passwordEncoder.matches(loginRequest.getPassword(), hashToCheck);

        if (userOptional.isEmpty() || !passwordOk) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(GENERIC_AUTH_ERROR);
        }

        User user = userOptional.get();
        if (!user.isEnabled()) {
            // Keep this distinct only because the UI needs to prompt the user to
            // verify; use a dedicated status to avoid looking like bad creds.
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Account not verified. Please complete OTP verification.");
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("message", "Login successful. Welcome " + user.getFirstName() + "!");
        return ResponseEntity.ok(response);
    }

    /** Generate a zero-padded numeric OTP using {@link SecureRandom}. */
    private static String generateOtp() {
        int max = (int) Math.pow(10, OTP_LENGTH);
        int n = SECURE_RANDOM.nextInt(max);
        return String.format("%0" + OTP_LENGTH + "d", n);
    }

    /**
     * Constant-time comparison between the user-supplied OTP and the stored
     * bcrypt hash. Uses {@link PasswordEncoder#matches} which performs a
     * constant-time compare internally.
     */
    private boolean constantTimeMatches(String rawOtp, String storedHash) {
        if (rawOtp == null || storedHash == null) {
            // Still do a no-op compare to keep timing uniform.
            MessageDigest.isEqual("0".getBytes(StandardCharsets.UTF_8),
                    "1".getBytes(StandardCharsets.UTF_8));
            return false;
        }
        return passwordEncoder.matches(rawOtp, storedHash);
    }

    /** Build a rate-limit key combining the caller's remote address with the supplied identifier. */
    private static String clientKey(HttpServletRequest request, String identifier) {
        String ip = request != null ? request.getRemoteAddr() : "unknown";
        return (ip == null ? "unknown" : ip) + "|" + (identifier == null ? "" : identifier.toLowerCase());
    }
}
