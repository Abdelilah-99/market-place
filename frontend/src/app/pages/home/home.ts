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
    SkeletonComponent
  ],
  templateUrl: './home.html',
  styleUrls: ['./home.css'],
})
export class Home {
  public products = signal<Product[]>([]);
  public isLoading = signal(true);

  public categories = [
    { name: 'Textiles', icon: '🧵', count: 120, image: 'https://images.unsplash.com/photo-1576016773942-31757724f791?auto=format&fit=crop&w=400&q=80' },
    { name: 'Ceramics', icon: '🏺', count: 85, image: 'https://images.unsplash.com/photo-1610701596007-11502861dcfa?auto=format&fit=crop&w=400&q=80' },
    { name: 'Leather', icon: '👜', count: 64, image: 'https://images.unsplash.com/photo-1548036328-c9fa89d128fa?auto=format&fit=crop&w=400&q=80' },
    { name: 'Food', icon: '🧂', count: 42, image: 'https://images.unsplash.com/photo-1608500218890-c4f923e38707?auto=format&fit=crop&w=400&q=80' }
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
