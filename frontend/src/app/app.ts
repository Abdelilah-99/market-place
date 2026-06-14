import { Component, Inject, PLATFORM_ID, signal, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Navbar } from './sub-components/navbar/navbar';
import { FooterComponent } from './shared/components/footer/footer.component';
import { StateService } from './core/services/state-service';
import { LanguageService } from './core/services/language.service';
import { isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Navbar, FooterComponent],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App implements OnInit {
  protected readonly title = signal('frontend');

  constructor(
    private stateService: StateService,
    private languageService: LanguageService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) { }

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.languageService.initLanguage();
      let token = localStorage.getItem("token");
      if (token) this.stateService.getMyInfo();
    }
  }
}
