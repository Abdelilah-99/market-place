import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { ToastMessage, ToastType } from '../models/ToastType';


@Injectable({
  providedIn: 'root',
})
export class ToasterService {
  public toaster$ = new BehaviorSubject<ToastMessage | null>(null);
  private timeoutId: ReturnType<typeof setTimeout> | null = null;

  show(message: string, type: ToastType, duration: number = 3000) {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }

    const toast = new ToastMessage(type, message);
    this.toaster$.next(toast);

    this.timeoutId = setTimeout(() => {
      this.clear();
    }, duration);
  }

  clear() {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
      this.timeoutId = null;
    }

    this.toaster$.next(null);
  }

  success(message: string, duration?: number) {
    this.show(message, 'success', duration);
  }

  error(message: string, duration?: number) {
    this.show(message, 'error', duration);
  }

  info(message: string, duration?: number) {
    this.show(message, 'info', duration);
  }

  warning(message: string, duration?: number) {
    this.show(message, 'warning', duration);
  }
}
