import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Product } from '../../core/models/Product';
import { ProductsService } from '../../core/services/products-service';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.css',
})
export class ProductDetail {
  product = signal<Product | null>(null);
  loading = signal(true);
  error = signal('');
  activeImage = signal(0);

  readonly rating = 4.8;
  readonly reviewCount = 24;
  readonly ratingBreakdown = [
    { stars: 5, percent: 82 },
    { stars: 4, percent: 13 },
    { stars: 3, percent: 4 },
    { stars: 2, percent: 1 },
    { stars: 1, percent: 0 },
  ];

  constructor(
    private route: ActivatedRoute,
    private productsService: ProductsService
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('Product not found.');
      this.loading.set(false);
      return;
    }

    this.productsService.getProduct(id).subscribe({
      next: (res) => {
        this.product.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Product not found.');
        this.loading.set(false);
      }
    });
  }

  productImages(): string[] {
    const product = this.product();
    if (!product) return [];

    const images = product.images?.length ? product.images : product.image ? [product.image] : [];
    return images.filter(Boolean).map(image => `/api/media/products/${image}`);
  }

  selectImage(index: number) {
    this.activeImage.set(index);
  }

  nextImage() {
    const total = this.productImages().length;
    if (total <= 1) return;
    this.activeImage.set((this.activeImage() + 1) % total);
  }

  previousImage() {
    const total = this.productImages().length;
    if (total <= 1) return;
    this.activeImage.set((this.activeImage() - 1 + total) % total);
  }

  stars(): number[] {
    return [1, 2, 3, 4, 5];
  }
}
