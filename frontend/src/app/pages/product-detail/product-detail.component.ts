import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { SectionComponent } from '../../shared/components/section/section.component';
import { TypographyComponent } from '../../shared/components/typography/typography.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PriceTagComponent } from '../../shared/components/price-tag/price-tag.component';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';
import { Product } from '../../core/models/Product';
import { RecentlyViewedService } from '../../core/services/recently-viewed.service';
import { ProductsService } from '../../core/services/products-service';
import { UsersService } from '../../core/services/users-service';

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
  productImageUrl = signal('');
  
  constructor(
    private route: ActivatedRoute,
    public recentlyViewedService: RecentlyViewedService,
    private productsService: ProductsService,
    private userService: UsersService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const id = params['id'];
      this.loadProduct(id);
    });
  }

  loadProduct(id: string) {
    // In a real scenario, you'd have a getProductById method. 
    // For now, we fetch all and find, or assume the service is updated.
    this.productsService.getAllProducts().subscribe({
      next: (res: any) => {
        const product = res.data.find((p: Product) => p.id === id);
        if (product) {
          this.product = product;
          this.recentlyViewedService.addProduct(product);
          if (product.image) this.loadProductImage(product.image);
          
          // Simple related products logic
          this.relatedProducts = res.data
            .filter((p: Product) => p.category === product.category && p.id !== id)
            .slice(0, 3);
        }
      }
    });
  }

  loadProductImage(imageId: string) {
    this.userService.getAvatar(imageId).subscribe({
      next: (res) => {
        const objectUrl = URL.createObjectURL(res);
        this.productImageUrl.set(objectUrl);
      },
      error: (err) => console.error("Error loading product detail image", err)
    });
  }

  addToCart() {
    console.log('Added to cart:', this.product?.name);
  }

  addToWishlist() {
    console.log('Added to wishlist:', this.product?.name);
  }
}
