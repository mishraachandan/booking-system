package com.mishraachandan.booking_system.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import com.mishraachandan.booking_system.dto.entity.User;
import com.mishraachandan.booking_system.dto.pojo.UserRegistrationRequest;
import com.mishraachandan.booking_system.repository.UserRepository;
import com.mishraachandan.util.HelperUtility;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/register")
    // We add @Validated at the class level (on the controller) to enable validation when using @Valid or @Validated on method parameters.
    // For a POST endpoint that receives a request body meant to be validated, we annotate the parameter with @Valid (from jakarta.validation.Valid or org.springframework.validation.annotation.Validated).
    // This ensures that the validation annotations in UserRegistrationRequest are checked automatically.
    public ResponseEntity<String> register(@Valid @RequestBody UserRegistrationRequest userrequest) {
        // Check if the password is valid
        String passwordValidationMessage = HelperUtility.getPasswordValidationMessage(userrequest.getPassword());
        if (!passwordValidationMessage.isEmpty()) {
            return ResponseEntity
                .status(422) // Unprocessable Entity
                .body(passwordValidationMessage);
        }
        // Check if user already exists
        if (userRepository.findByEmail(userrequest.getEmail()).isPresent()) {
            return ResponseEntity
                .status(409) // Conflict
                .body("Email is already registered");
        }

        User user = User.builder()
            .email(userrequest.getEmail())
            .passwordHash(userrequest.getPassword())
            .firstName(userrequest.getFirstName())
            .lastName(userrequest.getLastName())
            .phone(userrequest.getPhone())
            .build();
        
        try {
            if(user != null){
                userRepository.save(user);
                return ResponseEntity
                .status(201) // Created
                .body("User " + user.getFirstName() + " registered successfully");
            }
        } catch (Exception e) {
            return ResponseEntity
            .status(500)
            .body("Sorry, something went wrong");
        }
        return ResponseEntity
        .status(400) // Bad Request
        .body("User Registration failed");
    }
}
