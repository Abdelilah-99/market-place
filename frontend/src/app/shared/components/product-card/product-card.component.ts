import { Component, Input, Output, EventEmitter, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TypographyComponent } from '../typography/typography.component';
import { BadgeComponent } from '../badge/badge.component';
import { PriceTagComponent } from '../price-tag/price-tag.component';
import { ButtonComponent } from '../button/button.component';
import { Product } from '../../../core/models/Product';
import { UsersService } from '../../../core/services/users-service';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, RouterModule, TypographyComponent, BadgeComponent, PriceTagComponent, ButtonComponent],
  template: `
    <div class="group relative bg-surface rounded-sm overflow-hidden shadow-elevation-1 hover:shadow-elevation-premium transition-all duration-500 border border-line hover:border-secondary/40">
      <!-- Image Container -->
      <div class="relative aspect-[4/5] overflow-hidden bg-accent/5">
        <img [src]="productImageUrl() || 'avatar.jpeg'" [alt]="product.name" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-[1200ms] ease-out">

        <!-- Overlay for Quick Add -->
        <div class="absolute inset-x-0 bottom-0 p-4 translate-y-full group-hover:translate-y-0 transition-transform duration-400 ease-out">
          <app-button variant="secondary" size="sm" [fullWidth]="true" (onClick)="onQuickAdd($event)">Quick Add</app-button>
        </div>

        <!-- Wishlist Button -->
        <button aria-label="Add to wishlist" class="absolute top-3 right-3 p-2 rounded-full bg-surface/90 backdrop-blur-sm text-accent hover:text-primary transition-all shadow-sm hover:scale-110 active:scale-90 z-10">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path></svg>
        </button>

        <!-- Category Badge -->
        <div class="absolute top-3 left-3" *ngIf="product.category">
          <span class="inline-flex items-center px-2.5 py-1 rounded-sm bg-surface/90 backdrop-blur-sm text-[9px] font-bold tracking-[0.18em] uppercase text-accent">{{ product.category }}</span>
        </div>
      </div>

      <!-- Content -->
      <a [routerLink]="['/product', product.id]" class="block p-5">
        <app-typography variant="caption" customClass="text-muted flex items-center gap-1.5 mb-2 text-xs">
          <svg class="w-3 h-3 text-secondary-dark" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z" clip-rule="evenodd"></path></svg>
          {{ product.region || 'Authentic Morocco' }}
        </app-typography>
        <app-typography variant="h6" [serif]="true" customClass="truncate mb-3 group-hover:text-primary transition-colors">{{ product.name }}</app-typography>
        <div class="flex items-center justify-between pt-3 border-t border-line">
          <app-price-tag [amount]="product.price" size="sm"></app-price-tag>
          <span class="text-[10px] font-bold uppercase tracking-[0.2em] text-primary inline-flex items-center gap-1">
            View
            <svg class="w-3 h-3 transition-transform group-hover:translate-x-0.5 rtl:rotate-180" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"></path></svg>
          </span>
        </div>
      </a>
    </div>
  `
})
export class ProductCardComponent implements OnInit {
  @Input() product!: Product;
  @Output() quickAdd = new EventEmitter<Product>();

  productImageUrl = signal('');

  constructor(private userService: UsersService) {}

  ngOnInit() {
    if (this.product.image) {
      this.loadProductImage(this.product.image);
    }
  }

  loadProductImage(imageId: string) {
    this.userService.getAvatar(imageId).subscribe({
      next: (res) => {
        const objectUrl = URL.createObjectURL(res);
        this.productImageUrl.set(objectUrl);
      },
      error: (err) => console.error("Error loading product image", err)
    });
  }

  onQuickAdd(event: MouseEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.quickAdd.emit(this.product);
  }
}
