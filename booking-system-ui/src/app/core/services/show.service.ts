import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Movie {
  id: number;
  title: string;
  description: string;
  language: string;
  genre: string;
  durationMinutes: number;
  releaseDate: string;
  posterUrl: string;
  trailerUrl?: string;
  cast?: string[];
  rating?: number;
}

export interface Screen {
  id: number;
  name: string;
  cinema: { id: number; name: string; address: string; city: { id: number; name: string } };
  totalSeats: number;
}

export interface Show {
  id: number;
  movie: Movie;
  screen: Screen;
  startTime: string;
  endTime: string;
}

export interface ShowSeat {
  id: number;
  show: Show;
  seat: { id: number; seatNumber: string; seatType: string };
  price: number;
  status: 'AVAILABLE' | 'LOCKED' | 'BOOKED';
  lockedByUserId: number | null;
}

export interface ShowSeatResponse {
  showSeatId: number;
  price: number;
  status: 'AVAILABLE' | 'LOCKED' | 'BOOKED';
  lockedByUserId: number | null;
  seatId: number;
  seatNumber: string;
  seatType: string;
  showId: number;
  startTime: string;
  endTime: string;
  movieTitle: string;
  moviePosterUrl: string;
  movieGenre: string;
  movieLanguage: string;
  movieDurationMinutes: number;
  screenName: string;
  cinemaName: string;
  cinemaAddress: string;
  cityName: string;
}

@Injectable({ providedIn: 'root' })
export class ShowService {
  private readonly baseUrl = '/api/v1/shows';

  constructor(private http: HttpClient) {}

  getAllShows(cityId?: number): Observable<Show[]> {
    const params = cityId && cityId > 0 ? `?cityId=${cityId}` : '';
    return this.http.get<Show[]>(`${this.baseUrl}${params}`);
  }

  getShowsByMovie(movieId: number): Observable<Show[]> {
    return this.http.get<Show[]>(`${this.baseUrl}/movie/${movieId}`);
  }

  getShowSeats(showId: number): Observable<ShowSeatResponse[]> {
    return this.http.get<ShowSeatResponse[]>(`${this.baseUrl}/${showId}/seats`);
  }

  getAvailableSeats(showId: number): Observable<ShowSeatResponse[]> {
    return this.http.get<ShowSeatResponse[]>(`${this.baseUrl}/${showId}/seats/available`);
  }

  lockSeats(showId: number, showSeatIds: number[]): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(
      `${this.baseUrl}/${showId}/seats/lock`,
      { showSeatIds }
    );
  }

  unlockSeat(showSeatId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/seats/${showSeatId}/unlock`, {});
  }
}
