import { Component, inject, effect, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeService } from '../../../core/services/theme.service';
import { CityService, City } from '../../../core/services/city.service';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEventType } from 'keycloak-angular';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, CommonModule, FormsModule],
  template: `
    <nav class="navbar">
      <div class="navbar-inner container">
        <div class="nav-left">
          <a routerLink="/" class="logo">
            <span class="logo-icon">🎬</span>
            <span class="logo-text">Book<span class="accent">My</span>Show</span>
          </a>

          <!-- City Selector in Navbar -->
          <div class="nav-city-wrap">
            <span class="city-pin">📍</span>
            <select
              class="nav-city-select"
              [ngModel]="selectedCityId()"
              (ngModelChange)="onCitySelected($event)"
              aria-label="Select City">
              @for (city of cities; track city.id) {
                <option [value]="city.id">{{ city.name }}</option>
              }
            </select>
          </div>
        </div>

        <div class="nav-links">
          <button class="theme-toggle-btn" (click)="toggleTheme()" aria-label="Toggle Theme">
            {{ currentTheme() === 'dark' ? '☀️' : '🌙' }}
          </button>
          @if (isLoggedIn) {
            <span class="user-greeting">👤 {{ firstName }}</span>
            <a routerLink="/my-bookings" class="nav-link">My Bookings</a>
            @if (isAdmin) {
              <a routerLink="/admin" class="nav-link admin-link">⚙️ Admin</a>
            }
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
      background: var(--glass-bg);
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      border-bottom: 1px solid var(--border);
      z-index: 1000;
    }
    .navbar-inner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 100%;
    }
    .nav-left {
      display: flex;
      align-items: center;
      gap: 24px;
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

    .nav-city-wrap {
      display: flex;
      align-items: center;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-sm);
      padding: 4px 8px;
      gap: 4px;
      transition: var(--transition);
      &:hover { border-color: var(--accent); box-shadow: 0 0 10px var(--accent-glow); }
    }
    .city-pin { font-size: 14px; }
    .nav-city-select {
      background: transparent;
      border: none;
      color: var(--text-primary);
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      padding-right: 4px;
      outline: none;
      option {
        background: var(--bg-secondary);
        color: var(--text-primary);
      }
    }

    .nav-links {
      display: flex;
      align-items: center;
      gap: 16px;
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

    @media (max-width: 680px) {
      .nav-city-wrap { display: none; }
      .nav-links { gap: 8px; }
      .nav-link { padding: 6px 10px; font-size: 12px; }
    }
  `]
})
export class NavbarComponent implements OnInit {
  isLoggedIn = false;
  isAdmin = false;
  firstName = '';
  cities: City[] = [];

  private readonly authService = inject(AuthService);
  private readonly themeService = inject(ThemeService);
  private readonly cityService = inject(CityService);
  private readonly keycloakSignal = inject(KEYCLOAK_EVENT_SIGNAL);

  currentTheme = this.themeService.theme;
  selectedCityId = this.cityService.selectedCityId;

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
          this.isAdmin = this.authService.isAdmin();
        } else {
          this.isAdmin = false;
        }
      }
    });
  }

  ngOnInit() {
    this.cityService.getCities().subscribe(c => {
      this.cities = c;
    });
  }

  onCitySelected(cityId: any) {
    const id = Number(cityId);
    const found = this.cities.find(c => c.id === id);
    if (found) {
      this.cityService.setCity(found.id, found.name);
    }
  }

  toggleTheme() {
    this.themeService.toggleTheme();
  }

  logout() {
    this.authService.logout();
  }
}

