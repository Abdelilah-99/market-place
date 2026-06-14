import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TypographyComponent } from '../typography/typography.component';
import { BadgeComponent } from '../badge/badge.component';
import { PriceTagComponent } from '../price-tag/price-tag.component';
import { ButtonComponent } from '../button/button.component';
import { Product } from '../../../core/models/Product';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, RouterModule, TypographyComponent, BadgeComponent, PriceTagComponent, ButtonComponent],
  template: `
    <div class="group relative bg-white rounded-lg overflow-hidden shadow-elevation-1 hover:shadow-elevation-premium transition-all duration-500 border border-primary/5">
      <!-- Image Container -->
      <div class="relative aspect-square overflow-hidden bg-accent/5">
        <img [src]="product.image || 'assets/placeholder.jpg'" [alt]="product.name" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700">

        <!-- Overlay for Quick Add -->
        <div class="absolute inset-0 bg-accent/20 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center gap-2">
          <app-button variant="secondary" size="sm" (onClick)="onQuickAdd($event)">Quick Add</app-button>
        </div>

        <!-- Wishlist Button -->
        <button class="absolute top-4 right-4 p-2 rounded-full bg-white/80 backdrop-blur-sm text-accent hover:text-primary transition-all shadow-sm hover:scale-110 active:scale-90 z-10">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path></svg>
        </button>

        <!-- Category Badge -->
        <div class="absolute top-4 left-4">
          <app-badge variant="primary">{{ product.category }}</app-badge>
        </div>
      </div>

      <!-- Content -->
      <div class="p-6">
        <div class="flex justify-between items-start mb-1">
          <app-typography variant="h6" [serif]="true" customClass="truncate flex-1">{{ product.name }}</app-typography>
          <app-price-tag [amount]="product.price" size="sm" customClass="ms-2"></app-price-tag>
        </div>
        <app-typography variant="caption" customClass="line-clamp-1 mb-4 flex items-center gap-1">
          <svg class="w-3 h-3 text-primary" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z" clip-rule="evenodd"></path></svg>
          {{ product.artisan }} • {{ product.region }}
        </app-typography>

        <a [routerLink]="['/product', product.id]" class="block text-center text-[10px] font-black uppercase tracking-[0.2em] text-primary hover:text-primary/70 transition-colors w-fit mx-auto border-b border-primary/20 hover:border-primary">View Details</a>
      </div>
    </div>
  `
})
export class ProductCardComponent {
  @Input() product!: Product;
  @Output() quickAdd = new EventEmitter<Product>();

  onQuickAdd(event: MouseEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.quickAdd.emit(this.product);
  }
}
