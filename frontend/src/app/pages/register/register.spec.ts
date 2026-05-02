import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of } from 'rxjs';
import { Register } from './register';
import { UsersService } from '../../core/services/users-service';

describe('Register Component integration', () => {
    let fixture: ComponentFixture<Register>;
    let component: Register;
    let mockUsersService: { registerUser: ReturnType<typeof vi.fn> };
    let mockRouter: { navigateByUrl: ReturnType<typeof vi.fn> };

    beforeEach(async () => {
        mockUsersService = {
            registerUser: vi.fn()
        };

        mockRouter = {
            navigateByUrl: vi.fn()
        };

        await TestBed.configureTestingModule({
            imports: [Register],
            providers: [
                { provide: UsersService, useValue: mockUsersService },
                { provide: Router, useValue: mockRouter },
                provideRouter([])
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(Register);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('renders the registration form and submits the full flow', () => {
        const emailInput = fixture.nativeElement.querySelector('#email') as HTMLInputElement;
        const usernameInput = fixture.nativeElement.querySelector('#username') as HTMLInputElement;
        const passwordInput = fixture.nativeElement.querySelector('#password') as HTMLInputElement;
        const confirmPasswordInput = fixture.nativeElement.querySelector('#confirmPassword') as HTMLInputElement;
        const roleSelect = fixture.nativeElement.querySelector('#role') as HTMLSelectElement;
        const submitButton = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
        const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;

        expect(emailInput).toBeTruthy();
        expect(usernameInput).toBeTruthy();
        expect(submitButton.disabled).toBe(true);

        mockUsersService.registerUser.mockReturnValue(of({ msg: 'Registration successful' }));

        emailInput.value = 'new@user.com';
        emailInput.dispatchEvent(new Event('input'));
        usernameInput.value = 'newuser';
        usernameInput.dispatchEvent(new Event('input'));
        passwordInput.value = 'password123';
        passwordInput.dispatchEvent(new Event('input'));
        confirmPasswordInput.value = 'password123';
        confirmPasswordInput.dispatchEvent(new Event('input'));
        roleSelect.value = 'BUYER';
        roleSelect.dispatchEvent(new Event('change'));
        fixture.detectChanges();

        expect(submitButton.disabled).toBe(false);

        form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
        fixture.detectChanges();

        expect(mockUsersService.registerUser).toHaveBeenCalledWith({
            email: 'new@user.com',
            name: 'newuser',
            password: 'password123',
            role: 'BUYER',
            avatarUrl: ''
        });
        expect(component.message).toBe('Registration successful');
        expect(mockRouter.navigateByUrl).toHaveBeenCalledWith('/login');
        expect(component.isSubmitting).toBe(false);
    });
});
