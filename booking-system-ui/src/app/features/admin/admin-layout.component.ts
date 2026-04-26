import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="admin-shell">
      <aside class="admin-sidebar">
        <div class="admin-brand">🛠️ Admin Console</div>
        <nav class="admin-nav">
          <a routerLink="/admin/analytics" routerLinkActive="active">📊 Analytics</a>
          <a routerLink="/admin/pricing" routerLinkActive="active">💰 Pricing Rules</a>
        </nav>
      </aside>
      <main class="admin-main">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .admin-shell {
      display: grid;
      grid-template-columns: 240px 1fr;
      min-height: calc(100vh - 64px);
      margin-top: 64px;
    }
    .admin-sidebar {
      background: rgba(20, 20, 30, 0.9);
      border-right: 1px solid var(--border);
      padding: 24px 16px;
    }
    .admin-brand {
      font-family: 'Outfit', sans-serif;
      font-size: 16px;
      font-weight: 700;
      color: var(--text-primary);
      margin-bottom: 20px;
      padding: 0 12px;
      letter-spacing: 0.5px;
    }
    .admin-nav {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .admin-nav a {
      display: block;
      padding: 10px 14px;
      border-radius: var(--radius-sm);
      color: var(--text-secondary);
      font-size: 14px;
      font-weight: 500;
      text-decoration: none;
      transition: var(--transition);
    }
    .admin-nav a:hover {
      color: var(--text-primary);
      background: var(--bg-card);
    }
    .admin-nav a.active {
      background: var(--accent);
      color: #fff;
    }
    .admin-main {
      padding: 32px 40px;
      overflow-y: auto;
    }
  `]
})
export class AdminLayoutComponent {}
