import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Product } from '../../core/models/Product';
import { ProductsService } from '../../core/services/products-service';
import { PublicProfile as PublicProfileModel, UsersService } from '../../core/services/users-service';

@Component({
  selector: 'app-public-profile',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './public-profile.html',
  styleUrl: './public-profile.css',
})
export class PublicProfile {
  profile = signal<PublicProfileModel | null>(null);
  products = signal<Product[]>([]);
  loading = signal(true);
  error = signal('');
  page = signal(0);
  totalPages = signal(0);
  readonly pageSize = 12;
  private userId = '';

  constructor(
    private route: ActivatedRoute,
    private usersService: UsersService,
    private productsService: ProductsService,
  ) {}

  ngOnInit(): void {
    this.userId = this.route.snapshot.paramMap.get('id') ?? '';
    if (!this.userId) {
      this.error.set('User profile not found.');
      this.loading.set(false);
      return;
    }
    this.loadProfile();
  }

  loadProfile(page = 0): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      profile: this.usersService.getPublicProfile(this.userId),
      products: this.productsService.getProductsByUser(this.userId, page, this.pageSize),
    }).subscribe({
      next: ({ profile, products }) => {
        this.profile.set(profile);
        this.products.set(products.data?.items ?? []);
        this.page.set(products.data?.page ?? page);
        this.totalPages.set(products.data?.totalPages ?? 0);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('User profile not found.');
        this.loading.set(false);
      },
    });
  }

  avatarUrl(): string {
    const avatar = this.profile()?.avatarUrl;
    return avatar ? `/api/media/users/${avatar}` : '';
  }

  productImage(product: Product): string {
    const image = product.image || product.images?.[0];
    return image ? `/api/media/products/${image}` : '';
  }
}
