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

      <!-- Search & Filters -->
      @if (!loading && bookings.length > 0) {
        <div class="search-filters" style="margin-bottom: 24px;">
          <div class="search-input-wrap" style="margin-bottom: 0;">
            <span class="search-icon">🔍</span>
            <input 
              type="text" 
              [(ngModel)]="bookingSearchTerm" 
              placeholder="Search bookings by movie name or booking ID..." 
              aria-label="Search bookings" />
          </div>
        </div>

        <!-- Tabs -->
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
        @if (filteredVisibleBookings.length === 0) {
          <div class="empty-state small">
            <p>No matching {{ activeTab === 'upcoming' ? 'upcoming' : 'past' }} bookings found.</p>
          </div>
        }
        <div class="bookings-list">
          @for (booking of filteredVisibleBookings; track booking.bookingId) {
            @if (booking.status === 'CONFIRMED') {
              <!-- Premium Boarding Pass E-Ticket -->
              <div [id]="'booking-card-' + booking.bookingId" class="boarding-pass booking-card">
                <div class="ticket-header">
                  <div class="ticket-logo">Book<span>My</span>Show</div>
                  <div class="ticket-type">E-Ticket</div>
                </div>
                <div class="ticket-body">
                  <div class="ticket-movie-title">{{ booking.movieTitle || booking.resourceName || 'Event Booking' }}</div>
                  
                  <div class="ticket-content-wrap" style="display: flex; gap: 24px; align-items: flex-start; justify-content: space-between;">
                    <div class="ticket-grid" style="flex: 1; display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-bottom: 0;">
                      <div class="ticket-field">
                        <label>Cinema / Venue</label>
                        <span>{{ booking.cinemaName || 'Standard Screen' }}</span>
                      </div>
                      <div class="ticket-field">
                        <label>Screen</label>
                        <span>{{ booking.screenName || 'Screen 1' }}</span>
                      </div>
                      <div class="ticket-field">
                        <label>Date &amp; Time</label>
                        <span>{{ booking.showStartTime || booking.startTime | date:'MMM d, yyyy · hh:mm a' }}</span>
                      </div>
                      <div class="ticket-field">
                        <label>Seats</label>
                        <span>{{ booking.numberOfTickets }} ticket{{ booking.numberOfTickets > 1 ? 's' : '' }}</span>
                      </div>
                    </div>

                    <div class="ticket-poster-wrap">
                      <img [src]="getMoviePoster(booking.movieTitle || booking.resourceName)" 
                           [alt]="booking.movieTitle || booking.resourceName" 
                           class="ticket-poster" />
                    </div>
                  </div>

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
                </div>

                <div class="ticket-divider"></div>

                <div class="ticket-footer">
                  <div class="qr-side">
                    <img [src]="'https://api.qrserver.com/v1/create-qr-code/?size=100x100&data=BOOKING-' + booking.bookingId" 
                         alt="Check-in QR Code" />
                  </div>
                  <div class="barcode-side">
                    <div class="barcode-stripes"></div>
                    <span class="barcode-num">BMS{{ booking.bookingId }}</span>
                  </div>
                </div>

                <div class="booking-actions" style="margin-top: 0; padding: 16px 28px; background: rgba(0,0,0,0.02); border-top: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center;">
                  <button class="btn btn-ghost" style="padding: 8px 12px; font-size: 13px;" (click)="shareBooking(booking)">
                    🔗 Share Details
                  </button>
                  <div style="display: flex; gap: 8px;">
                    <button class="btn btn-ghost danger" style="padding: 8px 12px; font-size: 13px;" *ngIf="canCancel(booking)" (click)="openCancelModal(booking)">
                      Cancel
                    </button>
                    <button class="btn btn-primary" style="padding: 8px 16px; font-size: 13px;" (click)="printTicket(booking)">
                      🖨️ Print E-Ticket
                    </button>
                  </div>
                </div>
              </div>
            } @else {
              <!-- Standard Booking Card for non-confirmed states -->
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
    .tab.active .tab-count { background: rgba(244, 63, 94, 0.15); color: var(--accent); }

    .empty-state {
      text-align: center; padding: 80px 20px; color: var(--text-muted);
      .empty-icon { font-size: 48px; display: block; margin-bottom: 16px; }
      p { margin-bottom: 24px; }
      &.small { padding: 40px 20px; }
    }
    .bookings-list { display: flex; flex-direction: column; gap: 24px; }
    .booking-card { padding: 24px; }
    .booking-main { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
    .booking-info {
      flex: 1; min-width: 0;
      h3 { font-size: 18px; font-weight: 600; margin-bottom: 4px; }
      .meta { font-size: 13px; color: var(--text-secondary); margin-bottom: 2px; }
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
      &.confirmed { background: rgba(16, 185, 129, 0.15); color: var(--success); }
      &.awaiting_payment { background: rgba(245, 158, 11, 0.15); color: var(--warning); }
      &.cancelled { background: rgba(239, 68, 68, 0.15); color: var(--danger); }
      &.pending { background: rgba(148, 163, 184, 0.15); color: var(--text-muted); }
      &.expired { background: rgba(148, 163, 184, 0.15); color: var(--text-muted); }
      &.completed { background: rgba(59, 130, 246, 0.15); color: #3b82f6; }
    }
    .booking-actions {
      border-top: 1px solid var(--border); margin-top: 16px; padding-top: 12px;
      text-align: right;
      .danger { color: var(--danger); &:hover { color: var(--danger); background: rgba(239, 68, 68, 0.1); } }
    }

    .ticket-poster-wrap {
      flex: 0 0 80px;
      width: 80px;
      aspect-ratio: 2/3;
      border-radius: var(--radius-sm);
      overflow: hidden;
      border: 1px solid var(--border);
      box-shadow: var(--shadow-sm);
      background: var(--bg-secondary);
      transition: all 0.3s ease;
      
      &:hover {
        transform: scale(1.05);
        border-color: var(--accent);
        box-shadow: 0 5px 15px var(--accent-glow);
      }
    }
    .ticket-poster {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s ease;
    }
    .ticket-poster-wrap:hover .ticket-poster {
      transform: scale(1.1);
    }

    @media (max-width: 600px) {
      .ticket-content-wrap {
        flex-direction: column-reverse;
        gap: 16px;
      }
      .ticket-poster-wrap {
        align-self: center;
        width: 120px;
        flex: 0 0 120px;
      }
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
        background: rgba(59, 130, 246, 0.08); border: 1px solid rgba(59, 130, 246, 0.15);
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
      background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.2);
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

  bookingSearchTerm = '';

  cancelTarget: BookingResponse | null = null;
  acceptedTc = false;
  cancelling = false;
  cancelError = '';
  refundNotification: { amount: number } | null = null;

  getMoviePoster(title: string | null): string {
    if (!title) return 'https://placehold.co/300x450/1a1a2e/e23744?text=Movie';
    const cleanTitle = title.trim();
    if (cleanTitle.includes('Dark Knight')) return '/posters/the-dark-knight-returns.jpg';
    if (cleanTitle.includes('Pushpa')) return '/posters/pushpa-3.jpg';
    if (cleanTitle.includes('Jawan')) return '/posters/jawan-2.jpg';
    if (cleanTitle.includes('RRR')) return '/posters/rrr-rise-again.jpg';
    if (cleanTitle.includes('Inception')) return '/posters/inception-2.jpg';
    if (cleanTitle.includes('Stree')) return '/posters/stree-3.jpg';
    if (cleanTitle.includes('KGF') || cleanTitle.includes('K.G.F')) return '/posters/kgf-chapter-3.jpg';
    if (cleanTitle.includes('Animal')) return '/posters/animal-park.jpg';
    return `https://placehold.co/300x450/1a1a2e/e23744?text=${encodeURIComponent(cleanTitle)}`;
  }

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

  get filteredVisibleBookings(): BookingResponse[] {
    let list = this.visibleBookings;
    if (this.bookingSearchTerm) {
      const term = this.bookingSearchTerm.toLowerCase().trim();
      list = list.filter(b => 
        (b.movieTitle && b.movieTitle.toLowerCase().includes(term)) ||
        (b.resourceName && b.resourceName.toLowerCase().includes(term)) ||
        (String(b.bookingId).includes(term))
      );
    }
    return list;
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

  printTicket(booking: BookingResponse) {
    const cardElement = document.getElementById(`booking-card-${booking.bookingId}`);
    if (cardElement) {
      cardElement.classList.add('printing-mode');
      window.print();
      setTimeout(() => {
        cardElement.classList.remove('printing-mode');
      }, 1000);
    }
  }

  shareBooking(b: BookingResponse) {
    const movie = b.movieTitle || b.resourceName || 'Movie';
    const date = new Date(b.showStartTime || b.startTime || 0).toLocaleString();
    const shareText = `Hey! I just booked tickets for "${movie}" on ${date}. Booking ID: #${b.bookingId}. See you there!`;
    
    if (typeof navigator !== 'undefined' && navigator.share) {
      navigator.share({
        title: 'Movie Ticket Booking',
        text: shareText,
        url: typeof window !== 'undefined' ? window.location.origin + '/my-bookings' : ''
      }).catch(err => {
        console.log('Share failed:', err);
      });
    } else if (typeof navigator !== 'undefined' && navigator.clipboard) {
      // Fallback: Copy to clipboard
      navigator.clipboard.writeText(shareText).then(() => {
        alert('Booking details copied to clipboard!');
      }).catch(err => {
        console.error('Clipboard copy failed:', err);
      });
    }
  }
}
