import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UsersService } from '../../core/services/users-service';
import { TypographyComponent } from '../../shared/components/typography/typography.component';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { SectionComponent } from '../../shared/components/section/section.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    RouterLink, 
    TypographyComponent, 
    ButtonComponent, 
    SectionComponent
  ],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  message = '';
  isSubmitting = false;
  errorMessage = signal({ msg: '', isthere: false })
  selectedAvatar: File | null = null;
  isAvatarSelected = signal(false);
  imageName: string = "";
  user = {
    email: '',
    name: '',
    password: '',
    confirmPassword: '',
    role: 'BUYER'
  };

  constructor(private loginService: UsersService, private router: Router) { }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (file && file.size > 1024 * 1024 * 2) {
      this.errorMessage.set({ msg: 'File size must not exceed 2MB.', isthere: true });
      return;
    }
    if (file && !['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
      this.errorMessage.set({ msg: 'File must be a PNG, JPEG, or WebP image.', isthere: true });
      return;
    }

    this.isAvatarSelected.set(!!file);
    this.selectedAvatar = file;
  }

  onSubmit(form: NgForm): void {
    if (form.invalid || this.isSubmitting) return;

    if (this.user.password !== this.user.confirmPassword) {
      this.errorMessage.set({ msg: 'Passwords do not match.', isthere: true });
      return;
    }

    this.isSubmitting = true;
    this.errorMessage.set({ msg: '', isthere: false });

    const register = (avatarUrl: string = '') => {
      this.loginService.registerUser({
        email: this.user.email,
        name: this.user.name,
        password: this.user.password,
        role: this.user.role,
        avatarUrl
      }).subscribe({
        next: (res) => {
          this.message = res.msg || 'Registration successful. You can login now.';
          this.isSubmitting = false;
          this.selectedAvatar = null;
          this.isAvatarSelected.set(false);
          form.resetForm({ email: '', name: '', password: '', confirmPassword: '', role: 'BUYER' });
          this.router.navigate(['/login']);
        },
        error: (err) => {
          this.errorMessage.set({
            msg: err?.error?.message || err?.error?.msg || 'Registration failed. Please try again.',
            isthere: true
          });
          this.isSubmitting = false;
        }
      });
    };

    if (this.isAvatarSelected() && this.selectedAvatar) {
      this.loginService.uploadAvatar(this.selectedAvatar).subscribe({
        next: (avatarUrl) => register(avatarUrl),
        error: (err) => {
          console.error(err);
          this.errorMessage.set({ msg: 'Avatar upload failed. Please try again.', isthere: true });
          this.isSubmitting = false;
        }
      });
    } else {
      register();
    }
  }
}
