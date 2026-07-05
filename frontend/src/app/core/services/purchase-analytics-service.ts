import { Injectable } from '@angular/core';
import { Product } from '../models/Product';
import { Me } from './users-service';

export interface PurchaseRecord {
  id: string;
  buyerId: string;
  buyerName: string;
  sellerId: string;
  productId: string;
  productName: string;
  productImage: string;
  productCategory?: string;
  unitPrice: number;
  quantity: number;
  total: number;
  averageRating: number;
  ratingCount: number;
  purchasedAt: string;
}

export interface ProductAnalyticsItem {
  productId: string;
  name: string;
  image: string;
  category?: string;
  quantity: number;
  total: number;
  averageRating: number;
  ratingCount: number;
}

export interface BuyerProfileAnalytics {
  totalSpent: number;
  totalOrders: number;
  totalItems: number;
  bestProducts: ProductAnalyticsItem[];
  mostBoughtProducts: ProductAnalyticsItem[];
}

export interface SellerProfileAnalytics {
  totalGained: number;
  totalOrders: number;
  totalItemsSold: number;
  bestSellingProducts: ProductAnalyticsItem[];
}

@Injectable({
  providedIn: 'root',
})
export class PurchaseAnalyticsService {
  private readonly storageKey = 'marketplace_purchase_records';

  recordPurchase(product: Product, buyer: Me, quantity = 1): PurchaseRecord {
    const safeQuantity = Math.max(1, Math.floor(quantity));
    const record: PurchaseRecord = {
      id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
      buyerId: buyer.id,
      buyerName: buyer.username,
      sellerId: product.userId,
      productId: product.id,
      productName: product.name,
      productImage: product.image || product.images?.[0] || '',
      productCategory: product.category,
      unitPrice: Number(product.price) || 0,
      quantity: safeQuantity,
      total: (Number(product.price) || 0) * safeQuantity,
      averageRating: Number(product.averageRating) || 0,
      ratingCount: Number(product.ratingCount) || 0,
      purchasedAt: new Date().toISOString(),
    };

    this.saveRecords([record, ...this.getRecords()]);
    return record;
  }

  getBuyerAnalytics(buyerId: string): BuyerProfileAnalytics {
    const purchases = this.getRecords().filter(record => record.buyerId === buyerId);
    const products = this.groupRecordsByProduct(purchases);

    return {
      totalSpent: purchases.reduce((sum, record) => sum + record.total, 0),
      totalOrders: purchases.length,
      totalItems: purchases.reduce((sum, record) => sum + record.quantity, 0),
      bestProducts: [...products]
        .sort((a, b) => (b.averageRating - a.averageRating) || (b.ratingCount - a.ratingCount) || (b.total - a.total))
        .slice(0, 4),
      mostBoughtProducts: [...products]
        .sort((a, b) => (b.quantity - a.quantity) || (b.total - a.total))
        .slice(0, 4),
    };
  }

  getSellerAnalytics(sellerId: string, products: Product[]): SellerProfileAnalytics {
    const productIds = new Set(products.map(product => product.id));
    const purchases = this.getRecords().filter(record => record.sellerId === sellerId || productIds.has(record.productId));
    const productsSold = this.groupRecordsByProduct(purchases);

    return {
      totalGained: purchases.reduce((sum, record) => sum + record.total, 0),
      totalOrders: purchases.length,
      totalItemsSold: purchases.reduce((sum, record) => sum + record.quantity, 0),
      bestSellingProducts: productsSold
        .sort((a, b) => (b.quantity - a.quantity) || (b.total - a.total))
        .slice(0, 5),
    };
  }

  private getRecords(): PurchaseRecord[] {
    try {
      const rawRecords = localStorage.getItem(this.storageKey);
      return rawRecords ? JSON.parse(rawRecords) as PurchaseRecord[] : [];
    } catch {
      return [];
    }
  }

  private saveRecords(records: PurchaseRecord[]): void {
    localStorage.setItem(this.storageKey, JSON.stringify(records));
  }

  private groupRecordsByProduct(records: PurchaseRecord[]): ProductAnalyticsItem[] {
    const grouped = new Map<string, ProductAnalyticsItem>();

    for (const record of records) {
      const current = grouped.get(record.productId);
      if (current) {
        current.quantity += record.quantity;
        current.total += record.total;
        current.averageRating = Math.max(current.averageRating, record.averageRating);
        current.ratingCount = Math.max(current.ratingCount, record.ratingCount);
        continue;
      }

      grouped.set(record.productId, {
        productId: record.productId,
        name: record.productName,
        image: record.productImage,
        category: record.productCategory,
        quantity: record.quantity,
        total: record.total,
        averageRating: record.averageRating,
        ratingCount: record.ratingCount,
      });
    }

    return [...grouped.values()];
  }
}
