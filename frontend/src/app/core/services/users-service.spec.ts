import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UsersService, LoginPayload, LoginResponse, RegisterPayload, Me, UpdateProfile, ApiMessageResponse } from './users-service';

describe('UsersService', () => {
  let service: UsersService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        UsersService
      ]
    });

    service = TestBed.inject(UsersService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ============================================
  // SERVICE CREATION
  // ============================================

  it('should create the users service', () => {
    expect(service).toBeDefined();
  });

  // ============================================
  // LOGIN TESTS
  // ============================================

  it('should login user with credentials', () => {
    const loginPayload: LoginPayload = {
      identification: 'user@test.com',
      password: 'password123'
    }

    const mockResponse: LoginResponse = {
      token: 'jwt_token_123',
      role: 'USER',
      message: 'Login successful'
    };

    service.loginUser(loginPayload).subscribe((response) => {
      expect(response.token).toBe('jwt_token_123');
      expect(response.role).toBe('USER');
    });

    const req = httpMock.expectOne('/api/users/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(loginPayload);
    req.flush(mockResponse);
  });

  it('should handle login error', () => {
    const loginPayload: LoginPayload = {
      identification: 'wrong@test.com',
      password: 'wrongpass'
    };

    service.loginUser(loginPayload).subscribe({
      next: () => expect(true).toBe(false),
      error: (error) => {
        expect(error.status).toBe(401);
      }
    });

    const req = httpMock.expectOne('/api/users/login');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });

  // ============================================
  // GET ME TESTS
  // ============================================

  it('should get current user info', () => {
    const mockMe: Me = {
      id: '1',
      username: 'testuser',
      email: 'test@test.com',
      role: 'USER',
      avatarUrl: 'avatar.jpg'
    };

    service.meUser().subscribe((response) => {
      expect(response.id).toBe('1');
      expect(response.username).toBe('testuser');
    });

    const req = httpMock.expectOne('/api/users/me');
    expect(req.request.method).toBe('GET');
    req.flush(mockMe);
  });

  // ============================================
  // REGISTER TESTS
  // ============================================

  it('should register user with avatar url', () => {
    const registerPayload: RegisterPayload = {
      email: 'newuser@test.com',
      name: 'New User',
      password: 'password123',
      role: 'ROLE_GUEST',
      avatarUrl: 'avatar.jpg'
    };

    const mockResponse: ApiMessageResponse = {
      msg: 'User registered successfully'
    };

    service.registerUser(registerPayload).subscribe((response) => {
      expect(response.msg).toBe('User registered successfully');
    });

    const req = httpMock.expectOne('/api/users/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(registerPayload);
    req.flush(mockResponse);
  });

  // ============================================
  // AVATAR TESTS
  // ============================================

  it('should get avatar as blob', () => {
    const avatarName = 'avatar_123.jpg';
    const mockBlob = new Blob(['image data'], { type: 'image/jpeg' });

    service.getAvatar(avatarName).subscribe((response) => {
      expect(response).toBeInstanceOf(Blob);
      expect(response.type).toBe('image/jpeg');
    });

    const req = httpMock.expectOne(`/api/media/users/${avatarName}`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(mockBlob);
  });

  it('should handle avatar not found error', () => {
    const avatarName = 'nonexistent.jpg';
    let errorReceived = false;

    service.getAvatar(avatarName).subscribe({
      next: () => expect(true).toBe(false),
      error: (error) => {
        expect(error.status).toBe(404);
        errorReceived = true;
      }
    });

    const req = httpMock.expectOne(`/api/media/users/${avatarName}`);
    req.error(new ProgressEvent('error'), { status: 404, statusText: 'Not Found' });
    expect(errorReceived).toBe(true);
  });

  // ============================================
  // UPLOAD AVATAR TESTS
  // ============================================

  it('should upload user avatar', () => {
    const mockFile = new File(['avatar data'], 'avatar.jpg', { type: 'image/jpeg' });
    const mockResponse: ApiMessageResponse = {
      msg: 'Avatar uploaded successfully'
    };

    service.registerUserWithAvatar(mockFile).subscribe((response) => {
      expect(response.msg).toBe('Avatar uploaded successfully');
    });

    const req = httpMock.expectOne('/api/media/users');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    req.flush(mockResponse);
  });

  it('should upload without avatar when null', () => {
    const mockResponse: ApiMessageResponse = {
      msg: 'Request processed'
    };

    service.registerUserWithAvatar(null).subscribe((response) => {
      expect(response.msg).toBe('Request processed');
    });

    const req = httpMock.expectOne('/api/media/users');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should upload without avatar when undefined', () => {
    const mockResponse: ApiMessageResponse = {
      msg: 'Request processed'
    };

    service.registerUserWithAvatar(undefined).subscribe((response) => {
      expect(response.msg).toBe('Request processed');
    });

    const req = httpMock.expectOne('/api/media/users');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  // ============================================
  // UPDATE USER TESTS
  // ============================================

  it('should update user profile', () => {
    const updatePayload: UpdateProfile = {
      name: 'Updated Name',
      email: 'updated@test.com',
      uuid: '1'
    };

    const mockMe: Me = {
      id: '1',
      username: 'testuser',
      email: 'updated@test.com',
      role: 'USER',
      avatarUrl: 'avatar.jpg'
    };

    service.updateUser(updatePayload).subscribe((response) => {
      expect(response.email).toBe('updated@test.com');
    });

    const req = httpMock.expectOne('/api/users/me');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(updatePayload);
    req.flush(mockMe);
  });

  it('should handle update user error', () => {
    const updatePayload: UpdateProfile = {
      name: 'New Name',
      email: 'new@test.com',
      uuid: '1'
    };

    service.updateUser(updatePayload).subscribe({
      next: () => expect(true).toBe(false),
      error: (error) => {
        expect(error.status).toBe(400);
      }
    });

    const req = httpMock.expectOne('/api/users/me');
    req.flush('Bad Request', { status: 400, statusText: 'Bad Request' });
  });

  // ============================================
  // EDGE CASES
  // ============================================

  it('should handle user with special characters in name', () => {
    const registerPayload: RegisterPayload = {
      email: 'user@test.com',
      name: "O'Brien-Smith",
      password: 'pass123',
      role: 'ROLE_GUEST',
      avatarUrl: ''
    };

    service.registerUser(registerPayload).subscribe();

    const req = httpMock.expectOne('/api/users/register');
    expect(req.request.body.name).toBe("O'Brien-Smith");
    req.flush({ msg: 'Success' });
  });

  it('should handle empty avatar url', () => {
    const registerPayload: RegisterPayload = {
      email: 'user@test.com',
      name: 'User',
      password: 'pass123',
      role: 'ROLE_GUEST',
      avatarUrl: ''
    };

    service.registerUser(registerPayload).subscribe();

    const req = httpMock.expectOne('/api/users/register');
    expect(req.request.body.avatarUrl).toBe('');
    req.flush({ msg: 'Success' });
  });
});
