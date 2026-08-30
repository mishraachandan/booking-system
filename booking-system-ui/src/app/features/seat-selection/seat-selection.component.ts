import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ShowService, ShowSeatResponse } from '../../core/services/show.service';
import { BookingService } from '../../core/services/booking.service';
import { AddOnService, AddOn, BookingAddOnLine } from '../../core/services/addon.service';
import { AuthService } from '../../core/services/auth.service';

export interface SeatRow {
  rowLetter: string;
  seats: ShowSeatResponse[];
}

export interface SeatTier {
  name: string;
  price: number;
  rows: SeatRow[];
}

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
          <div class="show-info-text">
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
          <div class="show-info-right">
            @if (lockTimer > 0) {
              <div class="timer" [class.urgent]="lockTimer < 60">
                ⏱ {{ lockMinutes }}:{{ lockSeconds }}
              </div>
            }
            @if (showInfo.moviePosterUrl) {
              <div class="show-poster-wrap">
                <img [src]="showInfo.moviePosterUrl" [alt]="showInfo.movieTitle" class="show-poster" />
              </div>
            }
          </div>
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
          <span class="cinema-screen-text">SCREEN THIS WAY</span>
        </div>

        <!-- Selected Seats Chips Bar -->
        @if (selectedSeats.length > 0) {
          <div class="selected-seats-bar">
            <div class="selected-seats-info">
              <span class="selection-badge">Selected ({{ selectedSeats.length }}/10)</span>
              <div class="seat-chips-list">
                @for (seat of selectedSeats; track seat.showSeatId) {
                  <span class="seat-chip" [class]="seat.seatType.toLowerCase()">
                    {{ seat.seatNumber }} ({{ seat.seatType }})
                    <button class="chip-remove" (click)="toggleSeat(seat)" title="Remove seat">✕</button>
                  </span>
                }
              </div>
            </div>
            <button class="btn btn-ghost danger" (click)="clearSelectedSeats()" style="padding: 4px 10px; font-size: 12px;">
              Clear All
            </button>
          </div>
        }

        <!-- Tiered Seating Hall Layout -->
        <div class="seating-hall">
          @for (tier of seatTiers; track tier.name) {
            <div class="tier-section">
              <div class="tier-header">
                <span class="tier-name">{{ tier.name }}</span>
                <span class="tier-price">₹{{ tier.price }}</span>
              </div>

              <div class="tier-rows">
                @for (row of tier.rows; track row.rowLetter) {
                  <div class="seat-row">
                    <span class="row-label left">{{ row.rowLetter }}</span>

                    <div class="row-seats">
                      @for (seat of row.seats; track seat.showSeatId; let idx = $index) {
                        <!-- Add an aisle separator after 2nd seat -->
                        @if (idx === 2) {
                          <div class="aisle-spacer"></div>
                        }
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
                          [title]="'Seat ' + seat.seatNumber + ' • ' + seat.seatType + ' • ₹' + seat.price">
                          {{ seat.seatNumber }}
                        </button>
                      }
                    </div>

                    <span class="row-label right">{{ row.rowLetter }}</span>
                  </div>
                }
              </div>
            </div>
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
                      <img [src]="addOn.imageUrl" [alt]="addOn.name" class="addon-image" loading="lazy" />
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
          <div class="summary-bar-inner">

            <!-- Left: seat & add-on info -->
            <div class="summary-details">
              <div class="summary-seats-row">
                <span class="summary-icon">🎟️</span>
                <span class="summary-label">
                  <strong>{{ selectedSeats.length }}</strong> seat{{ selectedSeats.length !== 1 ? 's' : '' }}
                  @if (addOnItems > 0) {
                    <span class="summary-sep">·</span>
                    <strong>{{ addOnItems }}</strong> add-on{{ addOnItems !== 1 ? 's' : '' }}
                  }
                </span>
              </div>
              <div class="summary-price-row">
                <span class="summary-amount">₹{{ grandTotal }}</span>
                @if (addOnTotal > 0) {
                  <span class="summary-breakdown">Seats ₹{{ totalPrice }} + F&B ₹{{ addOnTotal }}</span>
                }
              </div>
            </div>

            <!-- Right: CTA -->
            <button class="summary-cta" (click)="proceedToBooking()" [disabled]="bookingLoading">
              @if (bookingLoading) {
                <span class="btn-spinner"></span>
                Processing…
              } @else {
                Proceed to Pay
                <span class="cta-arrow">→</span>
              }
            </button>

          </div>
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
      display: flex;
      justify-content: space-between;
      align-items: stretch;
      margin-bottom: 24px;
      padding: 0;
      background: var(--bg-card);
      border-radius: var(--radius);
      border: 1px solid var(--border);
      overflow: hidden;
    }
    .show-info-text {
      flex: 1;
      padding: 22px 24px;
      min-width: 0;
    }
    .show-title { font-size: 26px; font-weight: 800; margin-bottom: 4px; }
    .show-meta { color: var(--text-secondary); font-size: 14px; margin-bottom: 12px; }
    .show-tags { display: flex; gap: 8px; flex-wrap: wrap; }
    .tag {
      background: rgba(244, 63, 94, 0.08); color: var(--accent);
      padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600;
      border: 1px solid rgba(244, 63, 94, 0.15);
    }

    /* Right column: timer + poster */
    .show-info-right {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      justify-content: space-between;
      gap: 10px;
      padding: 18px 18px 18px 8px;
      flex-shrink: 0;
    }

    /* Square poster thumbnail */
    .show-poster-wrap {
      width: 80px;
      height: 80px;
      border-radius: 10px;
      overflow: hidden;
      flex-shrink: 0;
      border: 2px solid var(--border);
      box-shadow: 0 4px 16px rgba(0,0,0,0.4);
    }
    .show-poster {
      width: 100%;
      height: 100%;
      object-fit: cover;
      object-position: center top;
      display: block;
    }

    .timer {
      font-size: 22px; font-weight: 700; font-family: var(--font-mono);
      color: var(--success); padding: 8px 16px;
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

    /* Seating Hall Layout */
    .seating-hall {
      max-width: 640px;
      margin: 0 auto 48px;
      display: flex;
      flex-direction: column;
      gap: 28px;
    }
    .tier-section {
      background: rgba(255, 255, 255, 0.015);
      border: 1px dashed var(--border);
      border-radius: var(--radius);
      padding: 18px 24px 22px;
    }
    .tier-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      padding-bottom: 8px;
      border-bottom: 1px solid var(--border);
      .tier-name { font-size: 12px; font-weight: 800; letter-spacing: 1px; color: var(--text-muted); text-transform: uppercase; }
      .tier-price { font-size: 14px; font-weight: 700; color: var(--accent); }
    }
    .tier-rows {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .seat-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }
    .row-label {
      font-size: 13px;
      font-weight: 800;
      color: var(--text-muted);
      min-width: 20px;
      text-align: center;
    }
    .row-seats {
      display: flex;
      align-items: center;
      gap: 10px;
      flex: 1;
      justify-content: center;
    }
    .aisle-spacer {
      width: 24px;
    }

    /* Selected seats chips bar */
    .selected-seats-bar {
      max-width: 640px;
      margin: 0 auto 24px;
      padding: 12px 18px;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-sm);
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
    }
    .selected-seats-info {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }
    .selection-badge {
      font-size: 12px;
      font-weight: 700;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .seat-chips-list {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }
    .seat-chip {
      background: var(--bg-secondary);
      border: 1px solid var(--border);
      color: var(--text-primary);
      font-size: 12px;
      font-weight: 700;
      padding: 4px 8px;
      border-radius: 6px;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      &.vip { border-color: #f59e0b; color: #f59e0b; }
      &.premium { border-color: #8b5cf6; color: #8b5cf6; }
      &.regular { border-color: var(--accent); color: var(--accent); }
    }
    .chip-remove {
      background: none; border: none; color: inherit; cursor: pointer;
      font-size: 10px; padding: 0 2px; opacity: 0.7;
      &:hover { opacity: 1; }
    }

    .seat {
      aspect-ratio: 1;
      width: 44px;
      height: 44px;
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

    /* ─── Add-ons Section ──────────────────────────────────────── */
    .addons-section {
      max-width: 800px;
      margin: 0 auto 40px;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      overflow: hidden;
    }
    .addons-head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 18px 24px;
      cursor: pointer;
      user-select: none;
      transition: background 0.18s;
      &:hover { background: rgba(255,255,255,0.03); }
      h2 { font-size: 17px; font-weight: 700; margin: 0 0 4px; }
      .sub { font-size: 13px; color: var(--text-secondary); margin: 0; }
      .chev {
        font-size: 20px; color: var(--text-muted); transition: transform 0.25s;
        &.open { transform: rotate(180deg); }
      }
    }
    .addons-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(168px, 1fr));
      gap: 14px;
      padding: 16px 20px 20px;
      border-top: 1px solid var(--border);
      background: var(--bg-primary);
    }
    .addon-card {
      display: flex;
      flex-direction: column;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: 12px;
      overflow: hidden;
      transition: border-color 0.2s, box-shadow 0.2s, transform 0.18s;
      &:hover {
        border-color: var(--accent);
        box-shadow: 0 6px 24px rgba(244, 63, 94, 0.12);
        transform: translateY(-2px);
      }
    }
    .addon-image-wrap {
      position: relative;
      width: 100%;
      height: 116px;
      overflow: hidden;
      background: #0d0d1a;
      flex-shrink: 0;
    }
    .addon-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
      object-position: center;
      display: block;
      transition: transform 0.3s ease;
    }
    .addon-card:hover .addon-image { transform: scale(1.06); }
    .addon-chip {
      position: absolute;
      top: 8px; left: 8px;
      font-size: 10px; font-weight: 700;
      letter-spacing: 0.4px; text-transform: uppercase;
      padding: 3px 8px; border-radius: 20px;
      background: rgba(10, 11, 20, 0.72);
      backdrop-filter: blur(4px);
      -webkit-backdrop-filter: blur(4px);
      color: #fff;
      border: 1px solid rgba(255,255,255,0.15);
      &.snack  { border-color: rgba(245,158,11,0.35); color: #fbbf24; }
      &.drink  { border-color: rgba(59,130,246,0.35); color: #60a5fa; }
      &.combo  { border-color: rgba(168,85,247,0.35); color: #c084fc; }
      &.meal   { border-color: rgba(34,197,94,0.35); color: #4ade80; }
    }
    .addon-body {
      display: flex;
      flex-direction: column;
      gap: 6px;
      padding: 12px 12px 14px;
      flex: 1;
    }
    .addon-name {
      font-size: 13px; font-weight: 700;
      color: var(--text-primary); line-height: 1.3;
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .addon-desc {
      font-size: 11px; color: var(--text-muted); line-height: 1.4; flex: 1;
      display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
    }
    .addon-footer {
      display: flex; align-items: center; justify-content: space-between; margin-top: 8px;
    }
    .addon-price {
      font-size: 15px; font-weight: 800; color: var(--accent);
    }
    .qty-stepper {
      display: flex; align-items: center;
      background: var(--bg-secondary);
      border: 1px solid var(--border); border-radius: 8px; overflow: hidden;
    }
    .qty-btn {
      background: none; border: none; color: var(--text-primary);
      width: 30px; height: 30px; font-size: 16px; font-weight: 700;
      cursor: pointer; display: flex; align-items: center; justify-content: center;
      transition: background 0.15s;
      &:hover:not(:disabled) { background: rgba(244, 63, 94, 0.12); color: var(--accent); }
      &:disabled { opacity: 0.3; cursor: not-allowed; }
    }
    .qty-val {
      font-size: 13px; font-weight: 700; color: var(--text-primary);
      min-width: 26px; text-align: center;
      border-left: 1px solid var(--border); border-right: 1px solid var(--border);
      line-height: 30px;
    }

    /* ─── Sticky Booking Summary Bar ─────────────────────────────── */
    .summary-bar {
      position: fixed;
      bottom: 0; left: 0; right: 0;
      z-index: 200;
      background: rgba(13, 14, 26, 0.82);
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      border-top: 1px solid rgba(244, 63, 94, 0.2);
      box-shadow: 0 -8px 40px rgba(0, 0, 0, 0.5);
      padding: 14px 24px;
      animation: slideUp 0.25s cubic-bezier(0.16, 1, 0.3, 1);
    }
    @keyframes slideUp {
      from { transform: translateY(100%); opacity: 0; }
      to   { transform: translateY(0);    opacity: 1; }
    }

    .summary-bar-inner {
      max-width: 1100px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 20px;
    }

    .summary-details {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .summary-seats-row {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .summary-icon { font-size: 18px; }
    .summary-label {
      font-size: 15px;
      color: var(--text-primary);
      font-weight: 500;
      strong { font-weight: 800; color: white; }
    }
    .summary-sep {
      color: var(--text-muted);
      margin: 0 4px;
    }
    .summary-price-row {
      display: flex;
      align-items: baseline;
      gap: 10px;
    }
    .summary-amount {
      font-size: 26px;
      font-weight: 800;
      color: white;
      letter-spacing: -0.5px;
      line-height: 1;
    }
    .summary-breakdown {
      font-size: 12px;
      color: var(--text-muted);
    }

    .summary-cta {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 14px 32px;
      background: var(--accent-gradient);
      color: white;
      font-size: 15px;
      font-weight: 700;
      border: none;
      border-radius: 12px;
      cursor: pointer;
      white-space: nowrap;
      flex-shrink: 0;
      box-shadow: 0 4px 20px rgba(244, 63, 94, 0.4);
      transition: transform 0.15s, box-shadow 0.15s, opacity 0.15s;
      &:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 8px 28px rgba(244, 63, 94, 0.55);
      }
      &:active:not(:disabled) { transform: translateY(0); }
      &:disabled { opacity: 0.55; cursor: not-allowed; }
      .cta-arrow {
        font-size: 18px;
        font-weight: 400;
        transition: transform 0.2s;
      }
      &:hover:not(:disabled) .cta-arrow { transform: translateX(4px); }
    }
    .btn-spinner {
      width: 16px; height: 16px;
      border: 2px solid rgba(255,255,255,0.35);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
      display: inline-block;
    }
  `]
})
export class SeatSelectionComponent implements OnInit, OnDestroy {
  seats: ShowSeatResponse[] = [];
  seatTiers: SeatTier[] = [];
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

  clearSelectedSeats() {
    this.selectedSeats = [];
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
        this.buildTiers(seats);
        if (seats.length > 0) {
          const first = seats[0];
          this.showInfo = {
            movieTitle: first.movieTitle,
            moviePosterUrl: first.moviePosterUrl,
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

  private buildTiers(seats: ShowSeatResponse[]) {
    const vipRows: Record<string, ShowSeatResponse[]> = {};
    const premiumRows: Record<string, ShowSeatResponse[]> = {};
    const regularRows: Record<string, ShowSeatResponse[]> = {};

    seats.forEach(seat => {
      const row = seat.seatNumber ? seat.seatNumber.charAt(0) : 'A';
      if (seat.seatType === 'VIP') {
        if (!vipRows[row]) vipRows[row] = [];
        vipRows[row].push(seat);
      } else if (seat.seatType === 'PREMIUM') {
        if (!premiumRows[row]) premiumRows[row] = [];
        premiumRows[row].push(seat);
      } else {
        if (!regularRows[row]) regularRows[row] = [];
        regularRows[row].push(seat);
      }
    });

    const tiers: SeatTier[] = [];
    if (Object.keys(vipRows).length > 0) {
      tiers.push({
        name: 'VIP SEATING',
        price: 500,
        rows: Object.entries(vipRows).map(([letter, list]) => ({
          rowLetter: letter,
          seats: list.sort((a, b) => a.seatNumber.localeCompare(b.seatNumber))
        }))
      });
    }
    if (Object.keys(premiumRows).length > 0) {
      tiers.push({
        name: 'PREMIUM SEATING',
        price: 350,
        rows: Object.entries(premiumRows).map(([letter, list]) => ({
          rowLetter: letter,
          seats: list.sort((a, b) => a.seatNumber.localeCompare(b.seatNumber))
        }))
      });
    }
    if (Object.keys(regularRows).length > 0) {
      tiers.push({
        name: 'CLASSIC / REGULAR SEATING',
        price: 200,
        rows: Object.entries(regularRows).map(([letter, list]) => ({
          rowLetter: letter,
          seats: list.sort((a, b) => a.seatNumber.localeCompare(b.seatNumber))
        }))
      });
    }

    this.seatTiers = tiers;
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

