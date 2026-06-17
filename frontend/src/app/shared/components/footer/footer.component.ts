import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TypographyComponent } from '../typography/typography.component';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterModule, TypographyComponent],
  template: `
    <footer class="bg-accent text-background py-20">
      <div class="max-w-7xl mx-auto px-6 grid grid-cols-1 md:grid-cols-4 gap-12 text-center md:text-left rtl:md:text-right">
        <div class="col-span-1 md:col-span-2">
          <div class="flex items-center justify-center md:justify-start gap-3 mb-5">
            <span class="w-9 h-9 border border-secondary/60 rotate-45 flex items-center justify-center">
              <span class="-rotate-45 font-serif text-secondary text-lg">M</span>
            </span>
            <span class="font-serif text-2xl tracking-tight">Marketo</span>
          </div>
          <p class="font-sans text-sm leading-relaxed text-background/60 max-w-md mx-auto md:mx-0">
            The premier marketplace for authentic Moroccan craftsmanship. We connect world-class artisans with global connoisseurs of heritage and beauty.
          </p>
        </div>
        <div>
          <h6 class="eyebrow text-secondary mb-6">Explore</h6>
          <ul class="space-y-3">
            <li><a routerLink="/shop" class="text-sm text-background/60 hover:text-secondary transition-colors">Shop All Crafts</a></li>
            <li><a routerLink="/shop" class="text-sm text-background/60 hover:text-secondary transition-colors">Featured Artisans</a></li>
            <li><a routerLink="/shop" class="text-sm text-background/60 hover:text-secondary transition-colors">Heritage Regions</a></li>
            <li><a routerLink="/" class="text-sm text-background/60 hover:text-secondary transition-colors">Our Story</a></li>
          </ul>
        </div>
        <div>
          <h6 class="eyebrow text-secondary mb-6">Customer Care</h6>
          <ul class="space-y-3">
            <li><a routerLink="/" class="text-sm text-background/60 hover:text-secondary transition-colors">Shipping & Returns</a></li>
            <li><a routerLink="/" class="text-sm text-background/60 hover:text-secondary transition-colors">Authenticity Guarantee</a></li>
            <li><a routerLink="/" class="text-sm text-background/60 hover:text-secondary transition-colors">FAQ</a></li>
            <li><a routerLink="/" class="text-sm text-background/60 hover:text-secondary transition-colors">Contact Us</a></li>
          </ul>
        </div>
      </div>
      <div class="max-w-7xl mx-auto px-6 mt-16 pt-8 border-t border-background/15 flex flex-col md:flex-row justify-between items-center gap-4">
        <p class="font-sans text-xs text-background/40">© 2026 Marketo Moroccan Marketplace. Crafted with care in Morocco.</p>
        <div class="flex gap-8">
          <a href="#" class="text-background/40 hover:text-secondary transition-colors text-[10px] font-bold uppercase tracking-widest">Privacy</a>
          <a href="#" class="text-background/40 hover:text-secondary transition-colors text-[10px] font-bold uppercase tracking-widest">Terms</a>
          <a href="#" class="text-background/40 hover:text-secondary transition-colors text-[10px] font-bold uppercase tracking-widest">Cookies</a>
        </div>
      </div>
    </footer>
  `
})
export class FooterComponent {}
