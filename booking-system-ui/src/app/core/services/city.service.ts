import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface City {
  id: number;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class CityService {
  private readonly baseUrl = '/api/v1/cities';

  constructor(private http: HttpClient) {}

  getCities(): Observable<City[]> {
    return this.http.get<City[]>(this.baseUrl);
  }

  createCity(name: string): Observable<City> {
    return this.http.post<City>(this.baseUrl, { name });
  }
}
