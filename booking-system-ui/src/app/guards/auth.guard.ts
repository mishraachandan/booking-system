import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import Keycloak from 'keycloak-js';

/**
 * authGuard — protects routes that require any authenticated user.
 *
 * Supports dual auth:
 *  1. Keycloak SSO (keycloak.authenticated)
 *  2. Legacy JWT login (accessToken in localStorage)
 *
 * If neither is present, redirects to the login page.
 */
export const authGuard: CanActivateFn = async (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const keycloak = inject(Keycloak);

  // Allow if authenticated via either method
  if (authService.isLoggedIn()) {
    return true;
  }

  // Try Keycloak login first; if Keycloak is available, use SSO
  try {
    if (keycloak.clientId) {
      await keycloak.login({ redirectUri: window.location.origin + state.url });
      return false;
    }
  } catch {
    // Keycloak unavailable — fall through to legacy login redirect
  }

  // Fallback: redirect to legacy login page
  return router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url }
  });
};

/**
 * adminGuard — protects admin routes. Requires ADMIN role.
 */
export const adminGuard: CanActivateFn = async (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const keycloak = inject(Keycloak);

  // Check if logged in via either method
  if (!authService.isLoggedIn()) {
    try {
      if (keycloak.clientId) {
        await keycloak.login({ redirectUri: window.location.origin + state.url });
        return false;
      }
    } catch {
      // Keycloak unavailable
    }
    return router.createUrlTree(['/login'], {
      queryParams: { returnUrl: state.url }
    });
  }

  // Check admin role
  if (authService.isAdmin()) {
    return true;
  }

  return router.createUrlTree(['/']);
};
