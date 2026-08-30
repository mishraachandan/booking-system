import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface City {
  id: number;
  name: string;
}

export const CITY_ID_KEY = 'selectedCityId';
export const CITY_NAME_KEY = 'selectedCityName';

@Injectable({ providedIn: 'root' })
export class CityService {
  private readonly baseUrl = '/api/v1/cities';

  // Reactive state for the currently active city
  readonly selectedCityId = signal<number>(
    typeof localStorage !== 'undefined' ? Number(localStorage.getItem(CITY_ID_KEY)) || 0 : 0
  );
  readonly selectedCityName = signal<string>(
    typeof localStorage !== 'undefined' ? localStorage.getItem(CITY_NAME_KEY) || '' : ''
  );

  constructor(private http: HttpClient) {}

  getCities(): Observable<City[]> {
    return this.http.get<City[]>(this.baseUrl).pipe(
      tap(cities => {
        // If no city is selected yet, default to the first city (e.g. Mumbai)
        if (cities.length > 0 && this.selectedCityId() === 0) {
          this.setCity(cities[0].id, cities[0].name);
        }
      })
    );
  }

  setCity(id: number, name: string) {
    this.selectedCityId.set(id);
    this.selectedCityName.set(name);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(CITY_ID_KEY, String(id));
      localStorage.setItem(CITY_NAME_KEY, name);
    }
  }

  createCity(name: string): Observable<City> {
    return this.http.post<City>(this.baseUrl, { name });
  }
}

