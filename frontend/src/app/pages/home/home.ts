import { Component, signal } from '@angular/core';
import { Product } from '../../core/models/Product';
import { ProductItem } from '../../sub-components/product/product';
import { CommonModule } from '@angular/common';
import { ProductsService } from '../../core/services/products-service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [ProductItem, CommonModule, FormsModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css'],
})
export class Home {
  public products = signal<Product[]>([]);
  public loading = signal(false);
  public total = signal(0);
  public page = signal(0);
  public totalPages = signal(0);
  public searchMode = signal(false);

  public query = '';
  public category = '';
  public minPrice: number | null = null;
  public maxPrice: number | null = null;
  public sort = 'newest';
  private readonly pageSize = 12;

  constructor(private productsService: ProductsService) {}

  ngOnInit() {
    this.loadProducts();
  }

  loadProducts() {
    this.loading.set(true);
    this.searchMode.set(false);
    this.productsService.getAllProducts().subscribe({
      next: (res: any) => {
        const products = res.data ?? [];
        this.products.set(products);
        this.total.set(products.length);
        this.page.set(0);
        this.totalPages.set(products.length > 0 ? 1 : 0);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading products', err);
        this.loading.set(false);
      }
    });
  }

  search(page = 0) {
    const hasSearchFilters = this.query.trim() || this.category.trim() || this.minPrice !== null || this.maxPrice !== null;
    if (!hasSearchFilters && this.sort === 'newest') {
      this.loadProducts();
      return;
    }

    this.loading.set(true);
    this.searchMode.set(true);
    this.productsService.searchProducts({
      q: this.query.trim(),
      category: this.category.trim(),
      minPrice: this.minPrice,
      maxPrice: this.maxPrice,
      page,
      size: this.pageSize,
      sort: this.sort
    }).subscribe({
      next: (res) => {
        const data = res.data;
        this.products.set((data?.items ?? []).map((product) => ({
          ...product,
          image: product.image || product.images?.[0] || ''
        })));
        this.total.set(data?.total ?? 0);
        this.page.set(data?.page ?? 0);
        this.totalPages.set(data?.totalPages ?? 0);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error searching products', err);
        this.products.set([]);
        this.total.set(0);
        this.totalPages.set(0);
        this.loading.set(false);
      }
    });
  }

  resetSearch() {
    this.query = '';
    this.category = '';
    this.minPrice = null;
    this.maxPrice = null;
    this.sort = 'newest';
    this.loadProducts();
  }

  nextPage() {
    if (this.page() + 1 < this.totalPages()) {
      this.search(this.page() + 1);
    }
  }

  previousPage() {
    if (this.page() > 0) {
      this.search(this.page() - 1);
    }
  }
}
