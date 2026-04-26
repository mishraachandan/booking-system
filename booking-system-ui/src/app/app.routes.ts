import { Routes } from '@angular/router';
import { authGuard, adminGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent) },
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) },
  {
    path: 'movies/:movieId',
    loadComponent: () => import('./features/movie-detail/movie-detail.component').then(m => m.MovieDetailComponent)
  },
  {
    path: 'show/:showId/seats',
    loadComponent: () => import('./features/seat-selection/seat-selection.component').then(m => m.SeatSelectionComponent),
    canActivate: [authGuard]
  },
  {
    path: 'booking/summary',
    loadComponent: () => import('./features/booking/booking-summary.component').then(m => m.BookingSummaryComponent),
    canActivate: [authGuard]
  },
  {
    path: 'my-bookings',
    loadComponent: () => import('./features/my-bookings/my-bookings.component').then(m => m.MyBookingsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'admin',
    loadComponent: () => import('./features/admin/admin-layout.component').then(m => m.AdminLayoutComponent),
    canActivate: [adminGuard],
    children: [
      { path: '', redirectTo: 'analytics', pathMatch: 'full' },
      {
        path: 'analytics',
        loadComponent: () => import('./features/admin/analytics-dashboard.component').then(m => m.AnalyticsDashboardComponent)
      },
      {
        path: 'pricing',
        loadComponent: () => import('./features/admin/pricing-rules.component').then(m => m.PricingRulesComponent)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
