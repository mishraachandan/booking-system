import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type AddOnCategory = 'FOOD' | 'BEVERAGE' | 'COMBO';

export interface AddOn {
  id: number;
  name: string;
  description: string;
  category: AddOnCategory;
  price: number;
  imageUrl: string;
}

export interface BookingAddOnLine {
  addOnId: number;
  quantity: number;
}

export interface BookingAddOnResponse {
  id: number;
  addOnId: number;
  name: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

@Injectable({ providedIn: 'root' })
export class AddOnService {
  private readonly baseUrl = '/api/v1/addons';

  constructor(private http: HttpClient) {}

  getAvailableAddOns(): Observable<AddOn[]> {
    return this.http.get<AddOn[]>(this.baseUrl);
  }
}
