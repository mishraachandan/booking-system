import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BookingService, BookingResponse } from '../../core/services/booking.service';

@Component({
  selector: 'app-my-bookings',
  imports: [CommonModule, RouterLink],
  template: `
    <div class="bookings-page page-enter container">
      <h1 class="page-title">My Bookings</h1>

      @if (loading) {
        <div class="loading">Loading your bookings...</div>
      } @else if (bookings.length === 0) {
        <div class="empty-state">
          <span class="empty-icon">🎫</span>
          <p>No bookings yet</p>
          <a routerLink="/" class="btn btn-primary">Browse Shows</a>
        </div>
      } @else {
        <div class="bookings-list">
          @for (booking of bookings; track booking.bookingId) {
            <div class="card booking-card">
              <div class="booking-main">
                <div class="booking-info">
                  <h3>{{ booking.movieTitle || booking.resourceName || 'Event Booking' }}</h3>
                  <p class="meta">
                    @if (booking.cinemaName) {
                      {{ booking.cinemaName }} · {{ booking.screenName }}
                    }
                  </p>
                  <p class="meta">{{ booking.showStartTime || booking.startTime | date:'MMM d, yyyy · hh:mm a' }}</p>
                  <p class="tickets">{{ booking.numberOfTickets }} ticket{{ booking.numberOfTickets > 1 ? 's' : '' }}</p>
                </div>
                <div class="booking-status">
                  <span class="status-badge" [class]="booking.status.toLowerCase()">
                    {{ booking.status | titlecase }}
                  </span>
                  <span class="booking-id">#{{ booking.bookingId }}</span>
                </div>
              </div>
              @if (booking.status === 'CONFIRMED' || booking.status === 'AWAITING_PAYMENT') {
                <div class="booking-actions">
                  <button class="btn btn-ghost danger" (click)="cancel(booking.bookingId)">Cancel Booking</button>
                </div>
              }
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .bookings-page { padding: 32px 20px 60px; }
    .page-title { font-size: 28px; font-weight: 700; margin-bottom: 28px; }
    .loading { text-align: center; padding: 60px; color: var(--text-muted); }
    .empty-state {
      text-align: center; padding: 80px 20px; color: var(--text-muted);
      .empty-icon { font-size: 48px; display: block; margin-bottom: 16px; }
      p { margin-bottom: 24px; }
    }
    .bookings-list { display: flex; flex-direction: column; gap: 16px; }
    .booking-card { padding: 24px; }
    .booking-main { display: flex; justify-content: space-between; align-items: flex-start; }
    .booking-info {
      h3 { font-size: 18px; font-weight: 600; margin-bottom: 4px; }
      .meta { font-size: 13px; color: var(--text-muted); margin-bottom: 2px; }
      .tickets { font-size: 14px; color: var(--text-secondary); margin-top: 8px; }
    }
    .booking-status { text-align: right; }
    .booking-id { display: block; font-size: 12px; color: var(--text-muted); margin-top: 4px; }
    .status-badge {
      display: inline-block; padding: 4px 12px; border-radius: 20px;
      font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px;
      &.confirmed { background: rgba(46, 204, 113, 0.15); color: var(--success); }
      &.awaiting_payment { background: rgba(243, 156, 18, 0.15); color: var(--warning); }
      &.cancelled { background: rgba(231, 76, 60, 0.15); color: var(--danger); }
      &.pending { background: rgba(149, 165, 166, 0.15); color: var(--text-muted); }
      &.expired { background: rgba(149, 165, 166, 0.15); color: var(--text-muted); }
      &.completed { background: rgba(52, 152, 219, 0.15); color: #3498db; }
    }
    .booking-actions {
      border-top: 1px solid var(--border); margin-top: 16px; padding-top: 12px;
      text-align: right;
      .danger { color: var(--danger); &:hover { color: var(--danger); background: rgba(231, 76, 60, 0.1); } }
    }
  `]
})
export class MyBookingsComponent implements OnInit {
  bookings: BookingResponse[] = [];
  loading = true;

  constructor(private bookingService: BookingService) {}

  ngOnInit() {
    this.loadBookings();
  }

  loadBookings() {
    this.bookingService.getMyBookings().subscribe({
      next: (b) => { this.bookings = b; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  cancel(bookingId: number) {
    if (confirm('Are you sure you want to cancel this booking?')) {
      this.bookingService.cancelBooking(bookingId).subscribe(() => {
        this.loadBookings();
      });
    }
  }
}
