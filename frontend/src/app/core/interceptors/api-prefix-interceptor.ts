import { Injectable } from '@angular/core';
import {
    HttpInterceptor,
    HttpRequest,
    HttpHandler,
    HttpEvent
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AppConfigService } from '../services/app-config-service';

@Injectable()
export class ApiPrefixInterceptor implements HttpInterceptor {
    constructor(private appConfigService: AppConfigService) {}

    intercept(
        req: HttpRequest<any>,
        next: HttpHandler
    ): Observable<HttpEvent<any>> {
        return next.handle(req.clone({ url: this.appConfigService.apiUrl(req.url) }));
    }
}
