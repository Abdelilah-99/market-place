import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TypographyComponent } from '../typography/typography.component';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterModule, TypographyComponent],
  template: `
    <footer class="bg-zellige-pattern py-16 border-t border-primary/10">
      <div class="max-w-7xl mx-auto px-4 grid grid-cols-1 md:grid-cols-4 gap-12 text-center md:text-left rtl:md:text-right">
        <div class="col-span-1 md:col-span-2">
          <app-typography variant="h3" [serif]="true" customClass="text-accent mb-4">Marketo</app-typography>
          <app-typography variant="body" customClass="text-accent/70 max-w-md mx-auto md:mx-0">
            The premier marketplace for authentic Moroccan craftsmanship. We connect world-class artisans with global connoisseurs of heritage and beauty.
          </app-typography>
        </div>
        <div>
          <app-typography variant="h6" [serif]="true" customClass="text-accent mb-6 uppercase tracking-[0.2em] text-[10px] font-black">Explore</app-typography>
          <ul class="space-y-3">
            <li><a routerLink="/" class="text-sm text-accent/70 hover:text-primary transition-colors">Shop All Crafts</a></li>
            <li><a routerLink="/" class="text-sm text-accent/70 hover:text-primary transition-colors">Featured Artisans</a></li>
            <li><a routerLink="/" class="text-sm text-accent/70 hover:text-primary transition-colors">Heritage Regions</a></li>
            <li><a routerLink="/" class="text-sm text-accent/70 hover:text-primary transition-colors">Our Story</a></li>
          </ul>
        </div>
        <div>
          <app-typography variant="h6" [serif]="true" customClass="text-accent mb-6 uppercase tracking-[0.2em] text-[10px] font-black">Customer Care</app-typography>
          <ul class="space-y-3">
            <li><a routerLink="/" class="text-sm text-accent/70 hover:text-primary transition-colors">Shipping & Returns</a></li>
            <li><a routerLink="/" class="text-sm text-accent/70 hover:text-primary transition-colors">Authenticity Guarantee</a></li>
            <li><a routerLink="/" class="text-sm text-accent/70 hover:text-primary transition-colors">FAQ</a></li>
            <li><a routerLink="/" class="text-sm text-accent/70 hover:text-primary transition-colors">Contact Us</a></li>
          </ul>
        </div>
      </div>
      <div class="max-w-7xl mx-auto px-4 mt-16 pt-8 border-t border-primary/10 flex flex-col md:flex-row justify-between items-center gap-4">
        <app-typography variant="caption" customClass="text-accent/40">© 2026 Marketo Moroccan Marketplace. Crafted with love in Morocco.</app-typography>
        <div class="flex gap-8">
          <a href="#" class="text-accent/40 hover:text-primary transition-colors text-[10px] font-black uppercase tracking-widest">Privacy</a>
          <a href="#" class="text-accent/40 hover:text-primary transition-colors text-[10px] font-black uppercase tracking-widest">Terms</a>
          <a href="#" class="text-accent/40 hover:text-primary transition-colors text-[10px] font-black uppercase tracking-widest">Cookies</a>
        </div>
      </div>
    </footer>
  `
})
export class FooterComponent {}
