import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BookingService, BookingResponse } from '../../core/services/booking.service';

type Tab = 'upcoming' | 'past';

@Component({
  selector: 'app-my-bookings',
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="bookings-page page-enter container">
      <h1 class="page-title">My Bookings</h1>

      <!-- Tabs -->
      @if (!loading && bookings.length > 0) {
        <div class="tabs">
          <button
            class="tab"
            [class.active]="activeTab === 'upcoming'"
            (click)="activeTab = 'upcoming'">
            Upcoming <span class="tab-count">{{ upcomingBookings.length }}</span>
          </button>
          <button
            class="tab"
            [class.active]="activeTab === 'past'"
            (click)="activeTab = 'past'">
            Past <span class="tab-count">{{ pastBookings.length }}</span>
          </button>
        </div>
      }

      @if (loading) {
        <div class="loading">Loading your bookings...</div>
      } @else if (bookings.length === 0) {
        <div class="empty-state">
          <span class="empty-icon">🎫</span>
          <p>No bookings yet</p>
          <a routerLink="/" class="btn btn-primary">Browse Shows</a>
        </div>
      } @else {
        @if (visibleBookings.length === 0) {
          <div class="empty-state small">
            <p>No {{ activeTab === 'upcoming' ? 'upcoming' : 'past' }} bookings.</p>
          </div>
        }
        <div class="bookings-list">
          @for (booking of visibleBookings; track booking.bookingId) {
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
                  <p class="tickets">
                    {{ booking.numberOfTickets }} ticket{{ booking.numberOfTickets > 1 ? 's' : '' }}
                    @if (booking.grandTotal != null) {
                      · <strong>₹{{ booking.grandTotal }}</strong>
                    }
                  </p>

                  @if (booking.addOns && booking.addOns.length > 0) {
                    <div class="addons-block">
                      <div class="addons-header">🍿 Add-ons</div>
                      <ul class="addons-list">
                        @for (a of booking.addOns; track a.id) {
                          <li>
                            <span>{{ a.name }} × {{ a.quantity }}</span>
                            <span class="addon-total">₹{{ a.lineTotal }}</span>
                          </li>
                        }
                      </ul>
                    </div>
                  }
                  @if (booking.status === 'CONFIRMED') {
                    <div class="qr-block">
                      <div class="qr-header">🎟️ ENTRY TICKET QR</div>
                      <div class="qr-container">
                        <img [src]="'https://api.qrserver.com/v1/create-qr-code/?size=120x120&data=BOOKING-' + booking.bookingId" 
                             alt="Check-in QR Code" 
                             class="qr-image" />
                        <div class="qr-caption">Scan at entry gate</div>
                      </div>
                    </div>
                  }
                </div>
                <div class="booking-status">
                  <span class="status-badge" [class]="booking.status.toLowerCase()">
                    {{ booking.status | titlecase }}
                  </span>
                  <span class="booking-id">#{{ booking.bookingId }}</span>
                </div>
              </div>
              @if (canCancel(booking)) {
                <div class="booking-actions">
                  <button class="btn btn-ghost danger" (click)="openCancelModal(booking)">
                    Cancel Booking
                  </button>
                </div>
              }
            </div>
          }
        </div>
      }

      <!-- Cancel Confirmation Modal -->
      @if (cancelTarget) {
        <div class="modal-backdrop" (click)="closeCancelModal()">
          <div class="modal-card" (click)="$event.stopPropagation()">
            <h2>Cancel booking #{{ cancelTarget.bookingId }}?</h2>
            <p class="modal-sub">{{ cancelTarget.movieTitle || cancelTarget.resourceName }}</p>

            <div class="policy-box">
              <h3>Refund policy</h3>
              <ul>
                <li>Cancellation &gt; 24h before show: <strong>full refund</strong> (minus gateway fees).</li>
                <li>Cancellation 2h – 24h before show: <strong>50% refund</strong>.</li>
                <li>Cancellation &lt; 2h before show / after show: <strong>no refund</strong>.</li>
                <li>Add-ons (food &amp; beverage) are refundable only if cancelled &gt; 24h before show.</li>
                <li>Refunds are processed to the original payment method within 5–7 working days.</li>
              </ul>
            </div>

            <div class="tc-box">
              <label>
                <input type="checkbox" [(ngModel)]="acceptedTc" #tc (change)="acceptedTc = tc.checked" />
                I have read and accepted the refund policy and terms of cancellation.
              </label>
            </div>

            @if (cancelError) {
              <div class="error-msg">{{ cancelError }}</div>
            }

            <div class="modal-actions">
              <button class="btn btn-outline" (click)="closeCancelModal()" [disabled]="cancelling">
                Keep booking
              </button>
              <button
                class="btn btn-primary danger"
                (click)="confirmCancel()"
                [disabled]="!acceptedTc || cancelling">
                {{ cancelling ? 'Cancelling...' : 'Yes, cancel booking' }}
              </button>
            </div>
          </div>
        </div>
      }

      <!-- Refund Notification Modal -->
      @if (refundNotification) {
        <div class="modal-backdrop" (click)="closeRefundNotification()">
          <div class="modal-card success-card" (click)="$event.stopPropagation()">
            <div class="success-icon">💰</div>
            <h2>Refund Simulated Successfully!</h2>
            <p class="modal-sub">Booking Cancellation Confirmed</p>

            <div class="refund-detail-box">
              <p>A simulated refund of <strong>₹{{ refundNotification.amount }}</strong> has been initiated to your original payment method.</p>
              <div class="alert alert-info">
                ℹ️ <strong>Mock Mode:</strong> The Razorpay API keys are not configured. The actual refund transaction has been mocked in the system.
              </div>
            </div>

            <div class="modal-actions">
              <button class="btn btn-primary" (click)="closeRefundNotification()">
                Okay, great!
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .bookings-page { padding: 32px 20px 60px; }
    .page-title { font-size: 28px; font-weight: 700; margin-bottom: 28px; }
    .loading { text-align: center; padding: 60px; color: var(--text-muted); }

    .tabs {
      display: flex; gap: 8px; margin-bottom: 24px;
      border-bottom: 1px solid var(--border);
    }
    .tab {
      background: none; border: none; padding: 12px 20px; cursor: pointer;
      color: var(--text-muted); font-size: 14px; font-weight: 600;
      border-bottom: 2px solid transparent; transition: all 0.15s;
      display: flex; align-items: center; gap: 8px;
      &.active { color: var(--accent); border-bottom-color: var(--accent); }
      &:hover { color: var(--text-primary); }
    }
    .tab-count {
      background: var(--bg-secondary); color: var(--text-muted);
      padding: 2px 8px; border-radius: 10px;
      font-size: 11px; font-weight: 700;
    }
    .tab.active .tab-count { background: rgba(226, 55, 68, 0.15); color: var(--accent); }

    .empty-state {
      text-align: center; padding: 80px 20px; color: var(--text-muted);
      .empty-icon { font-size: 48px; display: block; margin-bottom: 16px; }
      p { margin-bottom: 24px; }
      &.small { padding: 40px 20px; }
    }
    .bookings-list { display: flex; flex-direction: column; gap: 16px; }
    .booking-card { padding: 24px; }
    .booking-main { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
    .booking-info {
      flex: 1; min-width: 0;
      h3 { font-size: 18px; font-weight: 600; margin-bottom: 4px; }
      .meta { font-size: 13px; color: var(--text-muted); margin-bottom: 2px; }
      .tickets { font-size: 14px; color: var(--text-secondary); margin-top: 8px; }
    }
    .addons-block {
      margin-top: 12px; padding-top: 12px;
      border-top: 1px dashed var(--border);
    }
    .addons-header {
      font-size: 12px; font-weight: 700; text-transform: uppercase;
      letter-spacing: 0.5px; color: var(--text-muted); margin-bottom: 6px;
    }
    .addons-list {
      list-style: none; padding: 0; margin: 0;
      display: flex; flex-direction: column; gap: 4px;
      li {
        display: flex; justify-content: space-between;
        font-size: 13px; color: var(--text-secondary);
      }
      .addon-total { font-weight: 600; color: var(--text-primary); }
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

    .qr-block {
      margin-top: 16px; padding-top: 16px;
      border-top: 1px dashed var(--border);
    }
    .qr-header {
      font-size: 12px; font-weight: 700; text-transform: uppercase;
      letter-spacing: 0.5px; color: var(--text-muted); margin-bottom: 8px;
    }
    .qr-container {
      display: flex; flex-direction: column; align-items: center;
      background: white; border-radius: 8px; padding: 12px;
      width: fit-content; border: 1px solid var(--border);
    }
    .qr-image {
      width: 120px; height: 120px; display: block;
    }
    .qr-caption {
      margin-top: 6px; font-size: 11px; font-weight: 600;
      color: #333; letter-spacing: 0.5px; text-transform: uppercase;
    }

    .success-card {
      text-align: center;
      .success-icon { font-size: 48px; margin-bottom: 12px; }
      h2 { color: var(--success); }
    }
    .refund-detail-box {
      font-size: 14px; line-height: 1.5; color: var(--text-secondary);
      margin: 16px 0 24px;
      .alert {
        background: rgba(52, 152, 219, 0.1); border: 1px solid rgba(52, 152, 219, 0.2);
        color: var(--text-primary); border-radius: var(--radius-sm);
        padding: 10px 12px; margin-top: 14px; font-size: 13px; text-align: left;
      }
    }

    /* Modal */
    .modal-backdrop {
      position: fixed; inset: 0; z-index: 200;
      background: rgba(0, 0, 0, 0.65);
      display: flex; align-items: center; justify-content: center;
      padding: 16px;
    }
    .modal-card {
      width: 100%; max-width: 480px;
      background: var(--bg-card); border: 1px solid var(--border);
      border-radius: var(--radius); padding: 24px 24px 20px;
      box-shadow: 0 24px 60px rgba(0,0,0,0.5);
      h2 { font-size: 20px; font-weight: 700; margin-bottom: 4px; }
      .modal-sub { font-size: 13px; color: var(--text-muted); margin-bottom: 16px; }
    }
    .policy-box {
      background: var(--bg-secondary); border-radius: var(--radius-sm);
      padding: 12px 16px; margin-bottom: 16px;
      h3 { font-size: 13px; font-weight: 700; text-transform: uppercase;
           letter-spacing: 0.5px; color: var(--text-muted); margin-bottom: 8px; }
      ul { margin: 0; padding-left: 18px; font-size: 13px; color: var(--text-secondary); }
      li { margin-bottom: 4px; }
    }
    .tc-box {
      margin-bottom: 16px; font-size: 13px; color: var(--text-secondary);
      label { display: flex; gap: 8px; align-items: flex-start; cursor: pointer; }
      input { margin-top: 2px; }
    }
    .error-msg {
      background: rgba(231, 76, 60, 0.1); border: 1px solid rgba(231, 76, 60, 0.3);
      color: var(--danger); padding: 10px; border-radius: var(--radius-sm);
      font-size: 13px; margin-bottom: 12px;
    }
    .modal-actions {
      display: flex; gap: 10px; justify-content: flex-end;
      .danger { background: var(--danger); border-color: var(--danger); }
    }
  `]
})
export class MyBookingsComponent implements OnInit {
  bookings: BookingResponse[] = [];
  loading = true;
  activeTab: Tab = 'upcoming';

  cancelTarget: BookingResponse | null = null;
  acceptedTc = false;
  cancelling = false;
  cancelError = '';
  refundNotification: { amount: number } | null = null;

  constructor(private bookingService: BookingService) {}

  ngOnInit() {
    this.loadBookings();
  }

  loadBookings() {
    this.loading = true;
    this.bookingService.getMyBookings().subscribe({
      next: (b) => {
        this.bookings = (b || []).sort((x, y) =>
          new Date(y.showStartTime || y.startTime || y.createdAt).getTime() -
          new Date(x.showStartTime || x.startTime || x.createdAt).getTime()
        );
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  get upcomingBookings(): BookingResponse[] {
    const now = Date.now();
    return this.bookings.filter(b => {
      if (b.status === 'CANCELLED' || b.status === 'EXPIRED' || b.status === 'COMPLETED') return false;
      const t = new Date(b.showStartTime || b.startTime || 0).getTime();
      return t >= now;
    });
  }

  get pastBookings(): BookingResponse[] {
    const now = Date.now();
    return this.bookings.filter(b => {
      if (b.status === 'CANCELLED' || b.status === 'EXPIRED' || b.status === 'COMPLETED') return true;
      const t = new Date(b.showStartTime || b.startTime || 0).getTime();
      return t < now;
    });
  }

  get visibleBookings(): BookingResponse[] {
    return this.activeTab === 'upcoming' ? this.upcomingBookings : this.pastBookings;
  }

  canCancel(b: BookingResponse): boolean {
    if (b.status !== 'CONFIRMED' && b.status !== 'AWAITING_PAYMENT') return false;
    const t = new Date(b.showStartTime || b.startTime || 0).getTime();
    return t >= Date.now();
  }

  openCancelModal(b: BookingResponse) {
    this.cancelTarget = b;
    this.acceptedTc = false;
    this.cancelError = '';
  }

  closeCancelModal() {
    if (this.cancelling) return;
    this.cancelTarget = null;
    this.acceptedTc = false;
    this.cancelError = '';
  }

  confirmCancel() {
    if (!this.cancelTarget || !this.acceptedTc) return;
    this.cancelling = true;
    this.cancelError = '';
    const id = this.cancelTarget.bookingId;
    const isConfirmed = this.cancelTarget.status === 'CONFIRMED';
    const refundVal = this.cancelTarget.grandTotal || 0;

    this.bookingService.cancelBooking(id).subscribe({
      next: () => {
        this.cancelling = false;
        this.cancelTarget = null;
        this.acceptedTc = false;
        this.loadBookings();
        if (isConfirmed) {
          this.refundNotification = { amount: refundVal };
        }
      },
      error: (err) => {
        this.cancelling = false;
        this.cancelError = err?.error?.message || err?.error?.error ||
          'Could not cancel booking. Please try again.';
      }
    });
  }

  closeRefundNotification() {
    this.refundNotification = null;
  }
}
