import { KeycloakConfig } from 'keycloak-js';

/**
 * Keycloak connection configuration for the Angular frontend.
 *
 * Realm   : booking-system (created in docker-compose + realm-export.json)
 * Client  : booking-frontend (public client with PKCE)
 * Port    : 8180 (Keycloak dev mode — avoids conflict with Spring Boot on 8080)
 */
export const keycloakConfig: KeycloakConfig = {
  url: 'http://localhost:8180',
  realm: 'booking-system',
  clientId: 'booking-frontend',
};
