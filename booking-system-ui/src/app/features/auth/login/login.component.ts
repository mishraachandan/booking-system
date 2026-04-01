import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-page page-enter">
      <div class="auth-card card">
        <div class="auth-header">
          <h1>Welcome back</h1>
          <p>Sign in to your account</p>
          @if (fromBooking) {
            <div class="info-banner">🎬 Please sign in to complete your booking</div>
          }
        </div>

        <form (ngSubmit)="onLogin()">
          <div class="form-group">
            <label>Email</label>
            <input type="email" [(ngModel)]="email" name="email" placeholder="you@email.com" required />
          </div>
          <div class="form-group">
            <label>Password</label>
            <input type="password" [(ngModel)]="password" name="password" placeholder="••••••••" required />
          </div>

          @if (error) {
            <div class="error-msg">{{ error }}</div>
          }

          <button type="submit" class="btn btn-primary full-width" [disabled]="loading">
            {{ loading ? 'Signing in...' : 'Sign In' }}
          </button>
        </form>

        <p class="auth-footer">
          Don't have an account? <a routerLink="/register" class="link">Sign up</a>
        </p>
      </div>
    </div>
  `,
  styles: [`
    .auth-page {
      display: flex; justify-content: center; align-items: center;
      min-height: calc(100vh - 64px); padding: 20px;
    }
    .auth-card { width: 100%; max-width: 420px; padding: 40px; }
    .auth-header {
      text-align: center; margin-bottom: 32px;
      h1 { font-size: 28px; font-weight: 700; margin-bottom: 8px; }
      p { color: var(--text-muted); font-size: 15px; }
    }
    .info-banner {
      margin-top: 12px; padding: 10px 14px;
      background: rgba(226, 55, 68, 0.1); border: 1px solid rgba(226, 55, 68, 0.3);
      border-radius: var(--radius-sm); color: var(--accent); font-size: 13px;
    }
    .full-width { width: 100%; }
    .error-msg {
      background: rgba(231, 76, 60, 0.1); border: 1px solid rgba(231, 76, 60, 0.3);
      color: var(--danger); padding: 10px 14px;
      border-radius: var(--radius-sm); font-size: 13px; margin-bottom: 16px;
    }
    .auth-footer {
      text-align: center; margin-top: 24px; font-size: 14px; color: var(--text-muted);
      .link { color: var(--accent); font-weight: 500; }
    }
  `]
})
export class LoginComponent {
  email = '';
  password = '';
  error = '';
  loading = false;
  returnUrl = '/';
  fromBooking = false;

  constructor(
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/';
    this.fromBooking = this.returnUrl.includes('/show/') || this.returnUrl.includes('/booking');
  }

  onLogin() {
    this.loading = true;
    this.error = '';
    this.auth.login(this.email, this.password).subscribe({
      next: () => { this.router.navigateByUrl(this.returnUrl); },
      error: (err) => {
        this.error = err.error || err.message || 'Invalid email or password';
        this.loading = false;
      }
    });
  }
}
