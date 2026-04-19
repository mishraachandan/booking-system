package com.mishraachandan.booking_system.config;

import com.mishraachandan.booking_system.dto.entity.User;
import com.mishraachandan.booking_system.dto.status.UserRole;
import com.mishraachandan.booking_system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converts a validated Keycloak JWT into a Spring Authentication object.
 *
 * Keycloak JWT structure (relevant claims):
 *   - sub       — Keycloak user UUID
 *   - email     — user email
 *   - realm_access.roles — list of realm roles assigned to the user
 *
 * On first login, syncs Keycloak user to local DB (creates or updates User record).
 */
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger log = LoggerFactory.getLogger(KeycloakJwtConverter.class);

    private final UserRepository userRepository;

    public KeycloakJwtConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String keycloakId = jwt.getSubject();                         // KC UUID
        String email = jwt.getClaimAsString("email");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");

        // Extract realm roles from realm_access.roles
        List<SimpleGrantedAuthority> authorities = extractRoles(jwt);

        // Determine the highest role for local UserRole enum
        UserRole localRole = authorities.stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .filter(r -> r.equals("ADMIN") || r.equals("USER"))
                .findFirst()
                .map(r -> "ADMIN".equals(r) ? UserRole.ADMIN : UserRole.USER)
                .orElse(UserRole.USER);

        // Sync to local DB — find by keycloakId, then fallback to email
        Long localUserId = syncAndGetLocalUserId(keycloakId, email, firstName, lastName, localRole);

        AuthenticatedUser principal = new AuthenticatedUser(localUserId, keycloakId, email, authorities);
        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }

    // ─── Role Extraction ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<SimpleGrantedAuthority> extractRoles(Jwt jwt) {
        List<SimpleGrantedAuthority> roles = new ArrayList<>();

        // Primary: realm_access.roles (standard Keycloak claim)
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            realmRoles.stream()
                    .filter(r -> r.equals("USER") || r.equals("ADMIN"))
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .forEach(roles::add);
        }

        // Default to USER if no applicable role found
        if (roles.isEmpty()) {
            roles.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return roles;
    }

    // ─── User Sync ────────────────────────────────────────────────────────────

    /**
     * Ensures the Keycloak user has a corresponding local User record.
     * Look up order: keycloakId → email → create new.
     * Returns the local user's DB primary key (Long id).
     */
    private Long syncAndGetLocalUserId(String keycloakId, String email,
                                        String firstName, String lastName, UserRole role) {
        try {
            // 1. Find by keycloak_id (already synced user)
            Optional<User> byKcId = userRepository.findByKeycloakId(keycloakId);
            if (byKcId.isPresent()) {
                return byKcId.get().getId();
            }

            // 2. Find by email (DB user not yet linked to Keycloak)
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                User user = byEmail.get();
                user.setKeycloakId(keycloakId);
                user.setEnabled(true);
                if (firstName != null) user.setFirstName(firstName);
                if (lastName != null) user.setLastName(lastName);
                userRepository.save(user);
                log.info("Linked existing user {} to Keycloak ID {}", email, keycloakId);
                return user.getId();
            }

            // 3. New user — first login via Keycloak with no DB record
            User newUser = User.builder()
                    .email(email)
                    .passwordHash("")          // Keycloak owns the password
                    .keycloakId(keycloakId)
                    .firstName(firstName != null ? firstName : "")
                    .lastName(lastName != null ? lastName : "")
                    .role(role)
                    .enabled(true)
                    .build();
            User saved = userRepository.save(newUser);
            log.info("Created new local user for Keycloak user {} (KC ID: {})", email, keycloakId);
            return saved.getId();

        } catch (Exception e) {
            log.error("Error syncing user {} from Keycloak: {}", email, e.getMessage());
            // Return -1 sentinel so the request still gets through (auth succeeds) but
            // controllers that need userId will need to handle this gracefully.
            return -1L;
        }
    }
}
