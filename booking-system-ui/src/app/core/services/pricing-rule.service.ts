import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PricingRule {
  id?: number;
  name: string;
  description?: string | null;
  active?: boolean;
  priority?: number;
  daysOfWeek?: string | null;
  startHour?: number | null;
  endHour?: number | null;
  minLeadTimeHours?: number | null;
  maxLeadTimeHours?: number | null;
  multiplier: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface PriceBreakdown {
  base: number;
  effective: number;
  appliedRules: string[];
}

@Injectable({ providedIn: 'root' })
export class PricingRuleService {
  private readonly baseUrl = '/api/v1/admin/pricing';
  private http = inject(HttpClient);

  status(): Observable<{ enabled: boolean }> {
    return this.http.get<{ enabled: boolean }>(`${this.baseUrl}/status`);
  }

  list(): Observable<PricingRule[]> {
    return this.http.get<PricingRule[]>(`${this.baseUrl}/rules`);
  }

  create(rule: PricingRule): Observable<PricingRule> {
    return this.http.post<PricingRule>(`${this.baseUrl}/rules`, rule);
  }

  update(id: number, rule: PricingRule): Observable<PricingRule> {
    return this.http.put<PricingRule>(`${this.baseUrl}/rules/${id}`, rule);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/rules/${id}`);
  }

  preview(basePrice: number, showStart: string, at?: string): Observable<PriceBreakdown> {
    let params = new HttpParams()
      .set('basePrice', String(basePrice))
      .set('showStart', showStart);
    if (at) params = params.set('at', at);
    return this.http.get<PriceBreakdown>(`${this.baseUrl}/preview`, { params });
  }
}
