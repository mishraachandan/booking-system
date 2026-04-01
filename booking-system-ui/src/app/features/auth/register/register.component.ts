import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-page page-enter">
      <div class="auth-card card">
        @if (!otpSent) {
          <div class="auth-header">
            <h1>Create account</h1>
            <p>Join BookMyShow today</p>
          </div>

          <form (ngSubmit)="onRegister()">
            <div class="row">
              <div class="form-group half">
                <label>First Name</label>
                <input type="text" [(ngModel)]="firstName" name="firstName" placeholder="John" required />
              </div>
              <div class="form-group half">
                <label>Last Name</label>
                <input type="text" [(ngModel)]="lastName" name="lastName" placeholder="Doe" required />
              </div>
            </div>
            <div class="form-group">
              <label>Email</label>
              <input type="email" [(ngModel)]="email" name="email" placeholder="you@email.com" required />
            </div>
            <div class="form-group">
              <label>Phone</label>
              <input type="tel" [(ngModel)]="phone" name="phone" placeholder="+91 99999 99999" required />
            </div>
            <div class="form-group">
              <label>Password</label>
              <input type="password" [(ngModel)]="password" name="password" placeholder="Min 8 characters" required />
            </div>

            @if (error) { <div class="error-msg">{{ error }}</div> }
            @if (message) { <div class="success-msg">{{ message }}</div> }

            <button type="submit" class="btn btn-primary full-width" [disabled]="loading">
              {{ loading ? 'Registering...' : 'Create Account' }}
            </button>
          </form>
        } @else {
          <div class="auth-header">
            <h1>Verify OTP</h1>
            <p>Enter the 6-digit code sent to {{ email }}</p>
            <div class="dev-hint">💡 Dev mode: Use OTP <strong>123456</strong></div>
          </div>

          <form (ngSubmit)="onVerifyOtp()">
            <div class="form-group">
              <label>OTP Code</label>
              <input type="text" [(ngModel)]="otp" name="otp" placeholder="000000" maxlength="6" required
                     style="text-align: center; font-size: 24px; letter-spacing: 8px;" />
            </div>

            @if (error) { <div class="error-msg">{{ error }}</div> }
            @if (message) { <div class="success-msg">{{ message }}</div> }

            <button type="submit" class="btn btn-primary full-width" [disabled]="loading">
              {{ loading ? 'Verifying...' : 'Verify' }}
            </button>
          </form>
        }

        <p class="auth-footer">
          Already have an account? <a routerLink="/login" class="link">Sign in</a>
        </p>
      </div>
    </div>
  `,
  styles: [`
    .auth-page {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: calc(100vh - 64px);
      padding: 20px;
    }
    .auth-card { width: 100%; max-width: 480px; padding: 40px; }
    .auth-header {
      text-align: center; margin-bottom: 32px;
      h1 { font-size: 28px; font-weight: 700; margin-bottom: 8px; }
      p { color: var(--text-muted); font-size: 15px; }
    }
    .row { display: flex; gap: 12px; }
    .half { flex: 1; }
    .full-width { width: 100%; }
    .error-msg {
      background: rgba(231, 76, 60, 0.1);
      border: 1px solid rgba(231, 76, 60, 0.3);
      color: var(--danger); padding: 10px 14px;
      border-radius: var(--radius-sm); font-size: 13px; margin-bottom: 16px;
    }
    .success-msg {
      background: rgba(46, 204, 113, 0.1);
      border: 1px solid rgba(46, 204, 113, 0.3);
      color: var(--success); padding: 10px 14px;
      border-radius: var(--radius-sm); font-size: 13px; margin-bottom: 16px;
    }
    .auth-footer {
      text-align: center; margin-top: 24px;
      font-size: 14px; color: var(--text-muted);
      .link { color: var(--accent); font-weight: 500; }
    }
    .dev-hint {
      margin-top: 12px; padding: 8px 14px; font-size: 13px;
      background: rgba(241, 196, 15, 0.1); border: 1px solid rgba(241, 196, 15, 0.3);
      border-radius: var(--radius-sm); color: #f1c40f;
    }
  `]
})
export class RegisterComponent {
  firstName = ''; lastName = ''; email = ''; phone = ''; password = '';
  otp = '123456'; otpSent = false;
  error = ''; message = ''; loading = false;

  constructor(private auth: AuthService, private router: Router) {}

  onRegister() {
    this.loading = true; this.error = ''; this.message = '';
    this.auth.register({ email: this.email, password: this.password, firstName: this.firstName, lastName: this.lastName, phone: this.phone })
      .subscribe({
        next: (msg) => { this.message = msg; this.otpSent = true; this.loading = false; },
        error: (err) => {
          this.error = this.extractError(err);
          this.loading = false;
        }
      });
  }

  onVerifyOtp() {
    this.loading = true; this.error = ''; this.message = '';
    this.auth.verifyOtp(this.email, this.otp).subscribe({
      next: (msg) => { this.message = msg; setTimeout(() => this.router.navigate(['/login']), 500); this.loading = false; },
      error: (err) => { this.error = this.extractError(err); this.loading = false; }
    });
  }

  private extractError(err: any): string {
    let body = err.error;
    // When responseType is 'text', error body arrives as a raw JSON string — parse it
    if (typeof body === 'string') {
      try { body = JSON.parse(body); } catch { return body; }
    }
    if (!body) return 'Something went wrong. Please try again.';
    // Backend validation errors: { message, errors: { field: 'msg' } }
    if (body.errors && typeof body.errors === 'object') {
      return Object.values(body.errors).join(' · ');
    }
    if (body.message) return body.message;
    return 'Something went wrong. Please try again.';
  }
}
