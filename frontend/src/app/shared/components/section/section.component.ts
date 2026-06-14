import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-section',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section [ngClass]="classes">
      <div class="container mx-auto px-4" [ngClass]="{'max-w-7xl': !fullWidth}">
        <ng-content></ng-content>
      </div>
    </section>
  `
})
export class SectionComponent {
  @Input() background: 'default' | 'sand' | 'zellige' = 'default';
  @Input() padding: 'none' | 'sm' | 'md' | 'lg' | 'xl' = 'md';
  @Input() fullWidth = false;

  get classes() {
    return [
      'relative overflow-hidden',
      this.background === 'sand' ? 'bg-background' : '',
      this.background === 'zellige' ? 'bg-zellige-pattern' : '',
      this.padding === 'sm' ? 'py-8' : '',
      this.padding === 'md' ? 'py-16' : '',
      this.padding === 'lg' ? 'py-24' : '',
      this.padding === 'xl' ? 'py-32' : '',
    ].filter(Boolean).join(' ');
  }
}
