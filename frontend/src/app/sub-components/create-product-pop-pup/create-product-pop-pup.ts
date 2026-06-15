import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Product } from '../../core/models/Product';
import { MediaSevice } from '../../core/services/media-sevice';
import { ProductsService } from '../../core/services/products-service';
import { TypographyComponent } from '../../shared/components/typography/typography.component';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';

@Component({
  selector: 'app-create-product-pop-pup',
  standalone: true,
  imports: [
    FormsModule, 
    CommonModule, 
    TypographyComponent, 
    ButtonComponent, 
    BadgeComponent
  ],
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
    price: 0,
    image: '',
  };


  selectedImage = signal<File | null>(null);
  imagePreview = signal<string | null>(null);

  onImageSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];

    if (!this.mediaSevice.isImage(file)) {
      alert('Please select a valid image file!');

      this.selectedImage.set(null);
      this.imagePreview.set(null);

      input.value = '';
      return;
    }

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


  createProduct() {
    const file = this.selectedImage();
    if (!file) return;

    // Step 1: Upload the image
    this.mediaSevice.uploadProductImage(file).subscribe({
      next: (res: any) => {
        // Step 2: Set the uploaded image URL in the product
        this.product.image = res;

        // Step 3: Create the product
        this.productsService.createProduct(this.product).subscribe({
          next: (res: any) => {
            this.createdProduct.emit(res.data);
          },
          error: (err) => {
            console.error('Error creating product:', err);
          }
        });

      },
      error: (err) => {
        console.error('Error uploading image:', err);
      }
    });
  }




close() {
  this.closePopUp.emit();
}
}
