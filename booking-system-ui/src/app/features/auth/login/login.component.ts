import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

/**
 * Login component — now a minimal redirect bridge.
 * When the route /login is hit, it immediately redirects the user to the
 * Keycloak-hosted login page. After successful login, Keycloak redirects back
 * to the app (via redirect_uri = returnUrl or '/').
 *
 * If the user is already logged in, they are sent directly to the return URL.
 */
@Component({
  selector: 'app-login',
  imports: [CommonModule],
  template: `
    <div class="auth-page page-enter">
      <div class="auth-card card">
        <div class="auth-header">
          <div class="kc-logo">🔐</div>
          <h1>Signing you in…</h1>
          <p>You are being redirected to the secure login page.</p>
          @if (fromBooking) {
            <div class="info-banner">🎬 Sign in to complete your booking</div>
          }
        </div>

        <div class="spinner-row">
          <div class="spinner"></div>
        </div>

        <p class="redirect-note">
          Redirecting to Keycloak SSO…
        </p>

        <button class="btn btn-primary full-width" (click)="redirectToLogin()">
          Click here if you are not redirected
        </button>
      </div>
    </div>
  `,
  styles: [`
    .auth-page {
      display: flex; justify-content: center; align-items: center;
      min-height: calc(100vh - 64px); padding: 20px;
    }
    .auth-card { width: 100%; max-width: 420px; padding: 40px; text-align: center; }
    .auth-header {
      margin-bottom: 32px;
      .kc-logo { font-size: 48px; margin-bottom: 16px; }
      h1 { font-size: 26px; font-weight: 700; margin-bottom: 8px; }
      p { color: var(--text-muted); font-size: 15px; }
    }
    .info-banner {
      margin-top: 12px; padding: 10px 14px;
      background: rgba(226, 55, 68, 0.1); border: 1px solid rgba(226, 55, 68, 0.3);
      border-radius: var(--radius-sm); color: var(--accent); font-size: 13px;
    }
    .spinner-row { display: flex; justify-content: center; margin: 24px 0; }
    .spinner {
      width: 36px; height: 36px; border-radius: 50%;
      border: 3px solid var(--border);
      border-top-color: var(--accent);
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .redirect-note { color: var(--text-muted); font-size: 13px; margin-bottom: 20px; }
    .full-width { width: 100%; }
  `]
})
export class LoginComponent implements OnInit {
  fromBooking = false;
  private returnUrl = '/';

  constructor(
    private auth: AuthService,
    private route: ActivatedRoute
  ) {
    this.returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/';
    this.fromBooking =
      this.returnUrl.includes('/show/') || this.returnUrl.includes('/booking');
  }

  ngOnInit() {
    // Auto-redirect on load
    setTimeout(() => this.redirectToLogin(), 500);
  }

  redirectToLogin() {
    this.auth.login();
  }
}
