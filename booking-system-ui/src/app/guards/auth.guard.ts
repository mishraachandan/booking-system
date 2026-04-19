import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import Keycloak from 'keycloak-js';

export const authGuard: CanActivateFn = async (route, state) => {
  const keycloak = inject(Keycloak);

  if (keycloak.authenticated) {
    return true;
  }

  // Not authenticated → redirect to Keycloak login
  await keycloak.login({ redirectUri: window.location.origin + state.url });
  return false;
};

export const adminGuard: CanActivateFn = async (route, state) => {
  const keycloak = inject(Keycloak);
  const router = inject(Router);

  if (!keycloak.authenticated) {
    await keycloak.login({ redirectUri: window.location.origin + state.url });
    return false;
  }

  const roles = keycloak.realmAccess?.roles ?? [];
  if (roles.includes('ADMIN')) {
    return true;
  }

  return router.createUrlTree(['/']);
};
