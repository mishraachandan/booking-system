package com.mishraachandan.booking_system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Uses Keycloak Admin REST API to manage users in the booking-system realm.
 *
 * Keycloak Admin API is called after OTP verification to create/update the user
 * in Keycloak, so they can log in via the Keycloak SSO flow.
 *
 * Base URL: http://localhost:8180/admin/realms/booking-system
 */
@Service
public class KeycloakAdminService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminService.class);

    @Value("${keycloak.admin.server-url:http://localhost:8180}")
    private String keycloakServerUrl;

    @Value("${keycloak.admin.realm:booking-system}")
    private String realm;

    @Value("${keycloak.admin.client-id:admin-cli}")
    private String adminClientId;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:admin}")
    private String adminPassword;

    private final RestTemplate restTemplate;

    public KeycloakAdminService() {
        this.restTemplate = new RestTemplate();
    }

    // ─── Token ────────────────────────────────────────────────────────────────────

    /**
     * Gets a short-lived admin access token from the master realm.
     */
    private String getAdminToken() {
        String tokenUrl = keycloakServerUrl + "/realms/master/protocol/openid-connect/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id=" + adminClientId
                + "&username=" + adminUsername
                + "&password=" + adminPassword
                + "&grant_type=password";

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        }
        throw new RuntimeException("Could not obtain Keycloak admin token");
    }

    // ─── Create User ──────────────────────────────────────────────────────────────

    /**
     * Creates a user in the booking-system Keycloak realm.
     * Called after OTP verification is successful.
     *
     * @return Keycloak user UUID (from Location header), or null if Keycloak is unavailable
     */
    public String createUser(String email, String password, String firstName, String lastName) {
        try {
            String adminToken = getAdminToken();
            String usersUrl = keycloakServerUrl + "/admin/realms/" + realm + "/users";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);

            Map<String, Object> userPayload = Map.of(
                    "username", email,
                    "email", email,
                    "firstName", firstName,
                    "lastName", lastName,
                    "enabled", true,
                    "emailVerified", true,
                    "credentials", List.of(Map.of(
                            "type", "password",
                            "value", password,
                            "temporary", false
                    ))
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(userPayload, headers);
            ResponseEntity<Void> response = restTemplate.postForEntity(usersUrl, request, Void.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                // Extract the new user's UUID from the Location header
                String location = response.getHeaders().getFirst("Location");
                if (location != null) {
                    String keycloakId = location.substring(location.lastIndexOf('/') + 1);
                    // Assign USER role
                    assignRealmRole(keycloakId, "USER", adminToken);
                    log.info("Created Keycloak user {} with KC ID {}", email, keycloakId);
                    return keycloakId;
                }
            }
        } catch (HttpClientErrorException.Conflict e) {
            // User already exists in Keycloak — find and return their ID
            log.info("User {} already exists in Keycloak, fetching ID", email);
            return findUserIdByEmail(email);
        } catch (Exception e) {
            log.warn("Could not create user {} in Keycloak (Keycloak may be offline): {}",
                    email, e.getMessage());
            // Non-fatal — user can still log in via legacy JWT; KC sync happens on first KC login
        }
        return null;
    }

    // ─── Find User ────────────────────────────────────────────────────────────────

    public String findUserIdByEmail(String email) {
        try {
            String adminToken = getAdminToken();
            String url = keycloakServerUrl + "/admin/realms/" + realm + "/users?email=" + email + "&exact=true";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, request, List.class);

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && !response.getBody().isEmpty()) {
                Map<String, Object> user = (Map<String, Object>) response.getBody().get(0);
                return (String) user.get("id");
            }
        } catch (Exception e) {
            log.warn("Could not find Keycloak user by email {}: {}", email, e.getMessage());
        }
        return null;
    }

    // ─── Assign Role ──────────────────────────────────────────────────────────────

    private void assignRealmRole(String keycloakUserId, String roleName, String adminToken) {
        try {
            // Get role representation
            String roleUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles/" + roleName;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<Void> roleRequest = new HttpEntity<>(headers);
            ResponseEntity<Map> roleResponse = restTemplate.exchange(roleUrl, HttpMethod.GET, roleRequest, Map.class);

            if (!roleResponse.getStatusCode().is2xxSuccessful() || roleResponse.getBody() == null) {
                log.warn("Role {} not found in Keycloak realm", roleName);
                return;
            }

            // Assign role to user
            String assignUrl = keycloakServerUrl + "/admin/realms/" + realm
                    + "/users/" + keycloakUserId + "/role-mappings/realm";
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<Map>> assignRequest = new HttpEntity<>(List.of(roleResponse.getBody()), headers);
            restTemplate.postForEntity(assignUrl, assignRequest, Void.class);

            log.info("Assigned role {} to Keycloak user {}", roleName, keycloakUserId);
        } catch (Exception e) {
            log.warn("Could not assign role {} to user {}: {}", roleName, keycloakUserId, e.getMessage());
        }
    }

    // ─── Bulk Migration ───────────────────────────────────────────────────────────

    /**
     * Imports a user from the local DB into Keycloak with a temporary password.
     * Used by the one-time migration runner.
     * Migrated users receive a "Reset Password" email in production; in dev the temp password is used.
     *
     * @return Keycloak UUID or null on failure
     */
    public String migrateUser(String email, String firstName, String lastName, boolean isAdmin) {
        String keycloakId = createUser(email, "TempPass@1234", firstName, lastName);
        if (keycloakId != null && isAdmin) {
            try {
                String adminToken = getAdminToken();
                assignRealmRole(keycloakId, "ADMIN", adminToken);
            } catch (Exception e) {
                log.warn("Could not assign ADMIN role to migrated user {}", email);
            }
        }
        return keycloakId;
    }
}
