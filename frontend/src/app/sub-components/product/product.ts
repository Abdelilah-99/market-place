import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { Product } from '../../core/models/Product';
import { Router } from '@angular/router';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ProductsService } from '../../core/services/products-service';
import { FormsModule } from '@angular/forms';
import { MediaSevice } from '../../core/services/media-sevice';
import { ToasterService } from '../../core/services/toaster-service';
import { StateService } from '../../core/services/state-service';
import { PaymentsService } from '../../core/services/payments-service';

@Component({
  selector: 'app-product',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './product.html',
  styleUrls: ['./product.css'],
})
export class ProductItem {
  @Input() product!: Product;
  updatedProduct!: Product;
  @Output() deletedProductId = new EventEmitter<string>();

  // Signals for image preview and selected file
  imagePreview = signal<string | null>(null);
  selectedImage = signal<File | null>(null);
  isSaving = signal(false);
  isDeleting = signal(false);
  isDeleteConfirmOpen = signal(false);

  isMyProduct: boolean = false;
  isEditing = false;
  conditions = [
    { value: 'new', label: 'New' },
    { value: 'used', label: 'Used' },
  ];

  constructor(
    private router: Router,
    private producteService: ProductsService,
    private mediaSevice: MediaSevice,
    private toaster: ToasterService,
    private stateService: StateService,
    private paymentsService: PaymentsService
  ) { }

  ngOnInit() {
    this.updatedProduct = structuredClone(this.product);
    const currentUrl = this.router.url;
    this.isMyProduct = currentUrl === '/dashboard';
  }

  productImage(): string {
    const image = this.product.image || this.product.images?.[0] || '';
    return image ? `/api/media/products/${image}` : '';
  }

  conditionLabel(condition?: string): string {
    if (!condition) return 'Used';
    return condition.charAt(0).toUpperCase() + condition.slice(1).toLowerCase();
  }

  buyProduct(): void {
    const buyer = this.stateService.currentUserSubject.value;
    if (!buyer) {
      this.toaster.info('Please log in to buy this product.');
      this.router.navigateByUrl('/login');
      return;
    }

    if (buyer.id === this.product.userId) {
      this.toaster.warning('You cannot buy your own product.');
      return;
    }

    this.paymentsService.createCheckoutSession(this.product).subscribe({
      next: (session) => {
        if (!session.checkoutUrl) {
          this.toaster.error('Checkout session was not created.');
          return;
        }
        window.location.href = session.checkoutUrl;
      },
      error: (err) => {
        console.error('Stripe checkout failed', err);
        this.toaster.error(err?.error?.message || 'Payment checkout is not available right now.');
      },
    });
  }

  onImageSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];

    if (!file.type.startsWith('image/')) {
      this.toaster.error('Please select a valid image file.');
      this.selectedImage.set(null);
      this.imagePreview.set(null);
      input.value = '';
      return;
    }

    this.selectedImage.set(file);

    const reader = new FileReader();
    reader.onload = () => {
      this.imagePreview.set(reader.result as string);
      this.updatedProduct.image = reader.result as string;
    };
    reader.readAsDataURL(file);

    input.value = '';
  }


  save() {
    const file = this.selectedImage();
    if (this.isSaving()) return;

    this.isSaving.set(true);

    if (file) {
      this.mediaSevice.uploadProductImage(file).subscribe({
        next: (imageUrl: any) => {
          this.updatedProduct.image = imageUrl;
          this.updateProductDetails();
        },
        error: (err) => {
          console.error('Error uploading image:', err);
          this.toaster.error('Image upload failed. Please try again.');
          this.isSaving.set(false);
        }
      });
    } else {
      this.updateProductDetails();
    }
  }

  private updateProductDetails() {
    this.producteService.updateProduct(this.product.id, this.updatedProduct).subscribe({
      next: () => {
        this.product = structuredClone(this.updatedProduct);
        this.closeUpdate();
        this.toaster.success('Product updated successfully.');
        this.isSaving.set(false);
      },
      error: (err) => {
        console.error('Error updating product:', err);
        this.toaster.error('Failed to update product. Please try again.');
        this.isSaving.set(false);
      }
    });
  }

  cancel() {
    this.updatedProduct = structuredClone(this.product);
    this.closeUpdate()
  }

  closeUpdate() {
    this.isEditing = false
    this.selectedImage.set(null);
    this.imagePreview.set(null);
  }

  requestDelete() {
    this.isDeleteConfirmOpen.set(true);
  }

  cancelDelete() {
    if (this.isDeleting()) return;
    this.isDeleteConfirmOpen.set(false);
  }

  delete() {
    if (this.isDeleting()) return;
    this.isDeleting.set(true);

    this.producteService.deleteProducts(this.product.id).subscribe({
      next: () => {
        this.toaster.success('Product deleted successfully.');
        this.deletedProductId.emit(this.product.id);
        this.isDeleting.set(false);
        this.isDeleteConfirmOpen.set(false);
      },
      error: (err) => {
        console.error('Error deleting product:', err);
        this.toaster.error('Failed to delete product. Please try again.');
        this.isDeleting.set(false);
      },
    });
  }
}
