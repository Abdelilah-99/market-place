import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductItem } from '../../sub-components/product/product';
import { CreateProductPopPup } from '../../sub-components/create-product-pop-pup/create-product-pop-pup';
import { Product } from '../../core/models/Product';
import { ProductsService } from '../../core/services/products-service';
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
  private readonly pageSize = 12;

  constructor(private productsService: ProductsService) { }

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
    this.isPopPupOpen = false;
  }

  deleteProductById(id: string) {
    this.products.set(
      this.products().filter(product => product.id !== id)
    );
    this.total.update(current => Math.max(current - 1, 0));
  }


  public togglePopUp() {
    this.isPopPupOpen = !this.isPopPupOpen;
  }
}
