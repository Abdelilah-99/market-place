import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { StateService } from './state-service';
import { Me } from './users-service';

describe('StateService', () => {
  let service: StateService;
  let httpMock: HttpTestingController;

  const mockUser: Me = {
    id: '1',
    username: 'testuser',
    email: 'test@example.com',
    role: 'USER',
    avatarUrl: 'avatar.jpg'
  };

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        StateService
      ]
    });

    service = TestBed.inject(StateService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  // ============================================
  // SERVICE CREATION
  // ============================================

  it('should create the state service', () => {
    expect(service).toBeDefined();
  });

  // ============================================
  // OBSERVABLE TESTS
  // ============================================

  it('should provide currentUser as observable', () => {
    let userReceived = false;
    service.currentUser$.subscribe((user) => {
      if (user) {
        expect(user.id).toBe('1');
        userReceived = true;
      }
    });

    service.setUser(mockUser);
    expect(userReceived).toBe(true);
  });

  // ============================================
  // GET MY INFO TESTS
  // ============================================

  it('should fetch user info and update state', () => {
    service.getMyInfo();

    const req = httpMock.expectOne('/api/users/me');
    expect(req.request.method).toBe('GET');
    req.flush(mockUser);

    expect(service.currentUserSubject.value).toEqual(mockUser);
  });

  it('should save user to localStorage after fetching', () => {
    service.getMyInfo();

    const req = httpMock.expectOne('/api/users/me');
    req.flush(mockUser);

    const storedUser = localStorage.getItem('user');
    expect(storedUser).toBeTruthy();
    expect(JSON.parse(storedUser!)).toEqual(mockUser);
  });

  it('should handle getMyInfo error gracefully', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    service.getMyInfo();

    const req = httpMock.expectOne('/api/users/me');
    req.error(new ErrorEvent('Network error'));

    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  // ============================================
  // SET USER TESTS
  // ============================================

  it('should set user and save to localStorage', () => {
    service.setUser(mockUser);

    expect(service.currentUserSubject.value).toEqual(mockUser);
    expect(localStorage.getItem('user')).toBe(JSON.stringify(mockUser));
  });

  it('should emit user through observable when setting', () => {
    let userReceived = false;
    service.currentUser$.subscribe((user) => {
      if (user) {
        expect(user.username).toBe('testuser');
        userReceived = true;
      }
    });

    service.setUser(mockUser);
    expect(userReceived).toBe(true);
  });

  it('should set null user', () => {
    service.setUser(null);

    expect(service.currentUserSubject.value).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
  });

  it('should not save null user to localStorage', () => {
    localStorage.setItem('user', JSON.stringify({ id: '1', username: 'test' }));

    service.setUser(null);

    expect(localStorage.getItem('user')).toBeNull();
  });

  // ============================================
  // CLEAR USER TESTS
  // ============================================

  it('should clear user from state and localStorage', () => {
    service.setUser(mockUser);
    expect(localStorage.getItem('user')).toBeTruthy();

    service.clearUser();

    expect(service.currentUserSubject.value).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
  });

  it('should emit null through observable when clearing', () => {
    service.setUser(mockUser);
    service.clearUser();

    expect(service.currentUserSubject.value).toBeNull();
  });

  // ============================================
  // PERSISTENCE TESTS
  // ============================================

  it('should load user from localStorage on init', () => {
    localStorage.setItem('user', JSON.stringify(mockUser));

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        StateService
      ]
    });

    const newService = TestBed.inject(StateService);

    expect(newService.currentUserSubject.value).toEqual(mockUser);
  });

  // ============================================
  // MULTIPLE UPDATES TESTS
  // ============================================

  it('should handle multiple user updates', () => {
    const user1: Me = { id: '1', username: 'user1', email: 'user1@test.com', role: 'USER', avatarUrl: '' };
    const user2: Me = { id: '2', username: 'user2', email: 'user2@test.com', role: 'SELLER', avatarUrl: '' };

    service.setUser(user1);
    expect(service.currentUserSubject.value).toEqual(user1);

    service.setUser(user2);
    expect(service.currentUserSubject.value).toEqual(user2);
    expect(JSON.parse(localStorage.getItem('user')!)).toEqual(user2);
  });
});
