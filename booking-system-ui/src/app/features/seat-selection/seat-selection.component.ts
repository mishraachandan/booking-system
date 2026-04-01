import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ShowService, ShowSeatResponse } from '../../core/services/show.service';
import { BookingService } from '../../core/services/booking.service';

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

        @if (bookingError) {
          <div class="error-msg" style="text-align:center;margin-top:16px;">{{ bookingError }}</div>
        }
      }

      <!-- Summary footer -->
      @if (selectedSeats.length > 0 && !loading) {
        <div class="summary-bar">
          <div class="summary-info">
            <span class="seat-count">{{ selectedSeats.length }} seat(s) selected</span>
            <span class="total-price">₹{{ totalPrice }}</span>
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
      display: flex; gap: 20px; justify-content: center;
      flex-wrap: wrap; margin-bottom: 20px;
      .legend-item { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--text-secondary); }
    }
    .seat-dot {
      width: 14px; height: 14px; border-radius: 4px;
      &.available { background: var(--bg-card); border: 1px solid #555; }
      &.selected { background: var(--accent); }
      &.locked { background: var(--warning); }
      &.booked { background: #333; }
      &.vip { background: rgba(241, 196, 15, 0.3); border: 2px solid #f1c40f; }
      &.premium { background: rgba(155, 89, 182, 0.3); border: 2px solid #9b59b6; }
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
      gap: 10px;
      max-width: 320px;
      margin: 0 auto 32px;
    }

    .seat {
      aspect-ratio: 1;
      display: flex; align-items: center; justify-content: center;
      font-size: 11px; font-weight: 600;
      border-radius: 8px; border: 1px solid #444;
      background: var(--bg-card); color: var(--text-secondary);
      transition: all 0.2s ease; cursor: pointer;

      &.seat-available:hover {
        border-color: var(--accent); color: var(--accent);
        transform: scale(1.12); box-shadow: 0 0 10px rgba(226, 55, 68, 0.25);
      }
      &.seat-selected {
        background: var(--accent); color: white; border-color: var(--accent);
        transform: scale(1.05);
      }
      &.seat-locked { background: rgba(243,156,18,0.15); border-color: var(--warning); color: var(--warning); cursor: not-allowed; }
      &.seat-booked { background: var(--bg-secondary); color: #555; cursor: not-allowed; opacity: 0.5; }
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
      .seat-grid { max-width: 260px; gap: 7px; }
      .show-info-bar { flex-direction: column; gap: 12px; }
      .summary-bar { padding: 14px 16px; }
    }
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

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private showService: ShowService,
    private bookingService: BookingService
  ) {}

  get totalPrice(): number {
    return this.selectedSeats.reduce((sum, s) => sum + Number(s.price), 0);
  }
  get lockMinutes(): string { return String(Math.floor(this.lockTimer / 60)).padStart(2, '0'); }
  get lockSeconds(): string { return String(this.lockTimer % 60).padStart(2, '0'); }

  ngOnInit() {
    this.showId = Number(this.route.snapshot.paramMap.get('showId'));
    this.loadSeats();
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

    this.showService.lockSeats(this.showId, seatIds).subscribe({
      next: (res) => {
        if (res.success) {
          this.startTimer(8 * 60);
          this.bookingService.bookShowSeats(this.showId, seatIds).subscribe({
            next: (booking) => {
              this.router.navigate(['/booking/summary'], {
                queryParams: { bookingId: booking.id, total: this.totalPrice }
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
