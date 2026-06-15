import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Product } from '../../core/models/Product';
import { ProductsService } from '../../core/services/products-service';
import { SectionComponent } from '../../shared/components/section/section.component';
import { TypographyComponent } from '../../shared/components/typography/typography.component';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';
import { SkeletonComponent } from '../../shared/components/skeleton/skeleton.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule, 
    SectionComponent, 
    TypographyComponent, 
    ButtonComponent, 
    ProductCardComponent, 
    SkeletonComponent,
    BadgeComponent
  ],
  templateUrl: './home.html',
  styleUrls: ['./home.css'],
})
export class Home {
  public products = signal<Product[]>([]);
  public isLoading = signal(true);

  public categories = [
    { name: 'Textiles', icon: '🧵' },
    { name: 'Ceramics', icon: '🏺' },
    { name: 'Leather', icon: '👜' },
    { name: 'Food', icon: '🧂' }
  ];

  constructor(private productsService: ProductsService) {}

  ngOnInit() {
    this.productsService.getAllProducts().subscribe({
      next: (res: any) => {
        this.products.set(res.data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error loading products', err);
        this.isLoading.set(false);
      }
    });
  }
}
