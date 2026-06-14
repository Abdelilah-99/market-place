import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SectionComponent } from '../../shared/components/section/section.component';
import { TypographyComponent } from '../../shared/components/typography/typography.component';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';
import { SkeletonComponent } from '../../shared/components/skeleton/skeleton.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { Product, products } from '../../core/models/Product';

@Component({
  selector: 'app-shop',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    SectionComponent,
    TypographyComponent,
    ProductCardComponent,
    SkeletonComponent,
    BadgeComponent
  ],
  templateUrl: './shop.component.html'
})
export class ShopComponent implements OnInit {
  allProducts: Product[] = products;
  filteredProducts: Product[] = [];
  loading = true;

  searchQuery = '';
  selectedCategory = 'All';
  selectedRegion = 'All';
  maxPrice = 5000;

  categories = ['All', 'Textiles', 'Ceramics', 'Food', 'Clothing', 'Woodwork', 'Leather'];
  regions = ['All', 'Fez', 'Marrakech', 'Middle Atlas', 'Essaouira', 'Ifrane', 'Taza'];

  ngOnInit(): void {
    // Simulate loading
    setTimeout(() => {
      this.applyFilters();
      this.loading = false;
    }, 1000);
  }

  applyFilters() {
    this.filteredProducts = this.allProducts.filter(product => {
      const matchesSearch = product.name.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
                            product.description.toLowerCase().includes(this.searchQuery.toLowerCase());
      const matchesCategory = this.selectedCategory === 'All' || product.category === this.selectedCategory;
      const matchesRegion = this.selectedRegion === 'All' || product.region === this.selectedRegion;
      const matchesPrice = product.price <= this.maxPrice;

      return matchesSearch && matchesCategory && matchesRegion && matchesPrice;
    });
  }

  onSearchChange() {
    this.applyFilters();
  }

  setCategory(category: string) {
    this.selectedCategory = category;
    this.applyFilters();
  }

  setRegion(region: string) {
    this.selectedRegion = region;
    this.applyFilters();
  }
}
