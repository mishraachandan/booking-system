import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PaymentService } from '../../core/services/payment.service';

// Declare the Razorpay class loaded from the CDN script in index.html
declare const Razorpay: any;

@Component({
  selector: 'app-booking-summary',
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="summary-page page-enter">
      <div class="summary-card card">

        <!-- Payment Success State -->
        @if (confirmed) {
          <div class="success-state">
            <div class="check-circle">✓</div>
            <h1>Booking Confirmed!</h1>
            <p class="booking-id">Booking #{{ bookingId }}</p>
            <p class="total">Total Paid: ₹{{ total }}</p>
            <div class="actions">
              <a routerLink="/my-bookings" class="btn btn-primary">View My Bookings</a>
              <a routerLink="/" class="btn btn-outline">Browse More</a>
            </div>
          </div>

        <!-- Payment Pending / Cancelled State -->
        } @else if (paymentCancelled) {
          <div class="cancelled-state">
            <div class="cancel-icon">✕</div>
            <h1>Payment Cancelled</h1>
            <p class="sub">Your seats are still reserved for a few minutes. Try paying again.</p>
            @if (error) { <div class="error-msg">{{ error }}</div> }
            <div class="actions">
              <button class="btn btn-primary" (click)="startPayment()" [disabled]="loading">
                {{ loading ? 'Opening payment...' : 'Retry Payment ₹' + total }}
              </button>
              <a routerLink="/" class="btn btn-outline">Cancel & Go Home</a>
            </div>
          </div>

        <!-- Ready to Pay State -->
        } @else {
          <div class="confirm-state">
            <h1>Complete Your Booking</h1>
            <div class="details">
              <div class="detail-row">
                <span>Booking ID</span>
                <span>#{{ bookingId }}</span>
              </div>
              <div class="detail-row">
                <span>Seats</span>
                <span>{{ seats }} seat{{ seats !== 1 ? 's' : '' }} · ₹{{ seatTotal }}</span>
              </div>
              @if (addOnTotal > 0) {
                <div class="detail-row">
                  <span>Add-ons 🍿</span>
                  <span>₹{{ addOnTotal }}</span>
                </div>
              }
              <div class="detail-row total-row">
                <span>Total Amount</span>
                <span class="price">₹{{ total }}</span>
              </div>
            </div>

            <div class="razorpay-info">
              🔒 Secured by Razorpay
            </div>

            @if (error) { <div class="error-msg">{{ error }}</div> }

            <button
              id="pay-now-btn"
              class="btn btn-primary full-width pay-btn"
              (click)="startPayment()"
              [disabled]="loading">
              {{ loading ? 'Opening payment...' : '💳 Pay Now ₹' + total }}
            </button>

            <p class="cancel-note">
              Your seats are reserved. Payment must be completed within 10 minutes.
            </p>
          </div>
        }
      </div>
    </div>

    <!-- Dummy Checkout Simulation Modal -->
    @if (showDummyCheckout) {
      <div class="modal-backdrop">
        <div class="modal-card" style="max-width: 500px;" (click)="$event.stopPropagation()">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
            <h2>Simulated Checkout</h2>
            <button class="btn btn-ghost" style="padding: 4px 8px; font-size: 16px;" (click)="showDummyCheckout = false" [disabled]="isProcessingDummy">✕</button>
          </div>
          
          <div class="alert alert-info">
            ℹ️ <strong>Razorpay Mock Mode:</strong> Since api keys are not configured, you can test the checkout flow using this simulated sandbox.
          </div>

          @if (!isProcessingDummy) {
            <!-- Payment Mode Selector -->
            <div class="checkout-payment-modes">
              <button class="mode-btn" [class.active]="dummyPaymentMode === 'card'" (click)="dummyPaymentMode = 'card'">
                <span>💳</span> Card
              </button>
              <button class="mode-btn" [class.active]="dummyPaymentMode === 'upi'" (click)="dummyPaymentMode = 'upi'">
                <span>📱</span> UPI
              </button>
              <button class="mode-btn" [class.active]="dummyPaymentMode === 'netbanking'" (click)="dummyPaymentMode = 'netbanking'">
                <span>🏦</span> Netbanking
              </button>
            </div>

            <!-- Card form -->
            @if (dummyPaymentMode === 'card') {
              <div class="card-graphic">
                <div class="card-top">
                  <div class="chip-graphic"></div>
                  <div class="card-type">VISA</div>
                </div>
                <div class="card-number">{{ dummyCardNumber || '•••• •••• •••• ••••' }}</div>
                <div class="card-bottom">
                  <div class="card-holder">
                    <label>Card Holder</label>
                    <span>{{ dummyCardName || 'John Doe' }}</span>
                  </div>
                  <div class="card-expiry">
                    <label>Expires</label>
                    <span>{{ dummyCardExpiry || 'MM/YY' }}</span>
                  </div>
                </div>
              </div>

              <div class="form-group" style="margin-bottom: 16px;">
                <label>Card Number</label>
                <input type="text" [(ngModel)]="dummyCardNumber" placeholder="4111 2222 3333 4444" aria-label="Card Number" />
              </div>
              <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 16px; margin-bottom: 16px;">
                <div class="form-group" style="margin-bottom: 0;">
                  <label>Card Holder</label>
                  <input type="text" [(ngModel)]="dummyCardName" placeholder="John Doe" aria-label="Card Holder" />
                </div>
                <div class="form-group" style="margin-bottom: 0;">
                  <label>Expiry / CVV</label>
                  <input type="text" [(ngModel)]="dummyCardExpiry" placeholder="12/29" style="text-align: center;" aria-label="Expiry Date" />
                </div>
              </div>
            }

            <!-- UPI Form -->
            @if (dummyPaymentMode === 'upi') {
              <div class="form-group" style="margin-bottom: 20px;">
                <label>UPI ID (VPA)</label>
                <input type="text" [(ngModel)]="dummyUpiId" placeholder="username@upi" aria-label="UPI ID" />
              </div>
            }

            <!-- Netbanking Form -->
            @if (dummyPaymentMode === 'netbanking') {
              <div class="form-group" style="margin-bottom: 20px;">
                <label>Select Your Bank</label>
                <select [(ngModel)]="dummySelectedBank" aria-label="Select Bank">
                  <option value="sbi">State Bank of India (SBI)</option>
                  <option value="hdfc">HDFC Bank</option>
                  <option value="icici">ICICI Bank</option>
                  <option value="axis">Axis Bank</option>
                </select>
              </div>
            }

            <button class="btn btn-primary" style="width: 100%; padding: 14px; margin-top: 10px;" (click)="submitDummyPayment()">
              💸 Pay Simulated ₹{{ total }}
            </button>
          } @else {
            <!-- Processing animation -->
            <div style="text-align: center; padding: 20px 0;">
              <h3 style="margin-bottom: 16px;">Processing Simulated Payment</h3>
              
              <div class="step-progress-bar">
                <div class="step-progress-fill" [style.width.%]="dummyProgress"></div>
              </div>

              <div class="processing-step-list">
                @for (step of dummySteps; track step; let idx = $index) {
                  <div class="processing-step-item" 
                       [class.active]="dummyActiveStep === idx"
                       [class.done]="dummyActiveStep > idx">
                    <div class="step-icon">
                      @if (dummyActiveStep > idx) { ✓ } @else { {{ idx + 1 }} }
                    </div>
                    <span>{{ step }}</span>
                  </div>
                }
              </div>
            </div>
          }
        </div>
      </div>
    }
  `,
  styles: [`
    .summary-page {
      display: flex; justify-content: center; align-items: center;
      min-height: calc(100vh - 64px); padding: 20px;
    }
    .summary-card { width: 100%; max-width: 460px; padding: 48px; text-align: center; }
    h1 { font-size: 26px; font-weight: 700; margin-bottom: 8px; }

    /* Success State */
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

    /* Cancelled State */
    .cancelled-state {
      .cancel-icon {
        width: 72px; height: 72px; border-radius: 50%;
        background: rgba(239, 68, 68, 0.15); color: var(--danger);
        display: flex; align-items: center; justify-content: center;
        font-size: 36px; font-weight: 700; margin: 0 auto 20px;
        border: 2px solid rgba(239, 68, 68, 0.3);
      }
      .sub { color: var(--text-muted); font-size: 14px; margin-bottom: 24px; }
      .actions { display: flex; flex-direction: column; gap: 12px; }
    }

    @keyframes pop {
      0% { transform: scale(0); }
      100% { transform: scale(1); }
    }

    /* Confirm State */
    .confirm-state {
      text-align: left;
      h1 { text-align: center; margin-bottom: 28px; }
    }
    .details {
      background: var(--bg-secondary); border-radius: var(--radius-sm);
      padding: 16px; margin-bottom: 20px;
      border: 1px solid var(--border);
    }
    .detail-row {
      display: flex; justify-content: space-between; padding: 12px 0;
      font-size: 15px; color: var(--text-secondary);
      &:not(:last-child) { border-bottom: 1px solid var(--border); }
      .price { font-weight: 700; color: var(--accent); font-size: 20px; }
    }
    .total-row { background: rgba(244, 63, 94, 0.05); margin: -4px -4px -4px -4px;
      padding: 14px 4px; border-radius: var(--radius-sm); }

    .razorpay-info {
      text-align: center; font-size: 12px; color: var(--text-muted);
      margin-bottom: 16px; letter-spacing: 0.3px;
    }

    .pay-btn { margin-bottom: 12px; font-size: 16px; padding: 14px; }
    .full-width { width: 100%; }

    .cancel-note {
      text-align: center; font-size: 12px; color: var(--text-muted);
      margin-top: 8px;
    }

    .error-msg {
      background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.3);
      color: var(--danger); padding: 10px; border-radius: var(--radius-sm);
      font-size: 13px; margin-bottom: 16px;
    }
  `]
})
export class BookingSummaryComponent implements OnInit {
  bookingId = 0;
  total = 0;
  seatTotal = 0;
  addOnTotal = 0;
  seats = 0;
  confirmed = false;
  paymentCancelled = false;
  loading = false;
  error = '';

  // Simulated Checkout States
  showDummyCheckout = false;
  dummyPaymentMode: 'card' | 'upi' | 'netbanking' = 'card';
  dummyCardNumber = '4111 2222 3333 4444';
  dummyCardName = 'John Doe';
  dummyCardExpiry = '12/29';
  dummyCardCvv = '123';
  dummyUpiId = 'johndoe@upi';
  dummySelectedBank = 'sbi';
  isProcessingDummy = false;
  dummyProgress = 0;
  dummyActiveStep = 0;
  dummySteps = [
    'Initializing secure connection...',
    'Validating payment details...',
    'Authorizing simulated transaction...',
    'Confirming seats & finalizing ticket...'
  ];
  dummyOrder: any = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService
  ) {}

  ngOnInit() {
    const qp = this.route.snapshot.queryParamMap;
    this.bookingId = Number(qp.get('bookingId'));
    this.total = Number(qp.get('total'));
    this.seats = Number(qp.get('seats') || '1');
    this.seatTotal = Number(qp.get('seatTotal') || this.total);
    this.addOnTotal = Number(qp.get('addOnTotal') || 0);
  }

  /**
   * Step 1: Call backend to create a Razorpay order,
   * then open the Razorpay Checkout popup (or the dummy checkout modal).
   */
  startPayment() {
    this.loading = true;
    this.error = '';
    this.paymentCancelled = false;

    this.paymentService.createOrder(this.bookingId).subscribe({
      next: (order) => {
        // Dummy payment flow: trigger if the server signalled dummy mode via
        // either the placeholder Razorpay key or the dummy order-id prefix.
        const isDummy =
          order.keyId === 'RAZORPAY_KEY_NOT_SET' ||
          order.keyId.includes('REPLACE_ME') ||
          (typeof order.razorpayOrderId === 'string' &&
            order.razorpayOrderId.startsWith('order_dummy_'));
        if (isDummy) {
          // Instantly confirm the payment, skipping the dummy interactive modal
          this.verifyPayment(
            order.razorpayOrderId,
            'dummy_payment_id_' + Math.random().toString(36).substring(2, 11),
            'dummy_signature'
          );
        } else {
          this.loading = false;
          this.openRazorpayPopup(order);
        }
      },
      error: (err) => {
        this.loading = false;
        const msg = err?.error?.error || err?.error || err?.message || 'Failed to create payment order';
        this.error = msg;
      }
    });
  }

  /**
   * Step 2 (Mock): Open simulated payment sandbox popup.
   */
  openDummyCheckout(order: any) {
    this.dummyOrder = order;
    this.showDummyCheckout = true;
    this.isProcessingDummy = false;
    this.dummyProgress = 0;
    this.dummyActiveStep = 0;
  }

  submitDummyPayment() {
    this.isProcessingDummy = true;
    this.dummyProgress = 0;
    this.dummyActiveStep = 0;
    this.error = '';

    const interval = setInterval(() => {
      this.dummyProgress += 5;
      if (this.dummyProgress >= 25 && this.dummyActiveStep === 0) {
        this.dummyActiveStep = 1;
      }
      if (this.dummyProgress >= 50 && this.dummyActiveStep === 1) {
        this.dummyActiveStep = 2;
      }
      if (this.dummyProgress >= 75 && this.dummyActiveStep === 2) {
        this.dummyActiveStep = 3;
      }
      if (this.dummyProgress >= 100) {
        clearInterval(interval);
        this.showDummyCheckout = false;
        this.isProcessingDummy = false;
        this.verifyPayment(
          this.dummyOrder.razorpayOrderId,
          'dummy_payment_id_' + Math.random().toString(36).substr(2, 9),
          'dummy_signature'
        );
      }
    }, 150); // Total duration ~3 seconds
  }

  /**
   * Step 2: Open native Razorpay Checkout popup.
   * On success → verify signature with backend.
   * On dismiss → show retry screen.
   */
  private openRazorpayPopup(order: any) {
    const options = {
      key: order.keyId,
      amount: order.amount,          // paise
      currency: order.currency,
      name: 'BookMyShow Clone',
      description: `Booking #${this.bookingId}`,
      order_id: order.razorpayOrderId,
      theme: { color: '#F43F5E' },
      handler: (response: any) => {
        // Payment captured by Razorpay — verify signature on backend
        this.verifyPayment(
          response.razorpay_order_id,
          response.razorpay_payment_id,
          response.razorpay_signature
        );
      },
      modal: {
        ondismiss: () => {
          // User closed the popup without paying
          this.paymentCancelled = true;
        }
      }
    };

    const rzp = new Razorpay(options);
    rzp.on('payment.failed', (response: any) => {
      this.error = response?.error?.description || 'Payment failed. Please try again.';
      this.paymentCancelled = true;
    });
    rzp.open();
  }

  /**
   * Step 3: Send Razorpay callback data to backend for HMAC verification.
   * On success → show confirmed state.
   */
  private verifyPayment(
    razorpayOrderId: string,
    razorpayPaymentId: string,
    razorpaySignature: string
  ) {
    this.loading = true;
    this.paymentService.verifyPayment({ razorpayOrderId, razorpayPaymentId, razorpaySignature })
      .subscribe({
        next: () => {
          this.confirmed = true;
          this.loading = false;
        },
        error: (err) => {
          this.loading = false;
          this.error = err?.error?.error || 'Payment verification failed. Please contact support.';
          this.paymentCancelled = true;
        }
      });
  }
}
