import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../core/services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.getUserId()) {
    return true;
  }

  // Save the attempted URL and redirect to login
  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;
};
