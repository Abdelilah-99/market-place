import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Product } from '../../core/models/Product';
import { MediaSevice } from '../../core/services/media-sevice';
import { ProductsService } from '../../core/services/products-service';

@Component({
  selector: 'app-create-product-pop-pup',
  imports: [FormsModule, CommonModule],
  templateUrl: './create-product-pop-pup.html',
  styleUrl: './create-product-pop-pup.css',
})
export class CreateProductPopPup {

  constructor(private mediaSevice: MediaSevice, private productsService: ProductsService) { }

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


  selectedImage = signal<File | null>(null);
  imagePreview = signal<string | null>(null);
  isSubmitting = signal(false);
  formError = signal('');

  onImageSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];

    if (!this.mediaSevice.isImage(file)) {
      this.formError.set('Please select a valid image file.');

      this.selectedImage.set(null);
      this.imagePreview.set(null);

      input.value = '';
      return;
    }

    this.formError.set('');
    this.selectedImage.set(file);

    const reader = new FileReader();
    reader.onload = () => {
      this.imagePreview.set(reader.result as string);
      this.product.image = reader.result as string;
    };
    reader.readAsDataURL(file);

    input.value = '';
  }

  removeImage() {
    this.selectedImage.set(null);
    this.imagePreview.set(null);
  }

  selectedImageLabel(): string {
    const file = this.selectedImage();
    if (!file) return '';

    const sizeInMb = file.size / (1024 * 1024);
    return `${file.name} · ${sizeInMb.toFixed(2)} MB`;
  }


  createProduct() {
    const file = this.selectedImage();
    if (!file || this.isSubmitting()) return;

    this.formError.set('');
    this.isSubmitting.set(true);

    // Step 1: Upload the image
    this.mediaSevice.uploadProductImage(file).subscribe({
      next: (res: any) => {
        // Step 2: Set the uploaded image URL in the product
        this.product.image = res;

        // Step 3: Create the product
        this.productsService.createProduct(this.product).subscribe({
          next: (res: any) => {
            this.isSubmitting.set(false);
            this.createdProduct.emit(res.data);
          },
          error: (err) => {
            console.error('Error creating product:', err);
            this.formError.set('Product details were saved incorrectly. Please review and try again.');
            this.isSubmitting.set(false);
          }
        });

      },
      error: (err) => {
        console.error('Error uploading image:', err);
        this.formError.set('Image upload failed. Please try another image or upload again.');
        this.isSubmitting.set(false);
      }
    });
  }




  close() {
    this.closePopUp.emit();
  }
}
