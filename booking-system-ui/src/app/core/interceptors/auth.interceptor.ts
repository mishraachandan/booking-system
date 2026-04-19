import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject, Injector } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Legacy auth interceptor — fallback for API calls not covered by Keycloak's
 * includeBearerTokenInterceptor (e.g. calls with a legacy JJWT token in localStorage).
 *
 * Handles 401/403 errors:
 *   - 401: Trigger Keycloak login redirect
 *   - 403: Navigate home with an error message
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  // Inject the injector to resolve AuthService lazily (breaks circular dependency)
  const injector = inject(Injector);

  if (req.url.includes('/api/auth/')) {
    return next(req);
  }

  let authReq = req;
  if (!req.headers.has('Authorization')) {
    const legacyToken = localStorage.getItem('accessToken');
    if (legacyToken) {
      authReq = req.clone({ setHeaders: { Authorization: `Bearer ${legacyToken}` } });
    }
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Resolve AuthService dynamically to prevent circular DI
        const authService = injector.get(AuthService, null);
        
        if (authService) {
          authService.logout();
          authService.login();
        } else {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          router.navigate(['/login']);
        }
      }

      if (error.status === 403) {
        if (router.url !== '/') {
            router.navigate(['/']);
        }
      }

      return throwError(() => error);
    })
  );
};
