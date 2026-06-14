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

  get classes() {
    return [
      'inline-flex items-center px-3 py-1 rounded-sm text-[10px] font-black tracking-widest uppercase',
      this.variant === 'primary' ? 'bg-primary text-white' : '',
      this.variant === 'secondary' ? 'bg-secondary text-accent' : '',
      this.variant === 'accent' ? 'bg-accent text-white' : '',
      this.variant === 'neutral' ? 'bg-neutral text-white' : '',
    ].filter(Boolean).join(' ');
  }
}
