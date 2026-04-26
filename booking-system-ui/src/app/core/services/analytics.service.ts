import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AnalyticsOverview {
  totalBookings: number;
  confirmedBookings: number;
  cancelledBookings: number;
  totalRevenue: number;
  averageTicketPrice: number;
  refundRate: number;
}

export interface RevenuePoint {
  label: string;
  revenue: number;
  bookings: number;
}

export interface TopShowEntry {
  showId: number;
  movieTitle: string;
  cinemaName: string;
  startTime: string;
  totalSeats: number;
  bookedSeats: number;
  revenue: number;
  occupancyPercent: number;
}

export interface OccupancyEntry {
  showId: number;
  movieTitle: string;
  cinemaName: string;
  screenName: string;
  startTime: string;
  totalSeats: number;
  bookedSeats: number;
  lockedSeats: number;
  occupancyPercent: number;
}

export interface DateRange {
  from?: string;
  to?: string;
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly baseUrl = '/api/v1/admin/analytics';
  private http = inject(HttpClient);

  overview(range: DateRange = {}): Observable<AnalyticsOverview> {
    return this.http.get<AnalyticsOverview>(`${this.baseUrl}/overview`, { params: this.params(range) });
  }

  revenueDaily(range: DateRange = {}): Observable<RevenuePoint[]> {
    return this.http.get<RevenuePoint[]>(`${this.baseUrl}/revenue/daily`, { params: this.params(range) });
  }

  revenueByCinema(range: DateRange = {}): Observable<RevenuePoint[]> {
    return this.http.get<RevenuePoint[]>(`${this.baseUrl}/revenue/by-cinema`, { params: this.params(range) });
  }

  revenueByMovie(range: DateRange = {}): Observable<RevenuePoint[]> {
    return this.http.get<RevenuePoint[]>(`${this.baseUrl}/revenue/by-movie`, { params: this.params(range) });
  }

  topShows(range: DateRange = {}, limit = 10): Observable<TopShowEntry[]> {
    return this.http.get<TopShowEntry[]>(`${this.baseUrl}/top-shows`, {
      params: this.params(range).set('limit', String(limit))
    });
  }

  occupancy(range: DateRange = {}): Observable<OccupancyEntry[]> {
    return this.http.get<OccupancyEntry[]>(`${this.baseUrl}/occupancy`, { params: this.params(range) });
  }

  private params(range: DateRange): HttpParams {
    let p = new HttpParams();
    if (range.from) p = p.set('from', range.from);
    if (range.to) p = p.set('to', range.to);
    return p;
  }
}
