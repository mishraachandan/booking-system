import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Movie } from './show.service';

@Injectable({ providedIn: 'root' })
export class MovieService {
  private readonly baseUrl = '/api/v1/movies';

  constructor(private http: HttpClient) {}

  getAllMovies(): Observable<Movie[]> {
    return this.http.get<Movie[]>(this.baseUrl);
  }

  getMovie(movieId: number): Observable<Movie> {
    return this.http.get<Movie>(`${this.baseUrl}/${movieId}`);
  }
}
