import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, Inject, PLATFORM_ID, signal, computed } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { StateService } from '../../core/services/state-service';
import { Me, UsersService } from '../../core/services/users-service';
import { LanguageService, Language } from '../../core/services/language.service';
import { SettingsService, Currency } from '../../core/services/settings.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar {
  isOpen = false;
  isLangMenuOpen = false;
  isCurrencyMenuOpen = false;
  isSeller = signal(false);
  currentUser = signal<Me | null>(null);
  profileAvatarSrc = signal('');

  currentLang = computed(() => this.languageService.currentLanguage());
  currentCurrency = computed(() => this.settingsService.currentCurrency());

  constructor(
    private userService: UsersService,
    private router: Router,
    private stateService: StateService,
    private languageService: LanguageService,
    private settingsService: SettingsService,
    @Inject(PLATFORM_ID) private platformId: object
  ) { }

  ngOnInit() {
    this.stateService.currentUser$.subscribe((user: Me | null) => {
      if (user == null) {
        this.currentUser.set(null);
        this.isSeller.set(false);
        return
      }
      this.currentUser.set(user);
      this.isSeller.set(user.role === "SELLER");
      this.loadProfileImg(user.avatarUrl);
    });
  }

  loadProfileImg(avatarId: string | undefined): void {
    if (!avatarId) {
      this.profileAvatarSrc.set('');
      return;
    }

    this.userService.getAvatar(avatarId).subscribe({
      next: (res) => {
        if (!isPlatformBrowser(this.platformId)) return;
        if (this.profileAvatarSrc()) URL.revokeObjectURL(this.profileAvatarSrc());
        const objectUrl = URL.createObjectURL(res);
        this.profileAvatarSrc.set(objectUrl);
      },
      error: (err) => console.error("err loading avatar: ", err)
    });
  }

  toggleMenu() {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.isLangMenuOpen = false;
      this.isCurrencyMenuOpen = false;
    }
  }

  toggleLangMenu() {
    this.isLangMenuOpen = !this.isLangMenuOpen;
    if (this.isLangMenuOpen) {
      this.isOpen = false;
      this.isCurrencyMenuOpen = false;
    }
  }

  toggleCurrencyMenu() {
    this.isCurrencyMenuOpen = !this.isCurrencyMenuOpen;
    if (this.isCurrencyMenuOpen) {
      this.isOpen = false;
      this.isLangMenuOpen = false;
    }
  }

  setLang(lang: string) {
    this.languageService.setLanguage(lang as Language);
    this.isLangMenuOpen = false;
  }

  setCurrency(currency: string) {
    this.settingsService.setCurrency(currency as Currency);
    this.isCurrencyMenuOpen = false;
  }

  isAuthenticated(): boolean {
    if (isPlatformBrowser(this.platformId)) {
      return !!localStorage.getItem('token');
    }
    return false;
  }

  logout(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.stateService.clearUser();
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    this.isOpen = false;
    this.router.navigateByUrl('/login');
  }
}
