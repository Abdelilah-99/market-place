import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-typography',
  standalone: true,
  imports: [CommonModule],
  template: `
    <ng-container [ngSwitch]="variant">
      <h1 *ngSwitchCase="'h1'" [ngClass]="classes"><ng-content></ng-content></h1>
      <h2 *ngSwitchCase="'h2'" [ngClass]="classes"><ng-content></ng-content></h2>
      <h3 *ngSwitchCase="'h3'" [ngClass]="classes"><ng-content></ng-content></h3>
      <h4 *ngSwitchCase="'h4'" [ngClass]="classes"><ng-content></ng-content></h4>
      <h5 *ngSwitchCase="'h5'" [ngClass]="classes"><ng-content></ng-content></h5>
      <h6 *ngSwitchCase="'h6'" [ngClass]="classes"><ng-content></ng-content></h6>
      <p *ngSwitchCase="'body'" [ngClass]="classes"><ng-content></ng-content></p>
      <span *ngSwitchCase="'caption'" [ngClass]="classes"><ng-content></ng-content></span>
      <div *ngSwitchDefault [ngClass]="classes"><ng-content></ng-content></div>
    </ng-container>
  `
})
export class TypographyComponent {
  @Input() variant: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6' | 'body' | 'caption' = 'body';
  @Input() serif = false;
  @Input() customClass = '';

  get classes() {
    const isSerif = this.serif || ['h1', 'h2', 'h3', 'h4', 'h5', 'h6'].includes(this.variant);
    return [
      isSerif ? 'font-serif' : 'font-sans',
      this.variant === 'h1' ? 'text-4xl md:text-6xl font-black leading-tight' : '',
      this.variant === 'h2' ? 'text-3xl md:text-5xl font-bold leading-tight' : '',
      this.variant === 'h3' ? 'text-2xl md:text-4xl font-bold' : '',
      this.variant === 'h4' ? 'text-xl md:text-2xl font-semibold' : '',
      this.variant === 'h5' ? 'text-lg md:text-xl font-semibold' : '',
      this.variant === 'h6' ? 'text-base md:text-lg font-semibold' : '',
      this.variant === 'body' ? 'text-base leading-relaxed' : '',
      this.variant === 'caption' ? 'text-sm opacity-75' : '',
      this.customClass
    ].filter(Boolean).join(' ');
  }
}
