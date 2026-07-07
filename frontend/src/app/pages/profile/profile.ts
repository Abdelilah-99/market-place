import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { Me, UpdateProfile, UsersService } from '../../core/services/users-service';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { PLATFORM_ID, Inject } from '@angular/core';
import { EMPTY, Subject, Subscription, catchError, debounceTime, distinctUntilChanged, finalize, switchMap, tap } from 'rxjs';
import { BuyerProfileAnalytics, PurchaseAnalyticsService } from '../../core/services/purchase-analytics-service';

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
  userSearchQuery = signal('');
  userSearchResults = signal<Me[]>([]);
  isSearchingUsers = signal(false);
  hasSearchedUsers = signal(false);
  userSearchError = signal('');
  private readonly userSearchInput$ = new Subject<string>();
  private userSearchSubscription?: Subscription;
  buyerAnalytics = signal<BuyerProfileAnalytics>({
    totalSpent: 0,
    totalOrders: 0,
    totalItems: 0,
    bestProducts: [],
    mostBoughtProducts: [],
  });

  constructor(
    private userService: UsersService,
    private purchaseAnalyticsService: PurchaseAnalyticsService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: object
  ) { }

  ngOnInit(): void {
    this.setupUserSearch();
    this.loadProfile();
  }

  ngOnDestroy(): void {
    this.userSearchSubscription?.unsubscribe();
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
        this.revokeObjectUrl(this.profileAvatarSrc());
        this.profileAvatarSrc.set('');
        if (err?.status !== 404) {
          console.error("Failed to load profile image", err);
        }
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
        this.buyerAnalytics.set(this.purchaseAnalyticsService.getBuyerAnalytics(res.id));

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

  formatMoney(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      maximumFractionDigits: 2,
    }).format(value || 0);
  }

  productImage(image: string): string {
    return image ? `/api/media/products/${image}` : '';
  }

  onUserSearchChange(value: string): void {
    this.userSearchQuery.set(value);
    this.userSearchInput$.next(value);
  }

  searchUsers(): void {
    this.runUserSearch(this.userSearchQuery()).subscribe();
  }

  private setupUserSearch(): void {
    this.userSearchSubscription = this.userSearchInput$.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      switchMap((query) => this.runUserSearch(query))
    ).subscribe();
  }

  private runUserSearch(rawQuery: string) {
    const query = rawQuery.trim();
    this.hasSearchedUsers.set(true);
    this.userSearchError.set('');

    if (query.length === 0) {
      this.userSearchResults.set([]);
      this.hasSearchedUsers.set(false);
      this.isSearchingUsers.set(false);
      return EMPTY;
    }

    if (query.length < 2) {
      this.userSearchResults.set([]);
      this.userSearchError.set('Search with at least 2 characters.');
      return EMPTY;
    }

    this.isSearchingUsers.set(true);
    return this.userService.searchUsers(query).pipe(
      tap((res) => {
        this.userSearchResults.set(res.items ?? []);
      }),
      catchError((err) => {
        this.userSearchResults.set([]);
        this.userSearchError.set('Failed to search users. Please try again.');
        console.error(err);
        return EMPTY;
      }),
      finalize(() => this.isSearchingUsers.set(false))
    );
  }

  clearUserSearch(): void {
    this.userSearchQuery.set('');
    this.userSearchResults.set([]);
    this.userSearchError.set('');
    this.hasSearchedUsers.set(false);
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
