import { describe, it, expect, beforeEach, vi } from 'vitest';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth-service';
import { ApiResponse } from '../models/ApiResponse';
import { User } from '../models/User';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  const apiUrl = '/api/users';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ============================================
  // LOGIN TESTS
  // ============================================

  it('should create the auth service', () => {
    expect(service).toBeDefined();
  });

  it('should call login endpoint with correct credentials', () => {
    const email = 'test@example.com';
    const password = 'password123';
    const mockUser: User = { id: '1', username: 'testuser', role: 'ROLE_BUYER' as any };
    const mockResponse: ApiResponse<User> = {
      data: mockUser,
    } as any;

    service.login(email, password).subscribe((response) => {
      expect(response.username).toBe('testuser');
    });

    const req = httpMock.expectOne(`${apiUrl}/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('Authorization')).toBeTruthy();
    req.flush(mockResponse);
  });

  it('should encode credentials in Basic Auth format', () => {
    const email = 'user@test.com';
    const password = 'pass123';
    const mockResponse: ApiResponse<User> = {
      data: { id: '1', username: 'user', email },
    } as any;

    service.login(email, password).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/login`);
    const expectedAuth = `Basic ${btoa(`${email}:${password}`)}`;
    expect(req.request.headers.get('Authorization')).toBe(expectedAuth);
    req.flush(mockResponse);
  });

  // ============================================
  // REGISTER TESTS
  // ============================================

  it('should call register endpoint with correct credentials', () => {
    const username = 'newuser';
    const email = 'newuser@example.com';
    const password = 'password123';
    const mockUser: User = { id: '1', username, role: 'ROLE_GUEST' as any };
    const mockResponse: ApiResponse<User> = {
      data: mockUser,
    } as any;

    service.register(username, email, password).subscribe((response) => {
      expect(response.username).toBe(username);
    });

    const req = httpMock.expectOne(`${apiUrl}/register`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      username,
      email,
      password,
      role: 'ROLE_GUEST'
    });
    req.flush(mockResponse);
  });

  it('should register with custom role', () => {
    const username = 'seller';
    const email = 'seller@example.com';
    const password = 'password123';
    const role = 'ROLE_SELLER';
    const mockResponse: ApiResponse<User> = {
      data: { id: '1', username, email },
    } as any;

    service.register(username, email, password, role).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/register`);
    expect(req.request.body.role).toBe(role);
    req.flush(mockResponse);
  });

  it('should return default role ROLE_GUEST when not specified', () => {
    const username = 'user';
    const email = 'user@example.com';
    const password = 'pass123';
    const mockResponse: ApiResponse<User> = {
      data: { id: '1', username, email },
    } as any;

    service.register(username, email, password).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/register`);
    expect(req.request.body.role).toBe('ROLE_GUEST');
    req.flush(mockResponse);
  });

  // ============================================
  // ERROR HANDLING TESTS
  // ============================================

  it('should handle login error', () => {
    const email = 'test@example.com';
    const password = 'wrongpass';

    service.login(email, password).subscribe({
      next: () => expect(true).toBe(false),
      error: (error) => {
        expect(error.status).toBe(401);
      }
    });

    const req = httpMock.expectOne(`${apiUrl}/login`);
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });

  it('should handle register error', () => {
    const mockResponse: ApiResponse<User> = {
      data: {} as User,
    } as any;

    service.register('user', 'user@test.com', 'pass').subscribe({
      next: () => expect(true).toBe(false),
      error: (error) => {
        expect(error.status).toBe(400);
      }
    });

    const req = httpMock.expectOne(`${apiUrl}/register`);
    req.flush('Bad Request', { status: 400, statusText: 'Bad Request' });
  });

  // ============================================
  // RESPONSE MAPPING TESTS
  // ============================================

  it('should extract data from ApiResponse on login', () => {
    const email = 'test@example.com';
    const password = 'pass123';
    const userData: User = { id: '1', username: 'testuser', role: 'ROLE_BUYER' as any };
    const mockResponse: ApiResponse<User> = {
      data: userData,
    } as any;

    service.login(email, password).subscribe((response) => {
      expect(response).toEqual(userData);
      expect(response.id).toBe('1');
    });

    const req = httpMock.expectOne(`${apiUrl}/login`);
    req.flush(mockResponse);
  });

  it('should extract data from ApiResponse on register', () => {
    const username = 'newuser';
    const email = 'new@example.com';
    const password = 'pass123';role: 'USER'
    const userData: User = { id: '1', username, role: 'ROLE_BUYER' as any };
    const mockResponse: ApiResponse<User> = {
      data: userData,
    } as any;

    service.register(username, email, password).subscribe((response) => {
      expect(response).toEqual(userData);
    });

    const req = httpMock.expectOne(`${apiUrl}/register`);
    req.flush(mockResponse);
  });
});
