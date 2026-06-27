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
    price: 0,
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


  selectedImages = signal<File[]>([]);
  imagePreviews = signal<string[]>([]);
  isSubmitting = signal(false);
  formError = signal('');

  onImageSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const files = Array.from(input.files).slice(0, 5);
    const invalidFile = files.find(file => !this.mediaSevice.isImage(file));

    if (invalidFile) {
      this.formError.set('Please select a valid image file.');
      this.toaster.error('Please select a valid image file.');

      this.selectedImages.set([]);
      this.imagePreviews.set([]);

      input.value = '';
      return;
    }

    this.formError.set('');
    this.selectedImages.set(files);
    this.imagePreviews.set(Array(files.length).fill(''));

    files.forEach((file, index) => {
      const reader = new FileReader();
      reader.onload = () => {
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

  removeImage(index: number) {
    this.selectedImages.update(files => files.filter((_, currentIndex) => currentIndex !== index));
    this.imagePreviews.update(previews => previews.filter((_, currentIndex) => currentIndex !== index));
  }

  selectedImageLabel(): string {
    const files = this.selectedImages();
    if (files.length === 0) return '';

    const totalSizeInMb = files.reduce((total, file) => total + file.size, 0) / (1024 * 1024);
    return `${files.length} image${files.length > 1 ? 's' : ''} · ${totalSizeInMb.toFixed(2)} MB`;
  }


  createProduct() {
    const files = this.selectedImages();
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
