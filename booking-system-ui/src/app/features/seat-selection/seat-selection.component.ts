import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ShowService, ShowSeatResponse } from '../../core/services/show.service';
import { BookingService } from '../../core/services/booking.service';
import { AddOnService, AddOn, BookingAddOnLine } from '../../core/services/addon.service';
import { AuthService } from '../../core/services/auth.service';

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

        <!-- Curved Cinematic Screen -->
        <div class="cinema-screen-wrap">
          <div class="cinema-screen"></div>
          <span class="cinema-screen-text">SCREEN</span>
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
    .seat-page { padding-top: 20px; padding-bottom: 120px; }

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
      margin-bottom: 24px; padding: 24px; background: var(--bg-card);
      border-radius: var(--radius); border: 1px solid var(--border);
    }
    .show-title { font-size: 26px; font-weight: 800; margin-bottom: 4px; }
    .show-meta { color: var(--text-secondary); font-size: 14px; margin-bottom: 12px; }
    .show-tags { display: flex; gap: 8px; flex-wrap: wrap; }
    .tag {
      background: rgba(244, 63, 94, 0.08); color: var(--accent);
      padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600;
      border: 1px solid rgba(244, 63, 94, 0.15);
    }

    .timer {
      font-size: 28px; font-weight: 700; font-family: var(--font-mono);
      color: var(--success); padding: 10px 24px;
      background: rgba(16, 185, 129, 0.08); border-radius: var(--radius-sm);
      border: 1px solid rgba(16, 185, 129, 0.15);
      white-space: nowrap;
      animation: pulseSuccess 2s infinite;
      &.urgent {
        color: var(--danger);
        background: rgba(239, 68, 68, 0.08);
        border-color: rgba(239, 68, 68, 0.15);
        animation: pulseGlow 1s infinite;
      }
    }

    .legend {
      display: flex; gap: 24px; justify-content: center;
      flex-wrap: wrap; margin-bottom: 32px;
      padding: 16px 24px; background: var(--bg-card);
      border-radius: var(--radius-sm); border: 1px solid var(--border);
      .legend-item { display: flex; align-items: center; gap: 10px; font-size: 13px; color: var(--text-secondary); font-weight: 600; }
    }
    .seat-dot {
      width: 20px; height: 20px; border-radius: 6px;
      &.available { background: rgba(16, 185, 129, 0.1); border: 2px solid var(--success); }
      &.selected { background: var(--accent); border: 2px solid var(--accent-hover); box-shadow: 0 0 10px var(--accent-glow); }
      &.locked { background: rgba(245, 158, 11, 0.1); border: 2px solid var(--warning); }
      &.booked { background: #1e1e2e; border: 2px solid #333; position: relative; opacity: 0.5; }
      &.booked::after {
        content: '✕'; position: absolute; color: #555; font-size: 10px;
        display: flex; align-items: center; justify-content: center;
        width: 100%; height: 100%; top: 0; left: 0; font-weight: bold;
      }
      &.vip { background: rgba(245, 158, 11, 0.2); border: 2px solid #f59e0b; }
      &.premium { background: rgba(139, 92, 246, 0.2); border: 2px solid #8b5cf6; }
    }

    .seat-grid {
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: 14px;
      max-width: 380px;
      margin: 0 auto 48px;
    }

    .seat {
      aspect-ratio: 1;
      display: flex; align-items: center; justify-content: center;
      font-size: 13px; font-weight: 700; letter-spacing: 0.5px;
      border-radius: 8px; border: 2px solid transparent;
      transition: var(--transition); cursor: pointer;
      position: relative;
      background: var(--bg-secondary);
      color: var(--text-secondary);

      &.seat-available {
        background: rgba(16, 185, 129, 0.05);
        border-color: var(--success);
        color: var(--success);
      }
      &.seat-available:hover {
        background: rgba(16, 185, 129, 0.15);
        transform: scale(1.15);
        box-shadow: 0 0 12px rgba(16, 185, 129, 0.3);
      }

      &.seat-selected {
        background: var(--accent); color: white; border-color: var(--accent-hover);
        transform: scale(1.1);
        box-shadow: 0 0 16px var(--accent-glow);
        animation: pulseGlow 2s infinite;
      }

      &.seat-locked {
        background: rgba(245, 158, 11, 0.08);
        border-color: var(--warning); color: var(--warning);
        cursor: not-allowed;
      }

      &.seat-booked {
        background: var(--bg-primary); color: #3f3f56; cursor: not-allowed;
        border-color: var(--border); opacity: 0.4;
      }
      &.seat-booked::after {
        content: '';
        position: absolute; inset: 4px;
        border-radius: 4px;
        background: repeating-linear-gradient(
          -45deg, transparent, transparent 3px, rgba(100,100,100,0.1) 3px, rgba(100,100,100,0.1) 5px
        );
      }

      &.seat-premium { border-bottom: 3px solid #8b5cf6; }
      &.seat-vip { border-bottom: 3px solid #f59e0b; }
    }

    .summary-bar {
      position: fixed; bottom: 0; left: 0; right: 0;
      display: flex; align-items: center; justify-content: space-between;
      padding: 18px 36px;
      background: var(--glass-bg); backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      border-top: 1px solid var(--glass-border); z-index: 100;
      box-shadow: 0 -8px 30px rgba(0,0,0,0.3);
    }
    .summary-info { display: flex; align-items: center; gap: 24px; }
    .seat-count { font-size: 15px; color: var(--text-secondary); font-weight: 500; }
    .total-price { font-size: 28px; font-weight: 800; color: var(--accent); }

    .error-msg {
      background: rgba(239, 68, 68, 0.06); border: 1px solid rgba(239, 68, 68, 0.2);
      color: var(--danger); padding: 14px 20px; border-radius: var(--radius-sm);
      font-size: 14px; max-width: 540px; margin: 24px auto; text-align: center;
      font-weight: 500;
    }

    @media (max-width: 600px) {
      .seat-grid { max-width: 300px; gap: 8px; }
      .show-info-bar { flex-direction: column; gap: 16px; }
      .summary-bar { padding: 16px 20px; }
    }

    /* Add-ons picker */
    .addons-section {
      margin-top: 40px; background: var(--bg-card);
      border: 1px solid var(--border); border-radius: var(--radius);
      overflow: hidden;
      box-shadow: var(--shadow);
    }
    .addons-head {
      display: flex; justify-content: space-between; align-items: center;
      padding: 20px 24px; cursor: pointer;
      h2 { font-size: 18px; font-weight: 700; margin: 0; }
      .sub { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
      &:hover { background: var(--bg-secondary); }
    }
    .chev {
      font-size: 20px; color: var(--text-muted);
      transition: var(--transition);
      &.open { transform: rotate(180deg); }
    }
    .addons-grid {
      padding: 24px;
      display: grid; gap: 20px;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      border-top: 1px solid var(--border);
      background: rgba(0, 0, 0, 0.05);
    }
    .addon-card {
      background: var(--bg-secondary); border: 1px solid var(--border);
      border-radius: var(--radius-sm); overflow: hidden;
      display: flex; flex-direction: column;
      transition: var(--transition);
      &:hover {
        border-color: var(--border-hover);
        transform: translateY(-2px);
      }
    }
    .addon-image-wrap { position: relative; aspect-ratio: 4 / 3; background: #000; }
    .addon-image { width: 100%; height: 100%; object-fit: cover; display: block; }
    .addon-chip {
      position: absolute; top: 10px; left: 10px;
      font-size: 10px; font-weight: 700; letter-spacing: 0.8px;
      padding: 4px 10px; border-radius: 20px; text-transform: uppercase;
      &.food { background: rgba(245, 158, 11, 0.95); color: white; }
      &.beverage { background: rgba(59, 130, 246, 0.95); color: white; }
      &.combo { background: rgba(244, 63, 94, 0.95); color: white; }
    }
    .addon-body {
      flex: 1; padding: 16px;
      display: flex; flex-direction: column; gap: 8px;
    }
    .addon-name { font-weight: 700; font-size: 15px; color: var(--text-primary); }
    .addon-desc { font-size: 12px; color: var(--text-secondary); flex: 1; line-height: 1.5; }
    .addon-footer {
      display: flex; justify-content: space-between; align-items: center;
      margin-top: 8px;
    }
    .addon-price { font-weight: 800; color: var(--accent); font-size: 16px; }
    .qty-stepper {
      display: inline-flex; align-items: center;
      border: 1px solid var(--border); border-radius: 999px;
      background: var(--bg-card);
      padding: 2px;
    }
    .qty-btn {
      width: 30px; height: 30px; border: none; background: none;
      color: var(--text-primary); font-size: 16px; cursor: pointer;
      display: flex; align-items: center; justify-content: center;
      border-radius: 50%;
      transition: var(--transition);
      &:disabled { opacity: 0.35; cursor: not-allowed; }
      &:hover:not(:disabled) {
        color: var(--accent);
        background: var(--bg-secondary);
      }
    }
    .qty-val { min-width: 24px; text-align: center; font-weight: 700; font-size: 14px; color: var(--text-primary); }
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
  private ws: WebSocket | null = null;
  currentUserId: number | null = null;

  // Add-ons state
  addOns: AddOn[] = [];
  addOnQuantities: Record<number, number> = {};
  addOnsOpen = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private showService: ShowService,
    private bookingService: BookingService,
    private addOnService: AddOnService,
    private authService: AuthService
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
    this.currentUserId = this.authService.getUserId();
    this.loadSeats();
    this.addOnService.getAvailableAddOns().subscribe({
      next: (items) => { this.addOns = items; },
      error: () => { this.addOns = []; }
    });
    this.connectWebSocket();
  }

  connectWebSocket() {
    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const wsHost = window.location.host;
      this.ws = new WebSocket(`${protocol}//${wsHost}/ws/seats`);

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          if (data && Number(data.showId) === this.showId) {
            const seat = this.seats.find(s => s.showSeatId === Number(data.showSeatId));
            if (seat) {
              seat.status = data.status;

              // If a seat becomes locked or booked by someone else and we had it selected, deselect it
              if (data.status !== 'AVAILABLE' && this.isSelected(seat)) {
                if (data.status === 'BOOKED' || (data.status === 'LOCKED' && data.lockedByUserId !== this.currentUserId)) {
                  this.selectedSeats = this.selectedSeats.filter(s => s.showSeatId !== seat.showSeatId);
                }
              }
            }
          }
        } catch (e) {
          console.warn('Error parsing seat update WebSocket message:', e);
        }
      };

      this.ws.onclose = () => {
        console.log('Seat WebSocket connection closed. Reconnecting in 3s...');
        setTimeout(() => {
          if (!this.ws || this.ws.readyState === WebSocket.CLOSED) {
            this.connectWebSocket();
          }
        }, 3000);
      };

      this.ws.onerror = (err) => {
        console.warn('Seat WebSocket error:', err);
      };
    } catch (e) {
      console.warn('Could not establish WebSocket connection:', e);
    }
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

  ngOnDestroy() {
    clearInterval(this.timerInterval);
    if (this.ws) {
      this.ws.close();
    }
  }

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
