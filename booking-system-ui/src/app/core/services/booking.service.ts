import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BookingResponse {
  bookingId: number;
  status: string;
  numberOfTickets: number;
  notes: string;
  startTime: string;
  endTime: string;
  createdAt: string;

  // User info
  userId: number;
  userFirstName: string;
  userLastName: string;
  userEmail: string;

  // Show info
  showId: number | null;
  movieTitle: string | null;
  movieGenre: string | null;
  movieDurationMinutes: number | null;
  screenName: string | null;
  cinemaName: string | null;
  cityName: string | null;
  showStartTime: string | null;

  // Resource info
  resourceId: number | null;
  resourceName: string | null;
}

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly baseUrl = '/api/bookings';

  constructor(private http: HttpClient) {}

  bookShowSeats(showId: number, showSeatIds: number[], notes?: string): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(`${this.baseUrl}/show-seats`, { showId, showSeatIds, notes });
  }

  confirmBooking(bookingId: number): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(`${this.baseUrl}/${bookingId}/confirm`, {});
  }

  getMyBookings(): Observable<BookingResponse[]> {
    return this.http.get<BookingResponse[]>(`${this.baseUrl}/my`);
  }

  cancelBooking(bookingId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${bookingId}`);
  }
}
