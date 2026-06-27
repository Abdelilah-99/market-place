import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Product } from '../../core/models/Product';
import { ProductsService } from '../../core/services/products-service';
import { Me, UsersService } from '../../core/services/users-service';
import { ToasterService } from '../../core/services/toaster-service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.css',
})
export class AdminDashboard {
  users = signal<Me[]>([]);
  products = signal<Product[]>([]);
  usersPage = signal(0);
  productsPage = signal(0);
  usersTotal = signal(0);
  productsTotal = signal(0);
  usersHasNext = signal(false);
  productsHasNext = signal(false);
  usersLoading = signal(false);
  productsLoading = signal(false);
  readonly pageSize = 12;
  readonly roles = ['BUYER', 'SELLER', 'ADMIN'];

  constructor(
    private usersService: UsersService,
    private productsService: ProductsService,
    private toaster: ToasterService
  ) {}

  ngOnInit() {
    this.loadUsers();
    this.loadProducts();
  }

  loadUsers(page = 0, append = false) {
    if (this.usersLoading()) return;
    this.usersLoading.set(true);
    this.usersService.getAdminUsers(page, this.pageSize).subscribe({
      next: (res) => {
        this.users.set(append ? [...this.users(), ...res.items] : res.items);
        this.usersTotal.set(res.total);
        this.usersPage.set(res.page);
        this.usersHasNext.set(res.hasNext);
        this.usersLoading.set(false);
      },
      error: () => {
        this.toaster.error('Failed to load users.');
        this.usersLoading.set(false);
      },
    });
  }

  loadProducts(page = 0, append = false) {
    if (this.productsLoading()) return;
    this.productsLoading.set(true);
    this.productsService.getAdminProducts(page, this.pageSize).subscribe({
      next: (res) => {
        const data = res.data;
        const products = data?.items ?? [];
        this.products.set(append ? [...this.products(), ...products] : products);
        this.productsTotal.set(data?.total ?? products.length);
        this.productsPage.set(data?.page ?? page);
        this.productsHasNext.set(data?.hasNext ?? false);
        this.productsLoading.set(false);
      },
      error: () => {
        this.toaster.error('Failed to load products.');
        this.productsLoading.set(false);
      },
    });
  }

  updateUserRole(user: Me, role: string) {
    this.usersService.updateAdminUserRole(user.id, role).subscribe({
      next: (updated) => {
        this.users.update(users => users.map(item => item.id === updated.id ? updated : item));
        this.toaster.success('User role updated.');
      },
      error: () => this.toaster.error('Failed to update role.'),
    });
  }

  deleteUser(user: Me) {
    this.usersService.deleteAdminUser(user.id).subscribe({
      next: () => {
        this.users.update(users => users.filter(item => item.id !== user.id));
        this.usersTotal.update(total => Math.max(total - 1, 0));
        this.toaster.success('User deleted.');
      },
      error: () => this.toaster.error('Failed to delete user.'),
    });
  }

  deleteProduct(product: Product) {
    this.productsService.adminDeleteProduct(product.id).subscribe({
      next: () => {
        this.products.update(products => products.filter(item => item.id !== product.id));
        this.productsTotal.update(total => Math.max(total - 1, 0));
        this.toaster.success('Product deleted.');
      },
      error: () => this.toaster.error('Failed to delete product.'),
    });
  }

  loadMoreUsers() {
    if (this.usersHasNext()) this.loadUsers(this.usersPage() + 1, true);
  }

  loadMoreProducts() {
    if (this.productsHasNext()) this.loadProducts(this.productsPage() + 1, true);
  }
}
