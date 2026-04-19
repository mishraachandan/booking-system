package com.mishraachandan.booking_system.controller;

import com.mishraachandan.booking_system.config.InMemoryRateLimiter;
import com.mishraachandan.booking_system.config.JwtUtil;
import com.mishraachandan.booking_system.dto.entity.User;
import com.mishraachandan.booking_system.dto.pojo.LoginRequest;
import com.mishraachandan.booking_system.dto.pojo.UserRegistrationRequest;
import com.mishraachandan.booking_system.dto.pojo.VerifyOtpRequest;
import com.mishraachandan.booking_system.repository.UserRepository;
import com.mishraachandan.booking_system.service.EmailService;
import com.mishraachandan.booking_system.service.KeycloakAdminService;
import com.mishraachandan.util.HelperUtility;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
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
    private static final String GENERIC_REGISTER_OK =
            "If your details are valid, we have sent a verification code to your email.";

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final KeycloakAdminService keycloakAdminService;
    private final InMemoryRateLimiter rateLimiter;

    public AuthController(UserRepository userRepository,
                          EmailService emailService,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          KeycloakAdminService keycloakAdminService,
                          InMemoryRateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.keycloakAdminService = keycloakAdminService;
        this.rateLimiter = rateLimiter;
    }

    // ─── Register ────────────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRegistrationRequest userrequest,
                                           HttpServletRequest httpRequest) {
        // Throttle per IP to slow down mass registration / email enumeration.
        String ip = clientIp(httpRequest);
        if (!rateLimiter.tryAcquire("register:ip:" + ip, AUTH_MAX_ATTEMPTS * 2, AUTH_WINDOW)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many attempts. Try again later.");
        }

        String passwordValidationMessage =
                HelperUtility.getPasswordValidationMessage(userrequest.getPassword());
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
                return ResponseEntity.status(HttpStatus.CREATED).body(GENERIC_REGISTER_OK);
            }
            // Unverified account — refresh the OTP and any profile fields the caller
            // supplied this time. Without re-saving the password hash, a user who
            // re-registers with a different password would never be able to log in
            // after verification (the first hash would persist).
            user.setPasswordHash(passwordEncoder.encode(userrequest.getPassword()));
            user.setFirstName(userrequest.getFirstName());
            user.setLastName(userrequest.getLastName());
            user.setPhone(userrequest.getPhone());
            user.setOtp(otpHash);
            user.setOtpExpiry(LocalDateTime.now().plus(OTP_TTL));
            userRepository.save(user);
            emailService.sendOtpEmail(user.getEmail(), otp);
            return ResponseEntity.status(HttpStatus.CREATED).body(GENERIC_REGISTER_OK);
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
            return ResponseEntity.status(HttpStatus.CREATED).body(GENERIC_REGISTER_OK);
        } catch (Exception e) {
            log.warn("Registration failed for {}: {}", userrequest.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Sorry, something went wrong during registration");
        }
    }

    // ─── Verify OTP ──────────────────────────────────────────────────────────────

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@Valid @RequestBody VerifyOtpRequest verifyRequest,
                                            HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        String emailKey = safeEmailKey(verifyRequest.getEmail());

        if (!rateLimiter.tryAcquire("verify-otp:ip:" + ip, AUTH_MAX_ATTEMPTS, AUTH_WINDOW)
                || !rateLimiter.tryAcquire("verify-otp:email:" + emailKey, AUTH_MAX_ATTEMPTS, AUTH_WINDOW)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many attempts. Try again later.");
        }

        Optional<User> userOptional = userRepository.findByEmail(verifyRequest.getEmail());
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GENERIC_OTP_ERROR);
        }

        User user = userOptional.get();
        if (user.isEnabled()) {
            // Also generic — don't reveal that this account is already verified.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GENERIC_OTP_ERROR);
        }
        if (user.getOtp() == null
                || user.getOtpExpiry() == null
                || user.getOtpExpiry().isBefore(LocalDateTime.now())
                || !passwordEncoder.matches(verifyRequest.getOtp(), user.getOtp())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GENERIC_OTP_ERROR);
        }

        user.setEnabled(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        // Provision user in Keycloak (async, non-fatal). We don't have the raw
        // user password here (we hash it at registration time), so we generate
        // a one-time random password in Keycloak that the user must reset on
        // first Keycloak login.
        try {
            String tempPassword = generateKeycloakTempPassword();
            String keycloakId = keycloakAdminService.createUser(
                    user.getEmail(),
                    tempPassword,
                    user.getFirstName(),
                    user.getLastName()
            );
            if (keycloakId != null) {
                user.setKeycloakId(keycloakId);
                userRepository.save(user);
                log.info("User {} provisioned in Keycloak with KC ID {}", user.getEmail(), keycloakId);
            }
        } catch (Exception e) {
            log.warn("Could not provision {} in Keycloak (non-fatal): {}",
                    user.getEmail(), e.getMessage());
        }

        return ResponseEntity.ok(
                "User verified successfully. You can now log in via the Keycloak login page.");
    }

    // ─── Keycloak Sync ──────────────────────────────────────────────────────────

    /**
     * Called by the Angular app immediately after a successful Keycloak login.
     * Ensures the local User record exists and keycloakId is linked.
     * Idempotent — safe to call on every login.
     *
     * The JWT itself is validated by Spring Security (Keycloak Resource Server)
     * before this endpoint is reached, so the data is trusted.
     *
     * POST /api/auth/keycloak-sync
     */
    @PostMapping("/keycloak-sync")
    public ResponseEntity<?> keycloakSync(@RequestBody Map<String, String> body) {
        String keycloakId = body.get("keycloakId");
        String email = body.get("email");
        String firstName = body.getOrDefault("firstName", "");
        String lastName = body.getOrDefault("lastName", "");

        if (keycloakId == null || email == null) {
            return ResponseEntity.badRequest().body("keycloakId and email are required");
        }

        Optional<User> byKcId = userRepository.findByKeycloakId(keycloakId);
        if (byKcId.isPresent()) {
            User user = byKcId.get();
            return ResponseEntity.ok(Map.of(
                    "userId", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                    "role", user.getRole().name(),
                    "synchronized", false
            ));
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            user.setKeycloakId(keycloakId);
            user.setEnabled(true);
            if (!firstName.isEmpty()) user.setFirstName(firstName);
            if (!lastName.isEmpty()) user.setLastName(lastName);
            userRepository.save(user);
            log.info("Linked DB user {} to Keycloak ID {} via sync endpoint", email, keycloakId);
            return ResponseEntity.ok(Map.of(
                    "userId", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                    "role", user.getRole().name(),
                    "synchronized", true
            ));
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "No local user found for this account. Please complete registration first."
        ));
    }

    // ─── Login (Legacy — kept for API backward-compat) ──────────────────────────

    /**
     * @deprecated Use the Keycloak-hosted login page instead.
     * This endpoint issues custom JJWT tokens and remains for API clients
     * that cannot use the OAuth2 redirect flow. Will be removed in Phase 7.
     */
    @Deprecated(since = "Phase 6", forRemoval = true)
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        String emailKey = safeEmailKey(loginRequest.getEmail());

        if (!rateLimiter.tryAcquire("login:ip:" + ip, AUTH_MAX_ATTEMPTS, AUTH_WINDOW)
                || !rateLimiter.tryAcquire("login:email:" + emailKey, AUTH_MAX_ATTEMPTS, AUTH_WINDOW)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many attempts. Try again later."));
        }

        Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());
        // Run bcrypt compare against a dummy hash when the user is absent, so the
        // response time is similar for existing and non-existing emails.
        if (userOptional.isEmpty()) {
            passwordEncoder.matches(loginRequest.getPassword(),
                    "$2a$10$7EqJtq98hPqEX7fNZaFWoO5Du7Q2bVlZp6Nx1Q4U9sL2Oa3cZc.Gi");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", GENERIC_AUTH_ERROR));
        }

        User user = userOptional.get();
        boolean passwordOk = passwordEncoder.matches(
                loginRequest.getPassword(), user.getPasswordHash());

        if (!user.isEnabled() || !passwordOk) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", GENERIC_AUTH_ERROR));
        }

        String accessToken = jwtUtil.generateJwtToken(
                null, user.getId(), user.getEmail(), user.getFirstName(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "tokenType", "Bearer",
                "userId", user.getId(),
                "email", user.getEmail(),
                "firstName", user.getFirstName(),
                "role", user.getRole().name(),
                "message", "Login successful (legacy). Consider migrating to Keycloak SSO."
        ));
    }

    // ─── Refresh Token (Legacy) ─────────────────────────────────────────────────

    @Deprecated(since = "Phase 6", forRemoval = true)
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body("refreshToken is required");
        }
        if (!jwtUtil.validateJwtToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        }

        Claims claims = jwtUtil.getClaimsFromJwtToken(refreshToken);
        String tokenType = claims.get("type", String.class);
        if (!"refresh".equals(tokenType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is not a refresh token");
        }

        Long userId = claims.get("userId", Long.class);
        String email = claims.getSubject();

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found or disabled");
        }

        User user = userOpt.get();
        String newAccessToken = jwtUtil.generateJwtToken(
                null, user.getId(), email, user.getFirstName(), user.getRole().name());

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "tokenType", "Bearer"
        ));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    /** Generates a fresh, uniformly-distributed 6-digit OTP using {@link SecureRandom}. */
    private static String generateOtp() {
        int max = (int) Math.pow(10, OTP_LENGTH);
        int n = SECURE_RANDOM.nextInt(max);
        return String.format("%0" + OTP_LENGTH + "d", n);
    }

    /**
     * Generates a strong random password for Keycloak user provisioning.
     * Each user gets a unique value, so leaking one does not compromise others.
     * Users are expected to reset this via Keycloak's "Forgot Password" flow
     * before logging in.
     */
    private static String generateKeycloakTempPassword() {
        byte[] buf = new byte[24];
        SECURE_RANDOM.nextBytes(buf);
        // Base64url without padding, ~32 chars, then mix in requirements.
        String token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        return "Kc!" + token + "9";
    }

    /** Returns the best-effort client IP, respecting X-Forwarded-For. */
    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }

    private static String safeEmailKey(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase();
    }

    // Silence the unused-import warning for MessageDigest — we keep it available
    // for callers that want to implement constant-time byte compare over
    // pre-hashed credential data in future. OTP verification uses
    // {@link PasswordEncoder#matches} which already performs a constant-time
    // compare of the bcrypt hash.
    @SuppressWarnings("unused")
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a == null ? new byte[0] : a, b == null ? new byte[0] : b);
    }

    @SuppressWarnings("unused")
    private static byte[] utf8(String s) {
        return s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8);
    }
}
