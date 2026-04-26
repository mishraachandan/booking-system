import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ShowService, ShowSeatResponse } from '../../core/services/show.service';
import { BookingService } from '../../core/services/booking.service';
import { AddOnService, AddOn, BookingAddOnLine } from '../../core/services/addon.service';

@Component({
  selector: 'app-seat-selection',
  imports: [CommonModule],
  template: `
    <div class="seat-page page-enter container">
      @if (loading) {
        <div class="loading-state">
          <div class="spinner"></div>
          <p>Loading seats...</p>
        </div>
      } @else if (errorMsg) {
        <div class="error-box">
          <p>{{ errorMsg }}</p>
          <button class="btn btn-outline" (click)="loadSeats()">Retry</button>
        </div>
      } @else {
        <!-- Show info bar -->
        <div class="show-info-bar">
          <div>
            <h1 class="show-title">{{ showInfo.movieTitle }}</h1>
            <p class="show-meta">
              {{ showInfo.cinemaName }} &bull; {{ showInfo.screenName }}
              &bull; {{ showInfo.startTime | date:'MMM d, hh:mm a' }}
            </p>
            <p class="show-tags">
              <span class="tag">{{ showInfo.movieGenre }}</span>
              <span class="tag">{{ showInfo.movieLanguage }}</span>
              <span class="tag">{{ showInfo.movieDurationMinutes }} min</span>
            </p>
          </div>
          @if (lockTimer > 0) {
            <div class="timer" [class.urgent]="lockTimer < 60">
              ⏱ {{ lockMinutes }}:{{ lockSeconds }}
            </div>
          }
        </div>

        <!-- Seat Legend -->
        <div class="legend">
          <span class="legend-item"><span class="seat-dot available"></span> Available</span>
          <span class="legend-item"><span class="seat-dot selected"></span> Selected</span>
          <span class="legend-item"><span class="seat-dot locked"></span> Locked</span>
          <span class="legend-item"><span class="seat-dot booked"></span> Booked</span>
          <span class="legend-item"><span class="seat-dot vip"></span> VIP ₹500</span>
          <span class="legend-item"><span class="seat-dot premium"></span> Premium ₹350</span>
        </div>

        <!-- Screen indicator -->
        <div class="screen-indicator">
          <div class="screen-line"></div>
          <span>SCREEN</span>
        </div>

        <!-- Seat Grid -->
        <div class="seat-grid">
          @for (seat of seats; track seat.showSeatId) {
            <button
              class="seat"
              [class.seat-available]="seat.status === 'AVAILABLE' && !isSelected(seat)"
              [class.seat-selected]="isSelected(seat)"
              [class.seat-locked]="seat.status === 'LOCKED' && !isSelected(seat)"
              [class.seat-booked]="seat.status === 'BOOKED'"
              [class.seat-premium]="seat.seatType === 'PREMIUM'"
              [class.seat-vip]="seat.seatType === 'VIP'"
              [disabled]="seat.status === 'BOOKED' || (seat.status === 'LOCKED' && !isSelected(seat))"
              (click)="toggleSeat(seat)"
              [title]="seat.seatNumber + ' (' + seat.seatType + ') - ₹' + seat.price">
              {{ seat.seatNumber }}
            </button>
          }
        </div>

        <!-- Add-ons picker -->
        @if (selectedSeats.length > 0 && addOns.length > 0) {
          <div class="addons-section">
            <div class="addons-head" (click)="addOnsOpen = !addOnsOpen">
              <div>
                <h2>🍿 Add food &amp; beverages</h2>
                <p class="sub">
                  @if (addOnItems > 0) {
                    {{ addOnItems }} item(s) selected · ₹{{ addOnTotal }}
                  } @else {
                    Optional · snacks, drinks and combos
                  }
                </p>
              </div>
              <span class="chev" [class.open]="addOnsOpen">▾</span>
            </div>

            @if (addOnsOpen) {
              <div class="addons-grid">
                @for (addOn of addOns; track addOn.id) {
                  <div class="addon-card">
                    <div class="addon-image-wrap">
                      <img [src]="addOn.imageUrl" [alt]="addOn.name" class="addon-image" />
                      <span class="addon-chip" [class]="addOn.category.toLowerCase()">
                        {{ addOn.category }}
                      </span>
                    </div>
                    <div class="addon-body">
                      <div class="addon-name">{{ addOn.name }}</div>
                      <div class="addon-desc">{{ addOn.description }}</div>
                      <div class="addon-footer">
                        <span class="addon-price">₹{{ addOn.price }}</span>
                        <div class="qty-stepper">
                          <button
                            class="qty-btn"
                            (click)="decrementAddOn(addOn)"
                            [disabled]="(addOnQuantities[addOn.id] || 0) === 0"
                            aria-label="Decrease">−</button>
                          <span class="qty-val">{{ addOnQuantities[addOn.id] || 0 }}</span>
                          <button
                            class="qty-btn"
                            (click)="incrementAddOn(addOn)"
                            [disabled]="(addOnQuantities[addOn.id] || 0) >= 20"
                            aria-label="Increase">+</button>
                        </div>
                      </div>
                    </div>
                  </div>
                }
              </div>
            }
          </div>
        }

        @if (bookingError) {
          <div class="error-msg" style="text-align:center;margin-top:16px;">{{ bookingError }}</div>
        }
      }

      <!-- Summary footer -->
      @if (selectedSeats.length > 0 && !loading) {
        <div class="summary-bar">
          <div class="summary-info">
            <span class="seat-count">
              {{ selectedSeats.length }} seat(s){{ addOnItems > 0 ? ' · ' + addOnItems + ' add-on(s)' : '' }}
            </span>
            <span class="total-price">₹{{ grandTotal }}</span>
          </div>
          <button class="btn btn-primary" (click)="proceedToBooking()" [disabled]="bookingLoading">
            {{ bookingLoading ? 'Booking...' : 'Book Now →' }}
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .seat-page { padding-top: 20px; padding-bottom: 100px; }

    .loading-state {
      text-align: center; padding: 120px 0; color: var(--text-muted);
      p { margin-top: 16px; font-size: 16px; }
    }
    .spinner {
      width: 48px; height: 48px; border: 3px solid var(--border);
      border-top-color: var(--accent); border-radius: 50%;
      animation: spin 0.8s linear infinite; margin: 0 auto;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .error-box {
      text-align: center; padding: 80px 20px; color: var(--danger);
      button { margin-top: 16px; }
    }

    .show-info-bar {
      display: flex; justify-content: space-between; align-items: flex-start;
      margin-bottom: 24px; padding: 20px 24px; background: var(--bg-card);
      border-radius: var(--radius); border: 1px solid var(--border);
    }
    .show-title { font-size: 24px; font-weight: 700; margin-bottom: 4px; }
    .show-meta { color: var(--text-secondary); font-size: 14px; margin-bottom: 8px; }
    .show-tags { display: flex; gap: 8px; flex-wrap: wrap; }
    .tag {
      background: rgba(226,55,68,0.12); color: var(--accent);
      padding: 2px 10px; border-radius: 20px; font-size: 12px; font-weight: 500;
    }

    .timer {
      font-size: 28px; font-weight: 700; font-family: 'Outfit', monospace;
      color: var(--success); padding: 8px 20px;
      background: rgba(46, 204, 113, 0.1); border-radius: var(--radius-sm);
      white-space: nowrap;
      &.urgent { color: var(--danger); background: rgba(231, 76, 60, 0.1); }
    }

    .legend {
      display: flex; gap: 24px; justify-content: center;
      flex-wrap: wrap; margin-bottom: 24px;
      padding: 14px 20px; background: var(--bg-card);
      border-radius: var(--radius-sm); border: 1px solid var(--border);
      .legend-item { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--text-secondary); font-weight: 500; }
    }
    .seat-dot {
      width: 18px; height: 18px; border-radius: 4px;
      &.available { background: #1a6b3c; border: 2px solid #2ecc71; }
      &.selected { background: #e23744; border: 2px solid #ff4757; box-shadow: 0 0 6px rgba(226, 55, 68, 0.5); }
      &.locked { background: #c0760e; border: 2px solid #f39c12; }
      &.booked { background: #2c2c3a; border: 2px solid #555; position: relative; }
      &.booked::after {
        content: '✕'; position: absolute; color: #666; font-size: 10px;
        display: flex; align-items: center; justify-content: center;
        width: 100%; height: 100%;
      }
      &.vip { background: rgba(241, 196, 15, 0.25); border: 2px solid #f1c40f; }
      &.premium { background: rgba(155, 89, 182, 0.25); border: 2px solid #9b59b6; }
    }

    .screen-indicator {
      text-align: center; margin-bottom: 32px;
      span { font-size: 11px; color: var(--text-muted); letter-spacing: 4px; display: block; margin-top: 6px; }
      .screen-line {
        width: 55%; max-width: 440px; height: 4px; margin: 0 auto;
        background: var(--accent-gradient); border-radius: 2px;
        box-shadow: 0 0 20px rgba(226, 55, 68, 0.4);
      }
    }

    .seat-grid {
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: 12px;
      max-width: 360px;
      margin: 0 auto 32px;
    }

    .seat {
      aspect-ratio: 1;
      display: flex; align-items: center; justify-content: center;
      font-size: 12px; font-weight: 700; letter-spacing: 0.5px;
      border-radius: 8px; border: 2px solid transparent;
      transition: all 0.2s ease; cursor: pointer;
      position: relative;

      /* DEFAULT: Available — bright green border, dark green tint */
      &.seat-available {
        background: rgba(46, 204, 113, 0.08);
        border-color: #2ecc71;
        color: #2ecc71;
      }
      &.seat-available:hover {
        background: rgba(46, 204, 113, 0.2);
        transform: scale(1.12);
        box-shadow: 0 0 14px rgba(46, 204, 113, 0.4);
      }

      /* SELECTED — bright red/accent fill */
      &.seat-selected {
        background: var(--accent); color: white; border-color: #ff4757;
        transform: scale(1.08);
        box-shadow: 0 0 16px rgba(226, 55, 68, 0.5);
      }

      /* LOCKED — orange border+tint */
      &.seat-locked {
        background: rgba(243, 156, 18, 0.12);
        border-color: #f39c12; color: #f39c12;
        cursor: not-allowed;
      }

      /* BOOKED — muted dark, slashed */
      &.seat-booked {
        background: #1e1e2e; color: #444; cursor: not-allowed;
        border-color: #333; opacity: 0.6;
      }
      &.seat-booked::after {
        content: '';
        position: absolute; inset: 4px;
        border-radius: 4px;
        background: repeating-linear-gradient(
          -45deg, transparent, transparent 3px, rgba(100,100,100,0.15) 3px, rgba(100,100,100,0.15) 5px
        );
      }

      /* Type accents — bottom strip */
      &.seat-premium { border-bottom: 3px solid #9b59b6; }
      &.seat-vip { border-bottom: 3px solid #f1c40f; }
    }

    .summary-bar {
      position: fixed; bottom: 0; left: 0; right: 0;
      display: flex; align-items: center; justify-content: space-between;
      padding: 14px 32px;
      background: rgba(26, 26, 46, 0.97); backdrop-filter: blur(20px);
      border-top: 1px solid var(--border); z-index: 100;
    }
    .summary-info { display: flex; align-items: center; gap: 20px; }
    .seat-count { font-size: 14px; color: var(--text-secondary); }
    .total-price { font-size: 26px; font-weight: 700; }

    .error-msg {
      background: rgba(231, 76, 60, 0.1); border: 1px solid rgba(231, 76, 60, 0.3);
      color: var(--danger); padding: 12px 16px; border-radius: var(--radius-sm);
      font-size: 13px; max-width: 500px; margin: 16px auto;
    }

    @media (max-width: 600px) {
      .seat-grid { max-width: 280px; gap: 8px; }
      .show-info-bar { flex-direction: column; gap: 12px; }
      .summary-bar { padding: 14px 16px; }
    }

    /* Add-ons picker */
    .addons-section {
      margin-top: 32px; background: var(--bg-card);
      border: 1px solid var(--border); border-radius: var(--radius);
      overflow: hidden;
    }
    .addons-head {
      display: flex; justify-content: space-between; align-items: center;
      padding: 16px 20px; cursor: pointer;
      h2 { font-size: 16px; font-weight: 700; margin: 0; }
      .sub { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
      &:hover { background: var(--bg-secondary); }
    }
    .chev {
      font-size: 20px; color: var(--text-muted);
      transition: transform 0.2s;
      &.open { transform: rotate(180deg); }
    }
    .addons-grid {
      padding: 16px 20px 20px;
      display: grid; gap: 12px;
      grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
      border-top: 1px solid var(--border);
    }
    .addon-card {
      background: var(--bg-secondary); border: 1px solid var(--border);
      border-radius: var(--radius-sm); overflow: hidden;
      display: flex; flex-direction: column;
    }
    .addon-image-wrap { position: relative; aspect-ratio: 4 / 3; background: #0a0b14; }
    .addon-image { width: 100%; height: 100%; object-fit: cover; display: block; }
    .addon-chip {
      position: absolute; top: 8px; left: 8px;
      font-size: 10px; font-weight: 700; letter-spacing: 0.5px;
      padding: 3px 8px; border-radius: 10px; text-transform: uppercase;
      &.food { background: rgba(230, 126, 34, 0.9); color: white; }
      &.beverage { background: rgba(52, 152, 219, 0.9); color: white; }
      &.combo { background: rgba(226, 55, 68, 0.9); color: white; }
    }
    .addon-body {
      flex: 1; padding: 12px 14px;
      display: flex; flex-direction: column; gap: 6px;
    }
    .addon-name { font-weight: 600; font-size: 14px; }
    .addon-desc { font-size: 12px; color: var(--text-muted); flex: 1; line-height: 1.4; }
    .addon-footer {
      display: flex; justify-content: space-between; align-items: center;
      margin-top: 4px;
    }
    .addon-price { font-weight: 700; color: var(--accent); }
    .qty-stepper {
      display: inline-flex; align-items: center;
      border: 1px solid var(--border); border-radius: 999px;
      background: var(--bg-card);
    }
    .qty-btn {
      width: 28px; height: 28px; border: none; background: none;
      color: var(--text-primary); font-size: 16px; cursor: pointer;
      display: flex; align-items: center; justify-content: center;
      &:disabled { opacity: 0.35; cursor: not-allowed; }
      &:hover:not(:disabled) { color: var(--accent); }
    }
    .qty-val { min-width: 20px; text-align: center; font-weight: 600; font-size: 13px; }
  `]
})
export class SeatSelectionComponent implements OnInit, OnDestroy {
  seats: ShowSeatResponse[] = [];
  selectedSeats: ShowSeatResponse[] = [];
  showInfo: Partial<ShowSeatResponse> = {};
  loading = true;
  bookingLoading = false;
  errorMsg = '';
  bookingError = '';
  showId = 0;
  lockTimer = 0;
  private timerInterval: any;

