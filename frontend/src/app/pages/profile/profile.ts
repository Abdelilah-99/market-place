import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { Me, UpdateProfile, UsersService } from '../../core/services/users-service';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { PLATFORM_ID, Inject } from '@angular/core';
import { switchMap } from 'rxjs';

@Component({
  selector: 'app-profile',
  imports: [CommonModule, FormsModule],
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
  selectedAvatarPreview = signal('');
  selectedAvatar = signal<File | null>(null);
  removeCurrentAvatar = signal(false);

  constructor(
    private userService: UsersService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: object
  ) { }

  ngOnInit(): void {
    this.loadProfile();
  }

  ngOnDestroy(): void {
    this.revokeObjectUrl(this.profileAvatarSrc());
    this.revokeObjectUrl(this.selectedAvatarPreview());
  }

  loadProfileImg(avatarId: string | null): void {
    if (!avatarId) {
      this.revokeObjectUrl(this.profileAvatarSrc());
      this.profileAvatarSrc.set('');
      return;
    }

    this.userService.getAvatar(avatarId).subscribe({
      next: (res) => {
        if (!isPlatformBrowser(this.platformId)) {
          return;
        }

        this.revokeObjectUrl(this.profileAvatarSrc());

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
        this.removeCurrentAvatar.set(false);

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
      this.clearSelectedAvatar();
      this.removeCurrentAvatar.set(false);
    }
    this.isEditing.set(!this.isEditing());
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    this.errorMessage.set('');

    if (!file) {
      return;
    }

    if (file.size > 2 * 1024 * 1024) {
      this.errorMessage.set('Avatar image must be 2MB or smaller.');
      input.value = '';
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.errorMessage.set('Please choose a valid image file.');
      input.value = '';
      return;
    }

    this.revokeObjectUrl(this.selectedAvatarPreview());
    this.selectedAvatar.set(file);
    this.selectedAvatarPreview.set(URL.createObjectURL(file));
    this.removeCurrentAvatar.set(false);
    input.value = '';
  }

  clearSelectedAvatar(): void {
    this.revokeObjectUrl(this.selectedAvatarPreview());
    this.selectedAvatarPreview.set('');
    this.selectedAvatar.set(null);
  }

  removeAvatar(): void {
    this.clearSelectedAvatar();
    this.removeCurrentAvatar.set(true);
  }

  undoRemoveAvatar(): void {
    this.removeCurrentAvatar.set(false);
  }

  selectedAvatarLabel(): string {
    const file = this.selectedAvatar();
    if (!file) {
      return '';
    }

    return `${file.name} · ${this.formatFileSize(file.size)}`;
  }

  onSubmit(form: NgForm): void {
    if (form.invalid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const avatarFile = this.selectedAvatar();
    const nextAvatarId = this.removeCurrentAvatar() ? null : this.editFormAvatarUrl() || null;

    const update$ = avatarFile
      ? this.userService.uploadAvatar(avatarFile).pipe(
        switchMap((avatarIdOrUrl) => {
          const updateData: UpdateProfile = {
            name: this.editFormName(),
            email: this.editFormEmail(),
            uuid: avatarIdOrUrl,
          };
          return this.userService.updateUser(updateData);
        })
      )
      : this.userService.updateUser({
        name: this.editFormName(),
        email: this.editFormEmail(),
        uuid: nextAvatarId,
      });

    update$.subscribe({
      next: (res) => {
        this.userProfile.set(res);
        this.editFormAvatarUrl.set(res.avatarUrl || '');
        this.removeCurrentAvatar.set(false);
        this.clearSelectedAvatar();
        this.loadProfileImg(res.avatarUrl);
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

  private formatFileSize(size: number): string {
    if (size < 1024 * 1024) {
      return `${Math.max(1, Math.round(size / 1024))} KB`;
    }

    return `${(size / 1024 / 1024).toFixed(1)} MB`;
  }

  private revokeObjectUrl(url: string): void {
    if (isPlatformBrowser(this.platformId) && url) {
      URL.revokeObjectURL(url);
    }
  }
}
