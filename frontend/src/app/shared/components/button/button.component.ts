import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button [ngClass]="classes" [disabled]="disabled" (click)="onClick.emit($event)">
      <ng-content></ng-content>
    </button>
  `
})
export class ButtonComponent {
  @Input() variant: 'primary' | 'secondary' | 'outline' | 'ghost' = 'primary';
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() disabled = false;
  @Input() fullWidth = false;
  @Output() onClick = new EventEmitter<MouseEvent>();

  get classes() {
    return [
      'inline-flex items-center justify-center transition-all duration-300 font-sans font-semibold cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed active:scale-95',
      this.variant === 'primary' ? 'bg-primary text-white hover:bg-primary/90 shadow-elevation-1 hover:shadow-elevation-premium' : '',
      this.variant === 'secondary' ? 'bg-secondary text-accent hover:bg-secondary/90 shadow-elevation-1' : '',
      this.variant === 'outline' ? 'border-2 border-primary text-primary hover:bg-primary/5' : '',
      this.variant === 'ghost' ? 'text-accent hover:bg-accent/5' : '',
      this.size === 'sm' ? 'px-4 py-2 text-sm rounded-sm' : '',
      this.size === 'md' ? 'px-6 py-3 text-base rounded-lg' : '',
      this.size === 'lg' ? 'px-8 py-4 text-lg rounded-xl' : '',
      this.fullWidth ? 'w-full' : '',
    ].filter(Boolean).join(' ');
  }
}
