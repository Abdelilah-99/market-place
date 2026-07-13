import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Product } from '../models/Product';

export interface CheckoutSessionResponse {
  sessionId: string;
  checkoutUrl: string;
}

@Injectable({
  providedIn: 'root',
})
export class PaymentsService {
  private readonly apiUrl = '/api/payments';

  constructor(private http: HttpClient) { }

  createCheckoutSession(product: Product): Observable<CheckoutSessionResponse> {
    return this.http.post<CheckoutSessionResponse>(`${this.apiUrl}/checkout-sessions`, {
      productId: product.id,
      quantity: 1,
    });
  }

  redirectToCheckout(product: Product): void {
    this.createCheckoutSession(product).subscribe({
      next: (session) => {
        if (session.checkoutUrl) {
          window.location.href = session.checkoutUrl;
        }
      },
      error: (err) => {
        console.error('Stripe checkout failed', err);
      },
    });
  }

}
