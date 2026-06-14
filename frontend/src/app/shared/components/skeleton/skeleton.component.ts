import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div [ngClass]="classes" [style.width]="width" [style.height]="height" class="animate-pulse bg-accent/10"></div>
  `
})
export class SkeletonComponent {
  @Input() variant: 'text' | 'rect' | 'circle' = 'rect';
  @Input() width: string = '100%';
  @Input() height: string = '1rem';
  @Input() rounded: 'sm' | 'md' | 'lg' | 'xl' | 'full' = 'md';

  get classes() {
    return [
      this.variant === 'circle' ? 'rounded-full' : '',
      this.variant === 'rect' ? {
        'rounded-sm': this.rounded === 'sm',
        'rounded-md': this.rounded === 'md',
        'rounded-lg': this.rounded === 'lg',
        'rounded-xl': this.rounded === 'xl',
        'rounded-full': this.rounded === 'full'
      } : '',
      this.variant === 'text' ? 'rounded-sm' : ''
    ];
  }
}
