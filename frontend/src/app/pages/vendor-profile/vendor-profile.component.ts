import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { SectionComponent } from '../../shared/components/section/section.component';
import { TypographyComponent } from '../../shared/components/typography/typography.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';
import { Product, products } from '../../core/models/Product';

interface Artisan {
  id: string;
  name: string;
  region: string;
  bio: string;
  image: string;
  badges: string[];
  location: {
    lat: number;
    lng: number;
    address: string;
  };
}

@Component({
  selector: 'app-vendor-profile',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    SectionComponent,
    TypographyComponent,
    BadgeComponent,
    ProductCardComponent
  ],
  templateUrl: './vendor-profile.component.html'
})
export class VendorProfileComponent implements OnInit {
  artisan: Artisan | undefined;
  artisanProducts: Product[] = [];

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    // In a real app, we would fetch this from a service
    // For now, we'll mock it based on the ID or just provide a default
    this.mockArtisanData(id);
  }

  private mockArtisanData(id: string | null): void {
    // Mapping IDs to some of our known artisans from Product.ts
    const artisans: Record<string, Artisan> = {
      '1': {
        id: '1',
        name: 'Fatima Zohra',
        region: 'Middle Atlas',
        bio: 'Fatima Zohra has been weaving traditional Beni Ourain carpets for over 30 years. Using techniques passed down through generations of women in her family, she uses only the finest local wool and natural dyes to create pieces that are both timeless and contemporary.',
        image: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=800&q=80',
        badges: ['Heritage Keeper', 'Master Craftsman'],
        location: {
          lat: 33.5,
          lng: -5.1,
          address: 'Khenifra, Middle Atlas Mountains'
        }
      },
      '2': {
        id: '2',
        name: 'Ahmed El Fassi',
        region: 'Fez',
        bio: 'Ahmed is a master of the "Fez Blue" ceramic tradition. His workshop in the heart of the Fez Medina continues the intricate geometric patterns and cobalt blue glazes that have made Moroccan pottery famous worldwide.',
        image: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=800&q=80',
        badges: ['Master Craftsman'],
        location: {
          lat: 34.033,
          lng: -5.0,
          address: 'Quartier des Potiers, Fez'
        }
      }
    };

    this.artisan = artisans[id || '1'] || artisans['1'];
    
    // Filter products by this artisan's name
    this.artisanProducts = products.filter(p => p.artisan === this.artisan?.name);
  }
}
