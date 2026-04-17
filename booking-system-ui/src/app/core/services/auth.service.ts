import { Injectable, inject, effect } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, from, of } from 'rxjs';
import { tap, switchMap, catchError } from 'rxjs/operators';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEventType } from 'keycloak-angular';
import Keycloak from 'keycloak-js';

/**
 * AuthService — facade over Keycloak (keycloak-js).
 *
 * All components continue using the same AuthService interface as before.
 * Internally, tokens now come from Keycloak.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = '/api/auth';
  private loggedIn = new BehaviorSubject<boolean>(false);
  isLoggedIn$ = this.loggedIn.asObservable();

  private readonly keycloak = inject(Keycloak);
  private readonly http = inject(HttpClient);
  private readonly keycloakSignal = inject(KEYCLOAK_EVENT_SIGNAL);

  constructor() {
    effect(() => {
      const event = this.keycloakSignal();
      if (event.type === KeycloakEventType.Ready ||
          event.type === KeycloakEventType.AuthSuccess ||
          event.type === KeycloakEventType.AuthRefreshSuccess ||
          event.type === KeycloakEventType.AuthLogout) {
        this.loggedIn.next(this.keycloak ? (this.keycloak.authenticated ?? false) : false);
      }
    });
  }

  // ─── Login / Logout ────────────────────────────────────────────────────────

  async login(): Promise<void> {
    try {
      await this.keycloak.login({ redirectUri: window.location.origin + '/' });
    } catch(err) {
      console.warn('Keycloak login cancelled or failed:', err);
    }
  }

  async logout(): Promise<void> {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    this.loggedIn.next(false);
    try {
      await this.keycloak.logout({ redirectUri: window.location.origin });
    } catch(err) {
      console.warn('Keycloak logout failed:', err);
    }
  }

  // ─── Token Access ──────────────────────────────────────────────────────────

  getToken(): string | null {
    if (this.keycloak.token) return this.keycloak.token;
    return localStorage.getItem('accessToken');
  }

  isLoggedIn(): boolean {
    return this.keycloak.authenticated ?? !!localStorage.getItem('accessToken');
  }

  // ─── User Information ──────────────────────────────────────────────────────

  getUserRole(): string | null {
    const roles = this.keycloak.realmAccess?.roles ?? [];
    if (roles.includes('ADMIN')) return 'ADMIN';
    if (roles.includes('USER')) return 'USER';

    const token = localStorage.getItem('accessToken');
    if (token) {
      const payload = this.decodeTokenPayload(token);
      return payload?.role ?? null;
    }
    return null;
  }

  isAdmin(): boolean {
    return this.getUserRole() === 'ADMIN';
  }

  getFirstName(): string {
    const profile = this.keycloak.idTokenParsed as any;
    if (profile) {
      return profile.given_name ?? profile.name ?? 'User';
    }
    const token = localStorage.getItem('accessToken');
    if (token) return this.decodeTokenPayload(token)?.name ?? 'User';
    return 'User';
  }

  getUserId(): number | null {
    const token = localStorage.getItem('accessToken');
    if (token) return this.decodeTokenPayload(token)?.userId ?? null;
    return null;
  }

  // ─── Registration (OTP flow — unchanged) ──────────────────────────────────

  register(data: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phone: string;
  }): Observable<string> {
    return this.http.post(`${this.baseUrl}/register`, data, { responseType: 'text' });
  }

  verifyOtp(email: string, otp: string): Observable<string> {
    return this.http.post(`${this.baseUrl}/verify-otp`, { email, otp }, { responseType: 'text' });
  }

  // ─── Keycloak Sync ────────────────────────────────────────────────────────

  keycloakSync(): Observable<any> {
    if (!this.keycloak.tokenParsed) return of(null);
    const payload = this.keycloak.tokenParsed as any;
    return this.http.post(`${this.baseUrl}/keycloak-sync`, {
      keycloakId: payload.sub,
      email: payload.email,
      firstName: payload.given_name ?? '',
      lastName: payload.family_name ?? '',
    }).pipe(
      tap(() => this.loggedIn.next(true)),
      catchError(err => {
        console.warn('Keycloak sync warning:', err?.error?.error ?? err.message);
        return of(null);
      })
    );
  }

  // ─── Token Refresh ────────────────────────────────────────────────────────

  refreshAccessToken(): Observable<any> {
    return from(this.keycloak.updateToken(30)).pipe(
      switchMap(() => of({ accessToken: this.getToken() }))
    );
  }

  // ─── Legacy Token Decode ──────────────────────────────────────────────────

  private decodeTokenPayload(token: string): any {
    try {
      const base64 = token.split('.')[1];
      return JSON.parse(atob(base64.replace(/-/g, '+').replace(/_/g, '/')));
    } catch {
      return null;
    }
  }

  isTokenExpired(token: string): boolean {
    const payload = this.decodeTokenPayload(token);
    if (!payload) return true;
    return payload.exp * 1000 < Date.now();
  }
}
