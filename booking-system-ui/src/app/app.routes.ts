import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent) },
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) },
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
  { path: '**', redirectTo: '' }
];
