import { Component, Input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SettingsService } from '../../../core/services/settings.service';

@Component({
  selector: 'app-price-tag',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex items-baseline gap-1 font-sans" [ngClass]="customClass">
      <span class="text-xs font-semibold text-accent/60 uppercase">{{ displayData().currency }}</span>
      <span [ngClass]="sizeClasses">{{ displayData().amount | number:'1.2-2' }}</span>
    </div>
  `
})
export class PriceTagComponent {
  @Input() amount: number = 0;
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() customClass: string = '';

  displayData = computed(() => this.settingsService.convert(this.amount));

  constructor(private settingsService: SettingsService) {}

  get sizeClasses() {
    return [
      'font-bold text-accent',
      this.size === 'sm' ? 'text-lg' : '',
      this.size === 'md' ? 'text-2xl' : '',
      this.size === 'lg' ? 'text-4xl' : '',
    ].filter(Boolean).join(' ');
  }
}
