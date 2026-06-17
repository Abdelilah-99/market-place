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
  @Input() disabled: boolean | null | undefined = false;
  @Input() fullWidth = false;
  @Input() customClass = '';
  @Output() onClick = new EventEmitter<MouseEvent>();

  get classes() {
    return [
      'inline-flex items-center justify-center gap-2 font-sans font-semibold tracking-wide cursor-pointer transition-all duration-300 ease-out disabled:opacity-40 disabled:cursor-not-allowed active:scale-[0.98]',
      this.variant === 'primary' ? 'bg-primary text-white hover:bg-primary-dark shadow-elevation-1' : '',
      this.variant === 'secondary' ? 'bg-secondary text-accent hover:bg-secondary-dark hover:text-white shadow-elevation-1' : '',
      this.variant === 'outline' ? 'border border-primary/40 text-primary hover:border-primary hover:bg-primary/5' : '',
      this.variant === 'ghost' ? 'text-accent hover:text-primary' : '',
      this.size === 'sm' ? 'px-4 py-2 text-xs rounded-sm' : '',
      this.size === 'md' ? 'px-6 py-2.5 text-sm rounded-sm' : '',
      this.size === 'lg' ? 'px-8 py-3.5 text-base rounded-sm' : '',
      this.fullWidth ? 'w-full' : '',
      this.customClass,
    ].filter(Boolean).join(' ');
  }
}
