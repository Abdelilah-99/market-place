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
      productName: product.name,
      amount: product.price,
      currency: 'usd',
      imageUrl: this.absoluteProductImage(product),
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

  private absoluteProductImage(product: Product): string | null {
    const image = product.image || product.images?.[0];
    if (!image || typeof window === 'undefined') {
      return null;
    }
    return new URL(`/api/media/products/${image}`, window.location.origin).toString();
  }
}
