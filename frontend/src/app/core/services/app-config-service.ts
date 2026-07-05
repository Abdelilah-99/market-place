import { Injectable } from '@angular/core';

export interface RuntimeAppConfig {
  apiBaseUrl: string;
}

@Injectable({
  providedIn: 'root',
})
export class AppConfigService {
  private config: RuntimeAppConfig = {
    apiBaseUrl: '',
  };

  async load(): Promise<void> {
    try {
      const response = await fetch('/app-config.json', { cache: 'no-store' });
      if (!response.ok) {
        return;
      }

      const config = await response.json() as Partial<RuntimeAppConfig>;
      this.config = {
        apiBaseUrl: this.normalizeBaseUrl(config.apiBaseUrl || ''),
      };
    } catch {
      this.config = { apiBaseUrl: '' };
    }
  }

  apiUrl(path: string): string {
    if (!this.config.apiBaseUrl || !path.startsWith('/api')) {
      return path;
    }

    return `${this.config.apiBaseUrl}${path}`;
  }

  private normalizeBaseUrl(url: string): string {
    return url.trim().replace(/\/+$/, '');
  }
}
