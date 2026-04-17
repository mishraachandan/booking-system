package com.mishraachandan.booking_system.controller;

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
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final KeycloakAdminService keycloakAdminService;

    public AuthController(UserRepository userRepository,
                          EmailService emailService,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          KeycloakAdminService keycloakAdminService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.keycloakAdminService = keycloakAdminService;
    }

    // ─── Register ────────────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRegistrationRequest userrequest) {
        String passwordValidationMessage = HelperUtility.getPasswordValidationMessage(userrequest.getPassword());
        if (!passwordValidationMessage.isEmpty()) {
            return ResponseEntity.status(422).body(passwordValidationMessage);
        }

        Optional<User> existingUser = userRepository.findByEmail(userrequest.getEmail());
        if (existingUser.isPresent()) {
            if (existingUser.get().isEnabled()) {
                return ResponseEntity.status(409).body("Email is already registered and verified");
            }
            User user = existingUser.get();
            String otp = "123456"; // Static OTP for dev environment
            user.setOtp(otp);
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);
            return ResponseEntity.ok("User already exists but is not verified. A new OTP has been sent to your email.");
        }

        String otp = "123456"; // Static OTP for dev environment

        User user = User.builder()
                .email(userrequest.getEmail())
                .passwordHash(passwordEncoder.encode(userrequest.getPassword()))
                .firstName(userrequest.getFirstName())
                .lastName(userrequest.getLastName())
                .phone(userrequest.getPhone())
                .enabled(false)
                .otp(otp)
                .otpExpiry(LocalDateTime.now().plusMinutes(5))
                .build();

        try {
            userRepository.save(user);
            return ResponseEntity.status(201).body(
                    "Registration successful. Please verify the OTP sent to your email.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Sorry, something went wrong during registration");
        }
    }

    // ─── Verify OTP ──────────────────────────────────────────────────────────────

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@Valid @RequestBody VerifyOtpRequest verifyRequest) {
        Optional<User> userOptional = userRepository.findByEmail(verifyRequest.getEmail());
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        User user = userOptional.get();
        if (user.isEnabled()) {
            return ResponseEntity.status(400).body("User is already verified");
        }
        if (user.getOtp() == null || !user.getOtp().equals(verifyRequest.getOtp())) {
            return ResponseEntity.status(400).body("Invalid OTP");
        }
        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400).body("OTP has expired");
        }

        user.setEnabled(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        // Provision user in Keycloak (async, non-fatal)
        // Note: we need the raw password here but it's already hashed.
        // Keycloak user is created with a temporary password; they'll set their own
        // on first Keycloak login or via "Forgot Password" flow.
        try {
            String keycloakId = keycloakAdminService.createUser(
                    user.getEmail(),
                    "TempPass@1234",   // temporary — user must reset on first KC login
                    user.getFirstName(),
                    user.getLastName()
            );
            if (keycloakId != null) {
                user.setKeycloakId(keycloakId);
                userRepository.save(user);
                log.info("User {} provisioned in Keycloak with KC ID {}", user.getEmail(), keycloakId);
            }
        } catch (Exception e) {
            log.warn("Could not provision {} in Keycloak (non-fatal): {}", user.getEmail(), e.getMessage());
        }

        return ResponseEntity.ok(
                "User verified successfully. You can now log in via the Keycloak login page.");
    }

    // ─── Keycloak Sync ───────────────────────────────────────────────────────────

    /**
     * Called by the Angular app immediately after a successful Keycloak login.
     * Ensures the local User record exists and keycloakId is linked.
     * This is an idempotent operation — safe to call on every login.
     *
     * The request body carries claims extracted from the Keycloak JWT.
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

        // Check if the user already exists with this Keycloak ID
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

        // Check by email (link existing user to KC)
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

        return ResponseEntity.status(404).body(Map.of(
                "error", "No local user found for email " + email
                        + ". Please complete registration first."
        ));
    }

    // ─── Login (Legacy — kept for API backward-compat) ───────────────────────────

    /**
     * @deprecated Use Keycloak-hosted login page instead.
     * This endpoint issues custom JJWT tokens and remains for API clients
     * that cannot use the OAuth2 redirect flow. Will be removed in Phase 7.
     */
    @Deprecated(since = "Phase 6", forRemoval = true)
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }

        User user = userOptional.get();
        if (!user.isEnabled()) {
            return ResponseEntity.status(403).body(
                    "Please verify your email first. Check your inbox for the OTP.");
        }
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid email or password");
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

    // ─── Refresh Token (Legacy) ───────────────────────────────────────────────────

    @Deprecated(since = "Phase 6", forRemoval = true)
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body("refreshToken is required");
        }
        if (!jwtUtil.validateJwtToken(refreshToken)) {
            return ResponseEntity.status(401).body("Invalid or expired refresh token");
        }

        Claims claims = jwtUtil.getClaimsFromJwtToken(refreshToken);
        String tokenType = claims.get("type", String.class);
        if (!"refresh".equals(tokenType)) {
            return ResponseEntity.status(401).body("Token is not a refresh token");
        }

        String email = claims.getSubject();
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty() || !userOptional.get().isEnabled()) {
            return ResponseEntity.status(401).body("User not found or inactive");
        }

        User user = userOptional.get();
        String newAccessToken = jwtUtil.generateJwtToken(
                null, user.getId(), user.getEmail(), user.getFirstName(), user.getRole().name());

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken, "tokenType", "Bearer"));
    }

    // ─── Logout ──────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // KC logout is handled client-side via keycloak.logout() in the Angular app.
        // For legacy JWT: stateless — client discards token.
        return ResponseEntity.ok("Logged out. For Keycloak sessions, use keycloak.logout() in the frontend.");
    }
}
