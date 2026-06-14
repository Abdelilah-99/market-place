import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { SectionComponent } from '../../shared/components/section/section.component';
import { TypographyComponent } from '../../shared/components/typography/typography.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PriceTagComponent } from '../../shared/components/price-tag/price-tag.component';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';
import { Product, products } from '../../core/models/Product';
import { RecentlyViewedService } from '../../core/services/recently-viewed.service';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    SectionComponent,
    TypographyComponent,
    BadgeComponent,
    PriceTagComponent,
    ButtonComponent,
    ProductCardComponent
  ],
  templateUrl: './product-detail.component.html'
})
export class ProductDetailComponent implements OnInit {
  product?: Product;
  relatedProducts: Product[] = [];

  constructor(
    private route: ActivatedRoute,
    public recentlyViewedService: RecentlyViewedService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const id = params['id'];
      this.product = products.find(p => p.id === id);
      if (this.product) {
        this.recentlyViewedService.addProduct(this.product);
        this.relatedProducts = products
          .filter(p => p.category === this.product?.category && p.id !== id)
          .slice(0, 3);
      }
    });
  }

  addToCart() {
    console.log('Added to cart:', this.product?.name);
  }

  addToWishlist() {
    console.log('Added to wishlist:', this.product?.name);
  }
}
