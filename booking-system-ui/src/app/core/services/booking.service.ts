import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Booking {
  id: number;
  user: { id: number; firstName: string; lastName: string };
  show: { id: number; movie: { title: string }; screen: { name: string; cinema: { name: string } }; startTime: string };
  resource: any;
  numberOfTickets: number;
  status: string;
  notes: string;
  startTime: string;
  endTime: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly baseUrl = '/api/bookings';

  constructor(private http: HttpClient) {}

  bookShowSeats(showId: number, showSeatIds: number[], notes?: string): Observable<Booking> {
    return this.http.post<Booking>(`${this.baseUrl}/show-seats`, { showId, showSeatIds, notes });
  }

  confirmBooking(bookingId: number): Observable<Booking> {
    return this.http.post<Booking>(`${this.baseUrl}/${bookingId}/confirm`, {});
  }

  getMyBookings(): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${this.baseUrl}/my`);
  }

  cancelBooking(bookingId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${bookingId}`);
  }
}
