package com.mishraachandan.booking_system.config;

import com.mishraachandan.booking_system.dto.entity.User;
import com.mishraachandan.booking_system.dto.status.UserRole;
import com.mishraachandan.booking_system.repository.UserRepository;
import com.mishraachandan.booking_system.service.KeycloakAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * One-time migration runner: imports all enabled local DB users into Keycloak.
 *
 * Runs on every startup but is a no-op for users who already have a keycloakId.
 * When Keycloak is offline (dev mode without Docker), the migration is silently skipped.
 *
 * Migration strategy (Option A):
 *   - All enabled users are created in Keycloak with a temporary password.
 *   - Their keycloakId is saved back to the DB.
 *   - On first Keycloak login, their password is synced.
 *
 * In PRODUCTION: replace the temporary password with a "Reset Password" email trigger.
 */
@Component
public class UserMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserMigrationRunner.class);

    @Value("${keycloak.migration.enabled:true}")
    private boolean migrationEnabled;

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;

    public UserMigrationRunner(UserRepository userRepository,
                               KeycloakAdminService keycloakAdminService) {
        this.userRepository = userRepository;
        this.keycloakAdminService = keycloakAdminService;
    }

    @Override
    public void run(String... args) {
        if (!migrationEnabled) {
            log.info("Keycloak user migration is disabled (keycloak.migration.enabled=false)");
            return;
        }

        List<User> unmigrated = userRepository.findAll().stream()
                .filter(u -> u.isEnabled() && u.getKeycloakId() == null)
                .toList();

        if (unmigrated.isEmpty()) {
            log.info("Keycloak migration: all users are already synced.");
            return;
        }

        log.info("Keycloak migration: migrating {} users to Keycloak...", unmigrated.size());
        int success = 0;
        int skipped = 0;

        for (User user : unmigrated) {
            try {
                boolean isAdmin = user.getRole() == UserRole.ADMIN;
                String keycloakId = keycloakAdminService.migrateUser(
                        user.getEmail(),
                        user.getFirstName() != null ? user.getFirstName() : "",
                        user.getLastName() != null ? user.getLastName() : "",
                        isAdmin
                );

                if (keycloakId != null) {
                    user.setKeycloakId(keycloakId);
                    userRepository.save(user);
                    log.info("Migrated user {} → Keycloak ID {}", user.getEmail(), keycloakId);
                    success++;
                } else {
                    skipped++;
                    log.warn("Could not migrate user {} (Keycloak may be offline)", user.getEmail());
                }
            } catch (Exception e) {
                skipped++;
                log.error("Error migrating user {}: {}", user.getEmail(), e.getMessage());
            }
        }

        log.info("Keycloak migration complete: {} migrated, {} skipped/failed", success, skipped);
    }
}
