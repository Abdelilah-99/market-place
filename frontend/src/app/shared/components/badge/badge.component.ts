import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span [ngClass]="classes">
      <ng-content></ng-content>
    </span>
  `
})
export class BadgeComponent {
  @Input() variant: 'primary' | 'secondary' | 'accent' | 'neutral' = 'primary';
  @Input() customClass = '';

  get classes() {
    return [
      'inline-flex items-center px-3 py-1 rounded-sm text-[10px] font-bold tracking-[0.18em] uppercase',
      this.variant === 'primary' ? 'bg-primary/10 text-primary' : '',
      this.variant === 'secondary' ? 'bg-secondary/15 text-secondary-dark' : '',
      this.variant === 'accent' ? 'bg-accent text-background' : '',
      this.variant === 'neutral' ? 'bg-neutral/15 text-neutral' : '',
      this.customClass,
    ].filter(Boolean).join(' ');
  }
}
