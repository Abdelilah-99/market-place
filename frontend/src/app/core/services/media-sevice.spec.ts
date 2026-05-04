import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MediaSevice } from './media-sevice';

describe('MediaService', () => {
  let service: MediaSevice;
  let httpMock: HttpTestingController;
  const apiUrl = '/api/media';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MediaSevice]
    });

    service = TestBed.inject(MediaSevice);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ============================================
  // SERVICE CREATION
  // ============================================

  it('should create the media service', () => {
    expect(service).toBeDefined();
  });

  // ============================================
  // UPLOAD PRODUCT IMAGE TESTS
  // ============================================

  it('should upload product image with correct endpoint', () => {
    const mockFile = new File(['image-data'], 'product.jpg', { type: 'image/jpeg' });
    const mockResponse = { id: 'image123', url: 'path/to/image.jpg' };

    service.uploadProductImage(mockFile).subscribe((response) => {
      expect(response).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(`${apiUrl}/products/`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBe(mockFile);
    expect(req.request.headers.get('Content-Type')).toBe('image/jpeg');
    req.flush(mockResponse);
  });

  it('should send image with correct content type', () => {
    const pngFile = new File(['png-data'], 'image.png', { type: 'image/png' });
    
    service.uploadProductImage(pngFile).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/products/`);
    expect(req.request.headers.get('Content-Type')).toBe('image/png');
    req.flush({ url: 'path/to/image.png' });
  });

  it('should handle image upload with webp format', () => {
    const webpFile = new File(['webp-data'], 'image.webp', { type: 'image/webp' });
    
    service.uploadProductImage(webpFile).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/products/`);
    expect(req.request.headers.get('Content-Type')).toBe('image/webp');
    req.flush({ url: 'path/to/image.webp' });
  });

  it('should handle image upload with gif format', () => {
    const gifFile = new File(['gif-data'], 'image.gif', { type: 'image/gif' });
    
    service.uploadProductImage(gifFile).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/products/`);
    expect(req.request.headers.get('Content-Type')).toBe('image/gif');
    req.flush({ url: 'path/to/image.gif' });
  });

  it('should handle upload error', () => {
    const mockFile = new File(['image-data'], 'product.jpg', { type: 'image/jpeg' });

    service.uploadProductImage(mockFile).subscribe({
      next: () => expect(true).toBe(false),
      error: (error) => {
        expect(error.status).toBe(400);
      }
    });

    const req = httpMock.expectOne(`${apiUrl}/products/`);
    req.flush('Invalid file', { status: 400, statusText: 'Bad Request' });
  });

  it('should handle server error during upload', () => {
    const mockFile = new File(['image-data'], 'product.jpg', { type: 'image/jpeg' });

    service.uploadProductImage(mockFile).subscribe({
      next: () => expect(true).toBe(false),
      error: (error) => {
        expect(error.status).toBe(500);
      }
    });

    const req = httpMock.expectOne(`${apiUrl}/products/`);
    req.flush('Internal Server Error', { status: 500, statusText: 'Internal Server Error' });
  });

  // ============================================
  // IMAGE VALIDATION TESTS
  // ============================================

  it('should validate JPEG image', () => {
    const jpegFile = new File(['jpeg-data'], 'image.jpg', { type: 'image/jpeg' });
    expect(service.isImage(jpegFile)).toBe(true);
  });

  it('should validate PNG image', () => {
    const pngFile = new File(['png-data'], 'image.png', { type: 'image/png' });
    expect(service.isImage(pngFile)).toBe(true);
  });

  it('should validate GIF image', () => {
    const gifFile = new File(['gif-data'], 'image.gif', { type: 'image/gif' });
    expect(service.isImage(gifFile)).toBe(true);
  });

  it('should validate WebP image', () => {
    const webpFile = new File(['webp-data'], 'image.webp', { type: 'image/webp' });
    expect(service.isImage(webpFile)).toBe(true);
  });

  it('should validate SVG image', () => {
    const svgFile = new File(['svg-data'], 'image.svg', { type: 'image/svg+xml' });
    expect(service.isImage(svgFile)).toBe(true);
  });

  it('should reject non-image file (PDF)', () => {
    const pdfFile = new File(['pdf-data'], 'document.pdf', { type: 'application/pdf' });
    expect(service.isImage(pdfFile)).toBe(false);
  });

  it('should reject non-image file (Text)', () => {
    const textFile = new File(['text-data'], 'document.txt', { type: 'text/plain' });
    expect(service.isImage(textFile)).toBe(false);
  });

  it('should reject non-image file (JSON)', () => {
    const jsonFile = new File(['{"data": "json"}'], 'data.json', { type: 'application/json' });
    expect(service.isImage(jsonFile)).toBe(false);
  });

  it('should reject non-image file (Word)', () => {
    const docxFile = new File(['doc-data'], 'document.docx', { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' });
    expect(service.isImage(docxFile)).toBe(false);
  });

  it('should reject video file', () => {
    const videoFile = new File(['video-data'], 'video.mp4', { type: 'video/mp4' });
    expect(service.isImage(videoFile)).toBe(false);
  });

  it('should reject audio file', () => {
    const audioFile = new File(['audio-data'], 'song.mp3', { type: 'audio/mpeg' });
    expect(service.isImage(audioFile)).toBe(false);
  });

  // ============================================
  // EDGE CASE TESTS
  // ============================================

  it('should handle file with no extension', () => {
    const noExtFile = new File(['image-data'], 'imagefile', { type: 'image/jpeg' });
    expect(service.isImage(noExtFile)).toBe(true);
  });

  it('should handle file with multiple dots in name', () => {
    const multiDotFile = new File(['image-data'], 'image.backup.jpg', { type: 'image/jpeg' });
    expect(service.isImage(multiDotFile)).toBe(true);
  });

  it('should upload file with special characters in name', () => {
    const specialFile = new File(['image-data'], 'my image (1) copy.jpg', { type: 'image/jpeg' });
    
    service.uploadProductImage(specialFile).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/products/`);
    expect(req.request.headers.get('Content-Type')).toBe('image/jpeg');
    req.flush({ url: 'path/to/image.jpg' });
  });

  it('should handle large image file', () => {
    // Create a 5MB image file
    const largeFile = new File([new ArrayBuffer(5 * 1024 * 1024)], 'large-image.jpg', { type: 'image/jpeg' });
    
    service.uploadProductImage(largeFile).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/products/`);
    expect(req.request.body).toBe(largeFile);
    req.flush({ url: 'path/to/large-image.jpg' });
  });
});
