import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ProductsService } from './products-service';
import { ApiResponse } from '../models/ApiResponse';
import { Product } from '../models/Product';

describe('ProductsService', () => {
  let service: ProductsService;
  let httpMock: HttpTestingController;
  const apiUrl = '/api/products';

  const mockProduct: Product = {
    id: '1',
    name: 'Test Product',
    description: 'Test Description',
    price: 99.99,
    image: 'image.jpg',
    userId: 'user1'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ProductsService]
    });

    service = TestBed.inject(ProductsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ============================================
  // SERVICE CREATION
  // ============================================

  it('should create the products service', () => {
    expect(service).toBeDefined();
  });

  // ============================================
  // CREATE PRODUCT TESTS
  // ============================================

  it('should create a product', () => {
    const newProduct: Partial<Product> = {
      name: 'New Product',
      description: 'Description',
      price: 50.00
    };

    const mockResponse: ApiResponse<Product> = {
      data: { ...newProduct, id: '1' } as Product,
    } as any;

    service.createProduct(newProduct).subscribe((response) => {
      expect(response.data!.name).toBe('New Product');
      expect(response.data!.id).toBe('1');
    });

    const req = httpMock.expectOne(`${apiUrl}/`);
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('Content-Type')).toBe('application/json');
    expect(req.request.body).toEqual(newProduct);
    req.flush(mockResponse);
  });

  it('should send correct headers when creating product', () => {
    service.createProduct({ name: 'Product' }).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/`);
    expect(req.request.headers.get('Content-Type')).toBe('application/json');
    req.flush({});
  });

  // ============================================
  // GET ALL PRODUCTS TESTS
  // ============================================

  it('should fetch all products', () => {
    const mockProducts: Product[] = [mockProduct];
    const mockResponse: ApiResponse<Product[]> = {
      data: mockProducts,
    } as any;

    service.getAllProducts().subscribe((response) => {
      expect(response.data).toHaveLength(1);
      expect(response.data![0].name).toBe('Test Product');
    });

    const req = httpMock.expectOne(`${apiUrl}/`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should handle empty products list', () => {
    const mockResponse: ApiResponse<Product[]> = {
      data: [],
    } as any;

    service.getAllProducts().subscribe((response) => {
      expect(response.data).toHaveLength(0);
    });

    const req = httpMock.expectOne(`${apiUrl}/`);
    req.flush(mockResponse);
  });

  // ============================================
  // GET MY PRODUCTS TESTS
  // ============================================

  it('should fetch user\'s own products', () => {
    const mockProducts: Product[] = [mockProduct];
    const mockResponse: ApiResponse<Product[]> = {
      data: mockProducts,
    } as any;

    service.getMyProducts().subscribe((response) => {
      expect(response.data).toHaveLength(1);
    });

    const req = httpMock.expectOne(`${apiUrl}/me`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  // ============================================
  // DELETE PRODUCT TESTS
  // ============================================

  it('should delete a product by id', () => {
    const productId = '1';
    const mockResponse: ApiResponse<any> = { data: {} } as any;

    service.deleteProducts(productId).subscribe((response) => {
      expect(response).toBeDefined();
    });

    const req = httpMock.expectOne(`${apiUrl}/${productId}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(mockResponse);
  });

  it('should handle delete error gracefully', () => {
    const productId = '999';

    service.deleteProducts(productId).subscribe({
      next: () => expect(true).toBe(false),
      error: (error) => {
        expect(error.status).toBe(404);
      }
    });

    const req = httpMock.expectOne(`${apiUrl}/${productId}`);
    req.flush('Not Found', { status: 404, statusText: 'Not Found' });
  });

  // ============================================
  // UPDATE PRODUCT TESTS
  // ============================================

  it('should update a product', () => {
    const productId = '1';
    const updatedProduct: Product = {
      ...mockProduct,
      name: 'Updated Product'
    };

    const mockResponse: ApiResponse<Product> = {
      data: updatedProduct,
    } as any;

    service.updateProduct(productId, updatedProduct).subscribe((response) => {
      expect(response.data!.name).toBe('Updated Product');
    });

    const req = httpMock.expectOne(`${apiUrl}/${productId}`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(updatedProduct);
    req.flush(mockResponse);
  });

  it('should send correct headers when updating product', () => {
    service.updateProduct('1', mockProduct).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/1`);
    expect(req.request.headers.get('Content-Type')).toBe('application/json');
    req.flush({});
  });

  it('should handle update error', () => {
    const productId = '1';

    service.updateProduct(productId, mockProduct).subscribe({
      next: () => expect(true).toBe(false),
      error: (error) => {
        expect(error.status).toBe(400);
      }
    });

    const req = httpMock.expectOne(`${apiUrl}/${productId}`);
    req.flush('Bad Request', { status: 400, statusText: 'Bad Request' });
  });

  // ============================================
  // EDGE CASE TESTS
  // ============================================

  it('should handle product with zero price', () => {
    const freeProduct: Partial<Product> = {
      name: 'Free Product',
      price: 0
    };

    service.createProduct(freeProduct).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/`);
    expect(req.request.body.price).toBe(0);
    req.flush({ data: freeProduct });
  });

  it('should handle product with special characters in name', () => {
    const specialProduct: Partial<Product> = {
      name: 'Product with "quotes" & special chars',
      description: 'Test <html> tags'
    };

    service.createProduct(specialProduct).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/`);
    expect(req.request.body.name).toContain('quotes');
    req.flush({ data: specialProduct });
  });
});
