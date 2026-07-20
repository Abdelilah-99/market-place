import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductItem } from '../../sub-components/product/product';
import { CreateProductPopPup } from '../../sub-components/create-product-pop-pup/create-product-pop-pup';
import { Product } from '../../core/models/Product';
import { ProductsService } from '../../core/services/products-service';
import { PurchaseAnalyticsService, SellerProfileAnalytics } from '../../core/services/purchase-analytics-service';
import { StateService } from '../../core/services/state-service';
@Component({
  selector: 'app-seller-dashboard',
  imports: [ProductItem, CreateProductPopPup, CommonModule],
  templateUrl: './seller-dashboard.html',
  styleUrl: './seller-dashboard.css',
})
export class SellerDashboard {
  public isPopPupOpen: boolean = false;

  public products = signal<Product[]>([]);
  public total = signal(0);
  public page = signal(0);
  public hasNext = signal(false);
  public loading = signal(false);
  public unavailableProducts = computed(() =>
    this.products().filter(product => product.quantity !== undefined && product.quantity <= 0)
  );
  public unavailableCount = computed(() => this.unavailableProducts().length);
  public sellerAnalytics = signal<SellerProfileAnalytics>({
    totalGained: 0,
    totalOrders: 0,
    totalItemsSold: 0,
    bestSellingProducts: [],
  });
  private readonly pageSize = 12;

  constructor(
    private productsService: ProductsService,
    private purchaseAnalyticsService: PurchaseAnalyticsService,
    private stateService: StateService
  ) { }

  ngOnInit() {
    this.loadProducts();
  }

  loadProducts(page = 0, append = false) {
    if (this.loading()) return;
    this.loading.set(true);
    this.productsService.getMyProductsPage(page, this.pageSize).subscribe({
      next: (res) => {
        const data = res.data;
        const products = data?.items ?? [];
        this.products.set(append ? [...this.products(), ...products] : products);
        this.total.set(data?.total ?? products.length);
        this.page.set(data?.page ?? page);
        this.hasNext.set(data?.hasNext ?? false);
        this.refreshSellerAnalytics();
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading products', err);
        this.loading.set(false);
      }
    });
  }

  loadMore() {
    if (!this.hasNext()) return;
    this.loadProducts(this.page() + 1, true);
  }

  onProductCreated(product: Product) {
    this.products.update(current => [product, ...current]);
    this.total.update(current => current + 1);
    this.refreshSellerAnalytics();
    this.isPopPupOpen = false;
  }

  deleteProductById(id: string) {
    this.products.set(
      this.products().filter(product => product.id !== id)
    );
    this.total.update(current => Math.max(current - 1, 0));
    this.refreshSellerAnalytics();
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      maximumFractionDigits: 2,
    }).format(value || 0);
  }

  productImage(image: string): string {
    return image ? `/api/media/products/${image}` : '';
  }


  public togglePopUp() {
    this.isPopPupOpen = !this.isPopPupOpen;
  }

  private refreshSellerAnalytics(): void {
    const seller = this.stateService.currentUserSubject.value;
    if (!seller) {
      return;
    }

    this.sellerAnalytics.set(
      this.purchaseAnalyticsService.getSellerAnalytics(seller.id, this.products())
    );
  }
}
