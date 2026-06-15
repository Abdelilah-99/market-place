import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, Inject, PLATFORM_ID, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UsersService } from '../../core/services/users-service';
import { StateService } from '../../core/services/state-service';
import { TypographyComponent } from '../../shared/components/typography/typography.component';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { SectionComponent } from '../../shared/components/section/section.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink, TypographyComponent, ButtonComponent, SectionComponent],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  message = '';
  // errorMessage = '';
  errorMessage = signal("");
  // isSubmitting = false;
  isSubmitting = signal(false);

  user = {
    identification: '',
    password: ''
  };

  constructor(
    private loginService: UsersService,
    private StateService: StateService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: object
  ) { }

  onSubmit(form: NgForm): void {
    if (form.invalid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true)
    this.message = '';
    // this.errorMessage = '';
    this.errorMessage.set("");

    this.loginService.loginUser(this.user).subscribe({
      next: (res) => {
        this.message = res.message || 'Login successful.';
        if (isPlatformBrowser(this.platformId)) {
          localStorage.setItem('token', res.token);
          localStorage.setItem('role', res.role);
        }

        this.StateService.getMyInfo();

        form.resetForm({ identification: '', password: '' });
        // this.isSubmitting = false;
        this.isSubmitting.set(false)
        this.router.navigateByUrl('/');
      },
      error: (err) => {
        // this.errorMessage = err?.error?.message || err?.error?.msg || 'Login failed. Please verify your credentials.';
        // this.isSubmitting = false;
        console.error("error ll");

        this.errorMessage.set(err?.error?.message || err?.error?.msg || 'Login failed. Please verify your credentials.');
        this.isSubmitting.set(false)
      }
    });
  }
}
