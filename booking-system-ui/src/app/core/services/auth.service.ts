import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface LoginResponse {
  userId: number;
  email: string;
  firstName: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = '/api/auth';
  private loggedIn = new BehaviorSubject<boolean>(this.hasUserId());

  isLoggedIn$ = this.loggedIn.asObservable();

  constructor(private http: HttpClient) {}

  private hasUserId(): boolean {
    return !!localStorage.getItem('userId');
  }

  register(data: { email: string; password: string; firstName: string; lastName: string; phone: string }): Observable<string> {
    return this.http.post(`${this.baseUrl}/register`, data, { responseType: 'text' });
  }

  verifyOtp(email: string, otp: string): Observable<string> {
    return this.http.post(`${this.baseUrl}/verify-otp`, { email, otp }, { responseType: 'text' });
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, { email, password }).pipe(
      tap((res) => {
        localStorage.setItem('userId', String(res.userId));
        localStorage.setItem('userEmail', res.email);
        localStorage.setItem('firstName', res.firstName);
        this.loggedIn.next(true);
      })
    );
  }

  logout(): void {
    localStorage.removeItem('userId');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('firstName');
    this.loggedIn.next(false);
  }

  getUserId(): string | null {
    return localStorage.getItem('userId');
  }

  getFirstName(): string {
    return localStorage.getItem('firstName') || 'User';
  }
}
