import { Injectable, signal } from '@angular/core';

export type Currency = 'MAD' | 'EUR' | 'USD';

@Injectable({
  providedIn: 'root'
})
export class SettingsService {
  currentCurrency = signal<Currency>('MAD');
  
  // Simplified exchange rates for prototype
  private exchangeRates: Record<Currency, number> = {
    'MAD': 1,
    'EUR': 0.092,
    'USD': 0.1
  };

  constructor() {
    if (typeof localStorage !== 'undefined') {
      const saved = localStorage.getItem('preferredCurrency') as Currency;
      if (saved && ['MAD', 'EUR', 'USD'].includes(saved)) {
        this.currentCurrency.set(saved);
      }
    }
  }

  setCurrency(currency: Currency) {
    this.currentCurrency.set(currency);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('preferredCurrency', currency);
    }
  }

  convert(amount: number): { amount: number, currency: Currency } {
    const currency = this.currentCurrency();
    return {
      amount: amount * this.exchangeRates[currency],
      currency: currency
    };
  }
}
