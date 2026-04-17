import { Component, inject, effect } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEventType } from 'keycloak-angular';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, CommonModule],
  template: `
    <nav class="navbar">
      <div class="navbar-inner container">
        <a routerLink="/" class="logo">
          <span class="logo-icon">🎬</span>
          <span class="logo-text">Book<span class="accent">My</span>Show</span>
        </a>

        <div class="nav-links">
          @if (isLoggedIn) {
            <span class="user-greeting">👤 {{ firstName }}</span>
            <a routerLink="/my-bookings" class="nav-link">My Bookings</a>
            <button class="btn btn-ghost" (click)="logout()">Logout</button>
          } @else {
            <a routerLink="/login" class="btn btn-outline">Sign In</a>
            <a routerLink="/register" class="btn btn-primary">Sign Up</a>
          }
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      height: 64px;
      background: rgba(10, 10, 15, 0.85);
      backdrop-filter: blur(20px);
      border-bottom: 1px solid var(--border);
      z-index: 1000;
    }
    .navbar-inner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 100%;
    }
    .logo {
      display: flex;
      align-items: center;
      gap: 10px;
      font-family: 'Outfit', sans-serif;
      font-size: 22px;
      font-weight: 700;
    }
    .logo-icon { font-size: 26px; }
    .accent { color: var(--accent); }
    .nav-links {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .nav-link {
      color: var(--text-secondary);
      font-size: 14px;
      font-weight: 500;
      padding: 8px 16px;
      border-radius: var(--radius-sm);
      transition: var(--transition);
      &:hover { color: var(--text-primary); background: var(--bg-card); }
    }
    .user-greeting {
      font-size: 14px;
      color: var(--text-muted);
      font-weight: 500;
      padding: 0 8px;
    }
  `]
})
export class NavbarComponent {
  isLoggedIn = false;
  firstName = '';

  private readonly authService = inject(AuthService);
  private readonly keycloakSignal = inject(KEYCLOAK_EVENT_SIGNAL);

  constructor() {
    // React to Keycloak events via signal (v21 API)
    effect(() => {
      const event = this.keycloakSignal();
      if (event.type === KeycloakEventType.Ready ||
          event.type === KeycloakEventType.AuthSuccess ||
          event.type === KeycloakEventType.AuthRefreshSuccess ||
          event.type === KeycloakEventType.AuthLogout) {
        this.isLoggedIn = this.authService.isLoggedIn();
        if (this.isLoggedIn) {
          this.firstName = this.authService.getFirstName();
        }
      }
    });
  }

  logout() {
    this.authService.logout();
  }
}
