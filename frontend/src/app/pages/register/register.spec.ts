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
    let router: Router;
    let navigateSpy: ReturnType<typeof vi.spyOn>;

    beforeEach(async () => {
        mockUsersService = {
            registerUser: vi.fn()
        };

        await TestBed.configureTestingModule({
            imports: [Register],
            providers: [
                { provide: UsersService, useValue: mockUsersService },
                provideRouter([])
            ]
        }).compileComponents();

        router = TestBed.inject(Router);
        navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

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
        expect(navigateSpy).toHaveBeenCalledWith(['/login']);
        expect(component.isSubmitting).toBe(false);
    });
});
