import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CreateProductPopPup } from '../../sub-components/create-product-pop-pup/create-product-pop-pup';
import { Product } from '../../core/models/Product';
import { ProductsService } from '../../core/services/products-service';
import { TypographyComponent } from '../../shared/components/typography/typography.component';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { SectionComponent } from '../../shared/components/section/section.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [
    CreateProductPopPup, 
    CommonModule, 
    TypographyComponent, 
    ButtonComponent, 
    SectionComponent, 
    BadgeComponent,
    ProductCardComponent
  ],
  templateUrl: './seller-dashboard.html',
  styleUrl: './seller-dashboard.css',
})
export class SellerDashboard {
  public isPopPupOpen: boolean = false;

  public products = signal<Product[]>([]);

  constructor(private productsService: ProductsService) { }

  ngOnInit() {
    this.productsService.getMyProducts().subscribe({
      next: (res: any) => {
        this.products.set(res.data);
      },
      error: (err) => {
        console.error('Error loading products', err);
      }
    });
  }

  onProductCreated(product: Product) {
    this.products.update(current => [product, ...current]);
    this.isPopPupOpen = false;
  }

  deleteProductById(id: string) {
    this.products.set(
      this.products().filter(product => product.id !== id)
    );
  }


  public togglePopUp() {
    this.isPopPupOpen = !this.isPopPupOpen;
  }
}
