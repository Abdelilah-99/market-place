import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, Inject, PLATFORM_ID, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Navbar } from './sub-components/navbar/navbar';
import { StateService } from './core/services/state-service';
import { ToasterService } from './core/services/toaster-service';
import { ToastType } from './core/models/ToastType';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, Navbar],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App {
  protected readonly title = signal('frontend');

  constructor(
    private stateService: StateService,
    public toaster: ToasterService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) { }

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      let token = localStorage.getItem("token");
      if (token) this.stateService.getMyInfo();
    }
    // this.stateService.getMyInfo();
  }

  toastIcon(type: ToastType): string {
    const icons: Record<ToastType, string> = {
      success: 'fa fa-check',
      error: 'fa fa-triangle-exclamation',
      warning: 'fa fa-circle-exclamation',
      info: 'fa fa-circle-info',
    };

    return icons[type];
  }
}
