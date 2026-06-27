import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, Inject, PLATFORM_ID, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { StateService } from '../../core/services/state-service';
import { Me, UsersService } from '../../core/services/users-service';

@Component({
  selector: 'app-navbar',
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar {
  isOpen = false;
  isSeller = signal(false);
  currentUser = signal<Me | null>(null);
  // crrAvatar = signal('');
  profileAvatarSrc = signal('');


  constructor(
    private userService: UsersService,
    private router: Router,
    private stateService: StateService,
    @Inject(PLATFORM_ID) private platformId: object
  ) { }

  ngOnInit() {
    this.stateService.currentUser$.subscribe((user: Me | null) => {
      if (user == null) {
        this.currentUser.set(null);
        this.isSeller.set(false);
        this.profileAvatarSrc.set('');
        return;
      }
      this.currentUser.set(user);

      if (user && user.role === "SELLER") {
        this.isSeller.set(true);
      } else {
        this.isSeller.set(false);
      }
    });
    if (!this.currentUser()?.id) {
      return;
    }
    this.loadUser();
  }

  loadUser(): void {
    this.stateService.getMyInfo();
    this.loadProfileImg(this.currentUser()?.avatarUrl);
  }

  loadProfileImg(avatarId: string | null | undefined): void {
    if (!avatarId) {
      // console.log("here");

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
        const objectUrl = URL.createObjectURL(res);
        this.profileAvatarSrc.set(objectUrl);
      },
      error: (err) => {
        this.profileAvatarSrc.set('');
        if (err?.status !== 404) {
          console.error("Failed to load profile image", err);
        }
      }
    });
  }

  toggleMenu() {
    this.isOpen = !this.isOpen;
  }

  isAuthenticated(): boolean {
    if (isPlatformBrowser(this.platformId)) {
      return !!localStorage.getItem('token') && this.currentUser() !== null;
    }

    return false;
  }



  logout(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.stateService.clearUser();
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    this.isOpen = false;
    this.router.navigateByUrl('/login');
  }
}
