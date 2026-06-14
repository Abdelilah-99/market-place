import { Injectable, signal } from '@angular/core';
import { Product } from '../models/Product';

@Injectable({
  providedIn: 'root'
})
export class RecentlyViewedService {
  private readonly STORAGE_KEY = 'recentlyViewed';
  private readonly MAX_ITEMS = 4;
  
  recentlyViewed = signal<Product[]>([]);

  constructor() {
    this.loadFromStorage();
  }

  addProduct(product: Product) {
    let current = [...this.recentlyViewed()];
    // Remove if already exists and add to front
    current = current.filter(p => p.id !== product.id);
    current.unshift(product);
    // Limit items
    current = current.slice(0, this.MAX_ITEMS);
    
    this.recentlyViewed.set(current);
    this.saveToStorage();
  }

  private saveToStorage() {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.recentlyViewed()));
    }
  }

  private loadFromStorage() {
    if (typeof localStorage !== 'undefined') {
      const saved = localStorage.getItem(this.STORAGE_KEY);
      if (saved) {
        try {
          this.recentlyViewed.set(JSON.parse(saved));
        } catch (e) {
          console.error('Error parsing recently viewed', e);
        }
      }
    }
  }
}
