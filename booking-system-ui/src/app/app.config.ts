import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  provideKeycloak,
  withAutoRefreshToken,
  AutoRefreshTokenService,
  UserActivityService,
  createInterceptorCondition,
  IncludeBearerTokenCondition,
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  includeBearerTokenInterceptor,
} from 'keycloak-angular';

import { routes } from './app.routes';
import { traceIdInterceptor } from './core/interceptors/trace-id.interceptor';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { keycloakConfig } from './core/keycloak.config';

// Only attach KC Bearer token to calls to our own backend
const backendCondition = createInterceptorCondition<IncludeBearerTokenCondition>({
  urlPattern: /^(http:\/\/localhost:8080)|(\/api\/)/i,
  bearerPrefix: 'Bearer',
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),

    // ── Keycloak ──────────────────────────────────────────────────────────────
    // Initialize Keycloak during bootstrap WITHOUT `onLoad: 'check-sso'`.
    // This provides the adapter without blocking the Angular bootstrap
    // due to iframe/CORS issues under development mode.
    provideKeycloak({
      config: keycloakConfig,
      initOptions: {
        pkceMethod: 'S256',
        checkLoginIframe: false,
      },
      features: [
        withAutoRefreshToken({
          onInactivityTimeout: 'logout',
          sessionTimeout: 60000,
        }),
      ],
      providers: [AutoRefreshTokenService, UserActivityService],
    }),

    // Bearer token condition
    {
      provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
      useValue: [backendCondition],
    },

    // ── HTTP Interceptors ────────────────────────────────────────────────────
    provideHttpClient(
      withInterceptors([
        traceIdInterceptor,
        includeBearerTokenInterceptor,
        authInterceptor,
      ])
    ),
  ],
};
