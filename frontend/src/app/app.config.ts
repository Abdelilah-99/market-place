import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi, withInterceptors } from '@angular/common/http';
import { ApiPrefixInterceptor } from './core/interceptors/api-prefix-interceptor';
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withViewTransitions } from '@angular/router';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { AppConfigService } from './core/services/app-config-service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withInterceptors([authInterceptor]), withInterceptorsFromDi()),
    provideAppInitializer(() => inject(AppConfigService).load()),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ApiPrefixInterceptor,
      multi: true
    },
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withViewTransitions({
        skipInitialTransition: true
      })
    )
  ]
};
