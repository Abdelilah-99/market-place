import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ApiResponse } from '../models/ApiResponse';
import { Product } from '../models/Product';
import { Observable } from 'rxjs';

export interface ProductSearchResponse {
  items: Product[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface ProductSearchParams {
  q?: string;
  category?: string;
  minPrice?: number | null;
  maxPrice?: number | null;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ProductRatingStats {
  average: number;
  count: number;
  breakdown: Record<string, number>;
  myRating?: number | null;
}

@Injectable({
  providedIn: 'root',
})
export class ProductsService {
  private apiUrl = '/api/products';

  constructor(private http: HttpClient) { }

  createProduct(post: Partial<Product>): Observable<ApiResponse<Product>> {
    return this.http.post<ApiResponse<Product>>(
      `${this.apiUrl}/`,
      post,
      {
        headers: { 'Content-Type': 'application/json' }
      }
    );
  }

  getAllProducts(): Observable<ApiResponse<Product[]>> {
    return this.http.get<ApiResponse<Product[]>>(
      `${this.apiUrl}/`
    );
  }

  searchProducts(params: ProductSearchParams): Observable<ApiResponse<ProductSearchResponse>> {
    const query: Record<string, string> = {};

    if (params.q) query['q'] = params.q;
    if (params.category) query['category'] = params.category;
    if (params.minPrice !== null && params.minPrice !== undefined) query['minPrice'] = String(params.minPrice);
    if (params.maxPrice !== null && params.maxPrice !== undefined) query['maxPrice'] = String(params.maxPrice);
    query['page'] = String(params.page ?? 0);
    query['size'] = String(params.size ?? 12);
    query['sort'] = params.sort ?? 'newest';

    return this.http.get<ApiResponse<ProductSearchResponse>>(
      `${this.apiUrl}/search`,
      { params: query }
    );
  }

  getProduct(id: string): Observable<ApiResponse<Product>> {
    return this.http.get<ApiResponse<Product>>(
      `${this.apiUrl}/${id}`
    );
  }

  getProductRatings(id: string): Observable<ApiResponse<ProductRatingStats>> {
    return this.http.get<ApiResponse<ProductRatingStats>>(
      `${this.apiUrl}/${id}/ratings`
    );
  }

  rateProduct(id: string, stars: number): Observable<ApiResponse<ProductRatingStats>> {
    return this.http.post<ApiResponse<ProductRatingStats>>(
      `${this.apiUrl}/${id}/ratings`,
      { stars },
      {
        headers: { 'Content-Type': 'application/json' }
      }
    );
  }

  getMyProducts() {
    return this.http.get<ApiResponse<Product[]>>(
      `${this.apiUrl}/me`
    );
  }

  deleteProducts(id: string) {
    return this.http.delete<ApiResponse<any>>(
      `${this.apiUrl}/${id}`
    );
  }

  updateProduct(id: string, product: Product) {
    return this.http.put<ApiResponse<Product>>(
      `${this.apiUrl}/${id}`,
      product,
      {
        headers: { 'Content-Type': 'application/json' }
      }
    );
  }
}
