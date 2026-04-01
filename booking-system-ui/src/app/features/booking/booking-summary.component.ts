import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BookingService } from '../../core/services/booking.service';

@Component({
  selector: 'app-booking-summary',
  imports: [CommonModule, RouterLink],
  template: `
    <div class="summary-page page-enter">
      <div class="summary-card card">
        @if (confirmed) {
          <div class="success-state">
            <div class="check-circle">✓</div>
            <h1>Booking Confirmed!</h1>
            <p class="booking-id">Booking #{{ bookingId }}</p>
            <p class="total">Total: ₹{{ total }}</p>
            <div class="actions">
              <a routerLink="/my-bookings" class="btn btn-primary">View My Bookings</a>
              <a routerLink="/" class="btn btn-outline">Browse More</a>
            </div>
          </div>
        } @else {
          <div class="confirm-state">
            <h1>Complete Your Booking</h1>
            <div class="details">
              <div class="detail-row">
                <span>Booking ID</span>
                <span>#{{ bookingId }}</span>
              </div>
              <div class="detail-row">
                <span>Total Amount</span>
                <span class="price">₹{{ total }}</span>
              </div>
            </div>

            @if (error) { <div class="error-msg">{{ error }}</div> }

            <button class="btn btn-primary full-width" (click)="confirmPayment()" [disabled]="loading">
              {{ loading ? 'Processing...' : 'Confirm & Pay ₹' + total }}
            </button>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .summary-page {
      display: flex; justify-content: center; align-items: center;
      min-height: calc(100vh - 64px); padding: 20px;
    }
    .summary-card { width: 100%; max-width: 460px; padding: 48px; text-align: center; }
    h1 { font-size: 26px; font-weight: 700; margin-bottom: 8px; }

    .success-state {
      .check-circle {
        width: 72px; height: 72px; border-radius: 50%;
        background: var(--accent-gradient); color: white;
        display: flex; align-items: center; justify-content: center;
        font-size: 36px; font-weight: 700; margin: 0 auto 20px;
        animation: pop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
      }
      .booking-id { color: var(--text-muted); font-size: 14px; margin-bottom: 8px; }
      .total { font-size: 28px; font-weight: 700; margin-bottom: 28px; }
      .actions { display: flex; gap: 12px; justify-content: center; }
    }

    @keyframes pop {
      0% { transform: scale(0); }
      100% { transform: scale(1); }
    }

    .confirm-state {
      text-align: left;
      h1 { text-align: center; margin-bottom: 28px; }
    }
    .details {
      background: var(--bg-secondary); border-radius: var(--radius-sm);
      padding: 16px; margin-bottom: 24px;
    }
    .detail-row {
      display: flex; justify-content: space-between; padding: 8px 0;
      font-size: 15px; color: var(--text-secondary);
      &:not(:last-child) { border-bottom: 1px solid var(--border); }
      .price { font-weight: 700; color: var(--text-primary); font-size: 18px; }
    }
    .full-width { width: 100%; }
    .error-msg {
      background: rgba(231, 76, 60, 0.1); border: 1px solid rgba(231, 76, 60, 0.3);
      color: var(--danger); padding: 10px; border-radius: var(--radius-sm);
      font-size: 13px; margin-bottom: 16px;
    }
  `]
})
export class BookingSummaryComponent implements OnInit {
  bookingId = 0;
  total = 0;
  confirmed = false;
  loading = false;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private bookingService: BookingService
  ) {}

  ngOnInit() {
    this.bookingId = Number(this.route.snapshot.queryParamMap.get('bookingId'));
    this.total = Number(this.route.snapshot.queryParamMap.get('total'));
  }

  confirmPayment() {
    this.loading = true;
    this.error = '';
    this.bookingService.confirmBooking(this.bookingId).subscribe({
      next: () => { this.confirmed = true; this.loading = false; },
      error: (err) => { this.error = err.error || 'Payment failed'; this.loading = false; }
    });
  }
}