  // Add-ons state
  addOns: AddOn[] = [];
  addOnQuantities: Record<number, number> = {};
  addOnsOpen = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private showService: ShowService,
    private bookingService: BookingService,
    private addOnService: AddOnService
  ) {}

  get totalPrice(): number {
    return this.selectedSeats.reduce((sum, s) => sum + Number(s.price), 0);
  }
  get addOnTotal(): number {
    return this.addOns.reduce((sum, a) => {
      const qty = this.addOnQuantities[a.id] || 0;
      return sum + Number(a.price) * qty;
    }, 0);
  }
  get addOnItems(): number {
    return Object.values(this.addOnQuantities).reduce((a, b) => a + (b || 0), 0);
  }
  get grandTotal(): number { return this.totalPrice + this.addOnTotal; }

  get lockMinutes(): string { return String(Math.floor(this.lockTimer / 60)).padStart(2, '0'); }
  get lockSeconds(): string { return String(this.lockTimer % 60).padStart(2, '0'); }

  ngOnInit() {
    this.showId = Number(this.route.snapshot.paramMap.get('showId'));
    this.loadSeats();
    this.addOnService.getAvailableAddOns().subscribe({
      next: (items) => { this.addOns = items; },
      error: () => { this.addOns = []; }
    });
  }

  incrementAddOn(a: AddOn) {
    const cur = this.addOnQuantities[a.id] || 0;
    if (cur < 20) this.addOnQuantities[a.id] = cur + 1;
  }

  decrementAddOn(a: AddOn) {
    const cur = this.addOnQuantities[a.id] || 0;
    if (cur > 0) this.addOnQuantities[a.id] = cur - 1;
  }

  private collectAddOnLines(): BookingAddOnLine[] {
    return Object.entries(this.addOnQuantities)
      .filter(([, qty]) => (qty || 0) > 0)
      .map(([id, qty]) => ({ addOnId: Number(id), quantity: qty as number }));
  }

  ngOnDestroy() { clearInterval(this.timerInterval); }

  loadSeats() {
    this.loading = true;
    this.errorMsg = '';
    this.showService.getShowSeats(this.showId).subscribe({
      next: (seats) => {
        this.seats = seats;
        if (seats.length > 0) {
          const first = seats[0];
          this.showInfo = {
            movieTitle: first.movieTitle,
            cinemaName: first.cinemaName,
            screenName: first.screenName,
            startTime: first.startTime,
            movieGenre: first.movieGenre,
            movieLanguage: first.movieLanguage,
            movieDurationMinutes: first.movieDurationMinutes
          };
        }
        this.loading = false;
      },
      error: (err) => {
        this.errorMsg = 'Failed to load seats. Please try again.';
        this.loading = false;
        console.error('Seat load error', err);
      }
    });
  }

  isSelected(seat: ShowSeatResponse): boolean {
    return this.selectedSeats.some(s => s.showSeatId === seat.showSeatId);
  }

  toggleSeat(seat: ShowSeatResponse) {
    if (seat.status === 'BOOKED' || (seat.status === 'LOCKED' && !this.isSelected(seat))) return;
    if (this.isSelected(seat)) {
      this.selectedSeats = this.selectedSeats.filter(s => s.showSeatId !== seat.showSeatId);
    } else if (this.selectedSeats.length < 10) {
      this.selectedSeats.push(seat);
    }
  }

  proceedToBooking() {
    this.bookingLoading = true;
    this.bookingError = '';
    const seatIds = this.selectedSeats.map(s => s.showSeatId);
    const addOnLines = this.collectAddOnLines();

    this.showService.lockSeats(this.showId, seatIds).subscribe({
      next: (res) => {
        if (res.success) {
          this.startTimer(8 * 60);
          this.bookingService.bookShowSeats(this.showId, seatIds, undefined, addOnLines).subscribe({
            next: (booking) => {
              this.router.navigate(['/booking/summary'], {
                queryParams: {
                  bookingId: booking.bookingId,
                  total: this.grandTotal,
                  seatTotal: this.totalPrice,
                  addOnTotal: this.addOnTotal,
                  seats: this.selectedSeats.length
                }
              });
            },
            error: (err) => {
              this.bookingError = err?.error?.message || 'Booking failed. Please try again.';
              this.bookingLoading = false;
            }
          });
        } else {
          this.bookingError = res.message;
          this.bookingLoading = false;
        }
      },
      error: (err) => {
        this.bookingError = err?.error?.message || 'Failed to lock seats. Please try again.';
        this.bookingLoading = false;
      }
    });
  }

  private startTimer(seconds: number) {
    this.lockTimer = seconds;
    this.timerInterval = setInterval(() => {
      this.lockTimer--;
      if (this.lockTimer <= 0) {
        clearInterval(this.timerInterval);
        this.bookingError = 'Seat lock expired. Please re-select your seats.';
        this.selectedSeats = [];
        this.loadSeats();
      }
    }, 1000);
  }
}
