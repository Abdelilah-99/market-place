import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Product } from '../../core/models/Product';
import { ProductRatingStats, ProductsService } from '../../core/services/products-service';
import { ToasterService } from '../../core/services/toaster-service';
import { PurchaseAnalyticsService } from '../../core/services/purchase-analytics-service';
import { StateService } from '../../core/services/state-service';

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
  ratingStats = signal<ProductRatingStats>({
    average: 0,
    count: 0,
    breakdown: { '5': 0, '4': 0, '3': 0, '2': 0, '1': 0 },
    myRating: null,
  });
  selectedRating = signal(0);
  hoverRating = signal(0);
  ratingSubmitting = signal(false);

  constructor(
    private route: ActivatedRoute,
    private productsService: ProductsService,
    private toaster: ToasterService,
    private purchaseAnalyticsService: PurchaseAnalyticsService,
    private stateService: StateService,
    private router: Router
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
        this.loadRatings(id);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Product not found.');
        this.loading.set(false);
      }
    });
  }

  loadRatings(productId: string) {
    this.productsService.getProductRatings(productId).subscribe({
      next: (res) => {
        const stats = res.data ?? this.emptyRatingStats();
        this.ratingStats.set(stats);
        this.selectedRating.set(stats.myRating ?? 0);
      },
      error: () => {
        this.ratingStats.set({
          ...this.emptyRatingStats(),
          average: this.product()?.averageRating ?? 0,
          count: this.product()?.ratingCount ?? 0,
          breakdown: this.product()?.ratingBreakdown ?? this.emptyRatingStats().breakdown,
          myRating: null,
        });
      }
    });
  }

  productImages(): string[] {
    const product = this.product();
    if (!product) return [];

    const images = product.images?.length ? product.images : product.image ? [product.image] : [];
    return images.filter(Boolean).map(image => `/api/media/products/${image}`);
  }

  conditionLabel(condition?: string): string {
    if (!condition) return 'Used';
    return condition.charAt(0).toUpperCase() + condition.slice(1).toLowerCase();
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

  displayRating(): number {
    return this.hoverRating() || this.selectedRating();
  }

  roundedAverage(): number {
    return Math.round(this.ratingStats().average || 0);
  }

  ratingBreakdownRows() {
    const stats = this.ratingStats();
    return [5, 4, 3, 2, 1].map(stars => {
      const count = Number(stats.breakdown?.[String(stars)] ?? 0);
      const percent = stats.count ? Math.round((count / stats.count) * 100) : 0;
      return { stars, count, percent };
    });
  }

  submitRating(stars: number) {
    const product = this.product();
    if (!product || this.ratingSubmitting()) return;

    this.selectedRating.set(stars);
    this.ratingSubmitting.set(true);
    this.productsService.rateProduct(product.id, stars).subscribe({
      next: (res) => {
        const stats = res.data ?? this.emptyRatingStats();
        this.ratingStats.set(stats);
        this.selectedRating.set(stats.myRating ?? stars);
        this.ratingSubmitting.set(false);
        this.toaster.success('Your rating was saved.');
      },
      error: (err) => {
        this.ratingSubmitting.set(false);
        const message = err?.status === 401
          ? 'Please log in to rate this product.'
          : err?.error?.message || 'Could not save your rating.';
        this.toaster.error(message);
      }
    });
  }

  buyProduct(): void {
    const product = this.product();
    const buyer = this.stateService.currentUserSubject.value;

    if (!product) return;

    if (!buyer) {
      this.toaster.info('Please log in to buy this product.');
      this.router.navigateByUrl('/login');
      return;
    }

    if (buyer.id === product.userId) {
      this.toaster.warning('You cannot buy your own product.');
      return;
    }

    this.purchaseAnalyticsService.recordPurchase(
      {
        ...product,
        averageRating: this.ratingStats().average,
        ratingCount: this.ratingStats().count,
      },
      buyer
    );
    this.toaster.success('Purchase saved to your profile.');
  }

  private emptyRatingStats(): ProductRatingStats {
    return {
      average: 0,
      count: 0,
      breakdown: { '5': 0, '4': 0, '3': 0, '2': 0, '1': 0 },
      myRating: null,
    };
  }
}
