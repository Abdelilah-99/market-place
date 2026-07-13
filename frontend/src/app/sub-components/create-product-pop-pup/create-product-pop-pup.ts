import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Product } from '../../core/models/Product';
import { MediaSevice } from '../../core/services/media-sevice';
import { ProductsService } from '../../core/services/products-service';
import { ToasterService } from '../../core/services/toaster-service';

@Component({
  selector: 'app-create-product-pop-pup',
  imports: [FormsModule, CommonModule],
  templateUrl: './create-product-pop-pup.html',
  styleUrl: './create-product-pop-pup.css',
})
export class CreateProductPopPup {

  constructor(
    private mediaSevice: MediaSevice,
    private productsService: ProductsService,
    private toaster: ToasterService
  ) { }

  @Output() closePopUp = new EventEmitter<void>();
  @Output() createdProduct = new EventEmitter<any>();

  product: Partial<Product> = {
    name: '',
    description: '',
    category: '',
    condition: '',
    price: 0,
    quantity: 1,
    image: '',
  };

  categories = [
    'Electronics',
    'Fashion',
    'Home',
    'Books',
    'Sports',
    'Beauty',
    'Toys',
    'Automotive',
    'Other',
  ];

  conditions = [
    { value: 'new', label: 'New' },
    { value: 'used', label: 'Used' },
  ];


  selectedImages = signal<File[]>([]);
  imagePreviews = signal<string[]>([]);
  selectedImage = signal<File | null>(null);
  imagePreview = signal<string | null>(null);
  isSubmitting = signal(false);
  formError = signal('');
  readonly maxImages = 5;
  readonly maxImageSizeMb = 5;

  onImageSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const nextFiles = Array.from(input.files);
    const files = [...this.selectedImages(), ...nextFiles].slice(0, this.maxImages);
    const invalidFile = files.find(file => !this.mediaSevice.isImage(file));
    const oversizedFile = files.find(file => file.size > this.maxImageSizeMb * 1024 * 1024);

    if (invalidFile) {
      this.formError.set('Please select a valid image file.');
      this.toaster.error('Please select a valid image file.');

      this.selectedImages.set([]);
      this.imagePreviews.set([]);

      input.value = '';
      return;
    }

    if (oversizedFile) {
      const message = `Each image must be ${this.maxImageSizeMb}MB or smaller.`;
      this.formError.set(message);
      this.toaster.error(message);
      input.value = '';
      return;
    }

    this.formError.set('');
    this.selectedImages.set(files);
    this.selectedImage.set(files[0] ?? null);
    this.imagePreviews.set(Array(files.length).fill(''));
    this.imagePreview.set(null);

    files.forEach((file, index) => {
      const reader = new FileReader();
      reader.onload = () => {
        if (index === 0) {
          this.imagePreview.set(reader.result as string);
        }
        this.imagePreviews.update(current => {
          const next = [...current];
          next[index] = reader.result as string;
          return next;
        });
      };
      reader.readAsDataURL(file);
    });

    input.value = '';
  }

  removeImage(index = 0) {
    this.selectedImages.update(files => files.filter((_, currentIndex) => currentIndex !== index));
    this.imagePreviews.update(previews => previews.filter((_, currentIndex) => currentIndex !== index));
    this.selectedImage.set(this.selectedImages()[0] ?? null);
    this.imagePreview.set(this.imagePreviews()[0] ?? null);
  }

  selectedImageLabel(): string {
    const files = this.selectedImages();
    if (files.length === 0) return '';

    const totalSizeInMb = files.reduce((total, file) => total + file.size, 0) / (1024 * 1024);
    return `${files.length}/${this.maxImages} images · ${totalSizeInMb.toFixed(2)} MB`;
  }

  listingScore(): number {
    let score = 0;
    if ((this.product.name || '').trim().length >= 8) score += 20;
    if ((this.product.description || '').trim().length >= 40) score += 25;
    if (this.product.category) score += 15;
    if (this.product.condition) score += 10;
    if ((this.product.price || 0) > 0) score += 15;
    if (this.selectedImages().length > 0) score += 15;
    return score;
  }

  listingScoreLabel(): string {
    const score = this.listingScore();
    if (score >= 85) return 'Excellent';
    if (score >= 65) return 'Good';
    if (score >= 40) return 'Needs detail';
    return 'Draft';
  }

  canAddImages(): boolean {
    return this.selectedImages().length < this.maxImages;
  }

  formatPrice(): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      maximumFractionDigits: 2,
    }).format(Number(this.product.price || 0));
  }


  createProduct() {
    const legacyImage = this.selectedImage();
    const files = this.selectedImages().length ? this.selectedImages() : legacyImage ? [legacyImage] : [];
    if (files.length === 0 || this.isSubmitting()) return;

    this.formError.set('');
    this.isSubmitting.set(true);

    forkJoin(files.map(file => this.mediaSevice.uploadProductImage(file))).subscribe({
      next: (imageIds: string[]) => {
        this.product.image = imageIds[0];
        this.product.images = imageIds;

        this.productsService.createProduct(this.product).subscribe({
          next: (res: any) => {
            this.isSubmitting.set(false);
            this.toaster.success('Product listing published.');
            this.createdProduct.emit(res.data);
          },
          error: (err) => {
            console.error('Error creating product:', err);
            const message = 'Product details were saved incorrectly. Please review and try again.';
            this.formError.set(message);
            this.toaster.error(message);
            this.isSubmitting.set(false);
          }
        });

      },
      error: (err) => {
        console.error('Error uploading image:', err);
        const message = 'Image upload failed. Please try another image or upload again.';
        this.formError.set(message);
        this.toaster.error(message);
        this.isSubmitting.set(false);
      }
    });
  }




  close() {
    this.closePopUp.emit();
  }

  hasImages(): boolean {
    return this.imagePreviews().some(Boolean);
  }
}
