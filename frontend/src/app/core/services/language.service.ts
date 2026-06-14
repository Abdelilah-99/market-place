import { Injectable, signal, effect } from '@angular/core';

export type Language = 'en' | 'fr' | 'ar';

@Injectable({
  providedIn: 'root'
})
export class LanguageService {
  currentLanguage = signal<Language>('en');

  constructor() {
    // Sync with document on change
    effect(() => {
      const lang = this.currentLanguage();
      if (typeof document !== 'undefined') {
        document.documentElement.lang = lang;
        document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';
      }
    });
  }

  setLanguage(lang: Language) {
    this.currentLanguage.set(lang);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('preferredLang', lang);
    }
  }

  initLanguage() {
    if (typeof localStorage !== 'undefined') {
      const savedLang = localStorage.getItem('preferredLang') as Language;
      if (savedLang && ['en', 'fr', 'ar'].includes(savedLang)) {
        this.setLanguage(savedLang);
      }
    }
  }
}
