import { HttpClient, HttpEvent } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class MediaSevice {

  private apiUrl = '/api/media';

  constructor(private http: HttpClient) { }

  uploadProductImage(file: File) {
    return this.http.post(this.apiUrl + '/products/', file, {
      headers: {
        'Content-Type': file.type 
      },
      responseType: 'text' as const
    });
  }


  isImage(file: File): boolean {
    return file.type.startsWith('image/');
  }

}
