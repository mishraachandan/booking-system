import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CreateOrderResponse {
  razorpayOrderId: string;
  amount: number;       // in paise
  currency: string;
  keyId: string;
  bookingId: number;
}

export interface VerifyPaymentRequest {
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
}

export interface PaymentStatusResponse {
  paymentId: number;
  bookingId: number;
  status: 'CREATED' | 'SUCCESS' | 'FAILED';
  amount: number;
  razorpayOrderId: string;
  razorpayPaymentId: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly baseUrl = '/api/payments';

  constructor(private http: HttpClient) {}

  /**
   * Step 1: Create a Razorpay order for the booking.
   */
  createOrder(bookingId: number): Observable<CreateOrderResponse> {
    return this.http.post<CreateOrderResponse>(`${this.baseUrl}/create-order`, { bookingId });
  }

  /**
   * Step 2: Verify the Razorpay payment signature and confirm the booking.
   */
  verifyPayment(data: VerifyPaymentRequest): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/verify`, data);
  }

  /**
   * Get the latest payment status for a booking.
   */
  getPaymentStatus(bookingId: number): Observable<PaymentStatusResponse> {
    return this.http.get<PaymentStatusResponse>(`${this.baseUrl}/booking/${bookingId}`);
  }
}
