import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Login } from './login';
import { of, throwError } from 'rxjs';
// import { ɵPLATFORM_BROWSER_ID } from '@angular/common';

describe('Login Component - Simple Tests', () => {
    let component: Login;
    let mockUsersService: any;
    let mockStateService: any;
    let mockRouter: any;

    beforeEach(() => {
        // Create simple mock objects with mock functions
        mockUsersService = {
            loginUser: vi.fn()
        };

        mockStateService = {
            getMyInfo: vi.fn()
        };

        mockRouter = {
            navigateByUrl: vi.fn()
        };

        // Create the component with mocked dependencies

        component = new Login(
            mockUsersService,
            mockStateService,
            mockRouter,
            { useValue: 'browser' }
        );
    });

    // ============================================
    // BASIC TESTS - Understanding the component   
    // ============================================

    it('should create the login component', () => {
        expect(component).toBeDefined();
    });

    it('should have empty error message on init', () => {
        expect(component.errorMessage()).toBe('');
    });

    it('should not be submitting on init', () => {
        expect(component.isSubmitting()).toBe(false);
    });

    it('should have empty user credentials on init', () => {
        expect(component.user.identification).toBe('');
        expect(component.user.password).toBe('');
    });

    // ============================================
    // FORM VALIDATION TESTS                       
    // ============================================

    it('should NOT call loginUser if form is invalid', () => {
        const mockForm = { invalid: true } as any;
        component.onSubmit(mockForm);
        expect(mockUsersService.loginUser).not.toHaveBeenCalled();
    });

    it('should NOT submit if already submitting', () => {
        component.isSubmitting.set(true);
        const mockForm = { invalid: false } as any;

        component.onSubmit(mockForm);
        expect(mockUsersService.loginUser).not.toHaveBeenCalled();
    });

    // ============================================
    // SUCCESSFUL LOGIN TESTS
    // ============================================

    it('should call loginUser with correct credentials', () => {
        const mockForm = { invalid: false, resetForm: vi.fn() } as any;
        const credentials = { identification: 'user@test.com', password: 'pass123' };

        mockUsersService.loginUser.mockReturnValue(
            of({ message: 'Success', token: 'token123', role: 'user' })
        );

        component.user = credentials;
        component.onSubmit(mockForm);

        expect(mockUsersService.loginUser).toHaveBeenCalledWith(credentials);
    });

    it('should set success message on login', () => {
        const mockForm = { invalid: false, resetForm: vi.fn() } as any;

        mockUsersService.loginUser.mockReturnValue(
            of({ message: 'Login successful', token: 'token123', role: 'user' })
        );

        component.onSubmit(mockForm);

        expect(component.message).toBe('Login successful');
    });

    it('should navigate to home after successful login', () => {
        const mockForm = { invalid: false, resetForm: vi.fn() } as any;

        mockUsersService.loginUser.mockReturnValue(
            of({ message: 'Success', token: 'token123', role: 'user' })
        );

        component.onSubmit(mockForm);

        expect(mockRouter.navigateByUrl).toHaveBeenCalledWith('/');
    });

    it('should reset form after successful login', () => {
        const mockForm = { invalid: false, resetForm: vi.fn() } as any;

        mockUsersService.loginUser.mockReturnValue(
            of({ message: 'Success', token: 'token123', role: 'user' })
        );

        component.onSubmit(mockForm);

        expect(mockForm.resetForm).toHaveBeenCalled();
    });

    // ============================================
    // LOGIN ERROR TESTS
    // ============================================

    it('should set error message on login failure', () => {
        const mockForm = { invalid: false, resetForm: vi.fn() } as any;
        const mockError = { error: { message: 'Invalid credentials' } };

        mockUsersService.loginUser.mockReturnValue(throwError(() => mockError));

        component.onSubmit(mockForm);

        expect(component.errorMessage()).toBe('Invalid credentials');
    });

    it('should use default error message if error has no message', () => {
        const mockForm = { invalid: false, resetForm: vi.fn() } as any;
        const mockError = { error: {} };

        mockUsersService.loginUser.mockReturnValue(throwError(() => mockError));

        component.onSubmit(mockForm);

        expect(component.errorMessage()).toBe(
            'Login failed. Please verify your credentials.'
        );
    });

    it('should NOT navigate on login error', () => {
        const mockForm = { invalid: false, resetForm: vi.fn() } as any;
        const mockError = { error: { message: 'Error' } };

        mockUsersService.loginUser.mockReturnValue(throwError(() => mockError));

        component.onSubmit(mockForm);

        expect(mockRouter.navigateByUrl).not.toHaveBeenCalled();
    });

    // ============================================
    // LOADING STATE TESTS
    // ============================================

    it('should set isSubmitting to false after login completes', () => {
        const mockForm = { invalid: false, resetForm: vi.fn() } as any;

        mockUsersService.loginUser.mockReturnValue(
            of({ message: 'Success', token: 'token123', role: 'user' })
        );

        component.onSubmit(mockForm);

        expect(component.isSubmitting()).toBe(false);
    });

    it('should set isSubmitting to false after login error', () => {
        const mockForm = { invalid: false, resetForm: vi.fn() } as any;
        const mockError = { error: { message: 'Error' } };

        mockUsersService.loginUser.mockReturnValue(throwError(() => mockError));

        component.onSubmit(mockForm);

        expect(component.isSubmitting()).toBe(false);
    });
});
