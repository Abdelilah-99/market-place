import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, OnInit, Signal, signal } from '@angular/core';
import { Me, UpdateProfile, UsersService } from '../../core/services/users-service';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { PLATFORM_ID, Inject } from '@angular/core';
import { of, switchMap } from 'rxjs';
import { TypographyComponent } from '../../shared/components/typography/typography.component';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { SectionComponent } from '../../shared/components/section/section.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    TypographyComponent, 
    ButtonComponent, 
    SectionComponent, 
    BadgeComponent
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile implements OnInit, OnDestroy {
  userProfile = signal<Me | null>(null);
  isLoading = signal(true);
  isEditing = signal(false);
  isSubmitting = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  editFormName = signal('');
  editFormEmail = signal('');
  editFormAvatarUrl = signal('');
  profileAvatarSrc = signal('');
  selectedAvatar = signal<File | null>(null);
  avatarName: string = '';
  constructor(
    private userService: UsersService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: object
  ) { }

  ngOnInit(): void {
    this.loadProfile();
  }

  ngOnDestroy(): void {
    if (isPlatformBrowser(this.platformId) && this.profileAvatarSrc()) {
      URL.revokeObjectURL(this.profileAvatarSrc());
    }
  }

  loadProfileImg(avatarId: string): void {
    if (!avatarId) {
      this.profileAvatarSrc.set('');
      return;
    }

    this.userService.getAvatar(avatarId).subscribe({
      next: (res) => {
        if (!isPlatformBrowser(this.platformId)) {
          return;
        }

        if (this.profileAvatarSrc()) {
          URL.revokeObjectURL(this.profileAvatarSrc());
        }
        console.log("profile image");

        const objectUrl = URL.createObjectURL(res);
        this.profileAvatarSrc.set(objectUrl);
      },
      error: (err) => {
        console.error("err: ==============> ", err);
      }
    });
  }

  loadProfile(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.userService.meUser().subscribe({
      next: (res) => {
        this.userProfile.set(res);
        console.log(this.userProfile());

        this.editFormName.set(res.username);
        this.editFormEmail.set(res.email);
        this.editFormAvatarUrl.set(res.avatarUrl || '');
        console.log(res);

        this.loadProfileImg(res.avatarUrl);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Failed to load profile. Please try again.');
        this.isLoading.set(false);
        console.error(err);
      }
    });
  }

  toggleEdit(): void {
    if (this.isEditing()) {
      const profile = this.userProfile();
      if (profile) {
        this.editFormName.set(profile.username);
        this.editFormEmail.set(profile.email);
        this.editFormAvatarUrl.set(profile.avatarUrl || '');
      }
    }
    this.isEditing.set(!this.isEditing());
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    if (file && file?.size > 1024 * 1024 * 2048) {
      // this.errorMessage.set({ msg: 'file size must not passe 2mb', isthere: true });
      return;
    } else if (file && file.type != 'image/png') {
      // this.errorMessage.set({ msg: 'file type must be image', isthere: true });
      return;
    }
    this.selectedAvatar.set(file);
  }

  onSubmit(form: NgForm): void {
    if (form.invalid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const avatarFile = this.selectedAvatar();

    // Create the observable for update
    const update$ = avatarFile
      ? this.userService.uploadAvatar(avatarFile).pipe(
        switchMap((avatarIdOrUrl) => {
          this.avatarName = avatarIdOrUrl;
          const updateData: UpdateProfile = {
            name: this.editFormName(),
            email: this.editFormEmail(),
            uuid: avatarIdOrUrl, // avatar ID or URL returned from upload
          };
          this.loadProfileImg(avatarIdOrUrl); // update preview
          return this.userService.updateUser(updateData);
        })
      )
      : this.userService.updateUser({
        name: this.editFormName(),
        email: this.editFormEmail(),
        uuid: this.avatarName || this.editFormAvatarUrl(), // keep existing avatar if no new upload
      });

    update$.subscribe({
      next: (res) => {
        this.userProfile.set(res);
        this.successMessage.set('Profile updated successfully!');
        this.isSubmitting.set(false);
        this.isEditing.set(false);
      },
      error: (err) => {
        console.log("===> "+err.message);
        
        this.errorMessage.set(
          err?.error?.message || err?.error?.msg || 'Failed to update profile. Please try again.'
        );
        this.isSubmitting.set(false);
      },
    });
  }
  onDeleteUser() {
    if (this.isSubmitting()) {
      return;
    }

    if (!confirm('Are you sure you want to delete your account? This action cannot be undone.')) {
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.userService.deleteUser().subscribe({
      next: (res) => {
        this.successMessage.set(res.msg || 'Account deleted successfully.');
        if (isPlatformBrowser(this.platformId)) {
          localStorage.removeItem('token');
          localStorage.removeItem('role');
        }
        this.isSubmitting.set(false);
        this.router.navigateByUrl('/login');
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(
          err?.error?.message || err?.error?.msg || 'Failed to delete user. Please try again.'
        );
        console.error("user has not been deleted ", err);
      }
    });
  }
}
