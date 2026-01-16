package com.mishraachandan.booking_system.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import com.mishraachandan.booking_system.dto.entity.User;
import com.mishraachandan.booking_system.dto.pojo.UserRegistrationRequest;
import com.mishraachandan.booking_system.repository.UserRepository;
import com.mishraachandan.util.HelperUtility;
import com.mishraachandan.booking_system.service.EmailService;
import com.mishraachandan.booking_system.dto.pojo.VerifyOtpRequest;
import com.mishraachandan.booking_system.dto.pojo.LoginRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    // We add @Validated at the class level (on the controller) to enable validation
    // when using @Valid or @Validated on method parameters.
    // For a POST endpoint that receives a request body meant to be validated, we
    // annotate the parameter with @Valid (from jakarta.validation.Valid or
    // org.springframework.validation.annotation.Validated).
    // This ensures that the validation annotations in UserRegistrationRequest are
    // checked automatically.
    public ResponseEntity<String> register(@Valid @RequestBody UserRegistrationRequest userrequest) {
        // Check if the password is valid
        String passwordValidationMessage = HelperUtility.getPasswordValidationMessage(userrequest.getPassword());
        if (!passwordValidationMessage.isEmpty()) {
            return ResponseEntity
                    .status(422) // Unprocessable Entity
                    .body(passwordValidationMessage);
        }
        // Check if user already exists
        Optional<User> existingUser = userRepository.findByEmail(userrequest.getEmail());
        if (existingUser.isPresent()) {
            if (existingUser.get().isEnabled()) {
                return ResponseEntity
                        .status(409) // Conflict
                        .body("Email is already registered and verified");
            }
            // If user exists but is not verified, we can re-send OTP
            User user = existingUser.get();
            String otp = String.format("%06d", new Random().nextInt(999999));
            user.setOtp(otp);
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);
            emailService.sendOtpEmail(user.getEmail(), otp);
            return ResponseEntity
                    .status(200)
                    .body("User already exists but is not verified. A new OTP has been sent to your email.");
        }

        String otp = String.format("%06d", new Random().nextInt(999999));

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
            if (user != null) {
                userRepository.save(user);
            }
            emailService.sendOtpEmail(user.getEmail(), otp);
            return ResponseEntity
                    .status(201) // Created
                    .body("Registration successful. Please verify the OTP sent to your email.");
        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body("Sorry, something went wrong during registration");
        }
    }

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

        return ResponseEntity.ok("User verified successfully. You can now login.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest loginRequest) {
        Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }

        User user = userOptional.get();
        if (!user.isEnabled()) {
            return ResponseEntity.status(403).body("Please verify your account first");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }

        return ResponseEntity.ok("Login successful. Welcome " + user.getFirstName() + "!");
    }
}
