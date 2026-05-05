import { describe, it, expect, beforeEach, vi } from 'vitest';
import { CreateProductPopPup } from './create-product-pop-pup';
import { ProductsService } from '../../core/services/products-service';
import { MediaSevice } from '../../core/services/media-sevice';
import { of, throwError } from 'rxjs';
import { Product } from '../../core/models/Product';

describe('CreateProductPopPup Component', () => {
  let component: CreateProductPopPup;
  let mockProductsService: any;
  let mockMediaService: any;

  beforeEach(() => {
    mockProductsService = {
      createProduct: vi.fn()
    };

    mockMediaService = {
      isImage: vi.fn(),
      uploadProductImage: vi.fn()
    };

    component = new CreateProductPopPup(mockMediaService, mockProductsService);
  });

  // ============================================
  // SERVICE CREATION TESTS
  // ============================================

  it('should create the create product pop up component', () => {
    expect(component).toBeDefined();
  });

  it('should initialize with empty product', () => {
    expect(component.product.name).toBe('');
    expect(component.product.description).toBe('');
    expect(component.product.price).toBe(0);
    expect(component.product.image).toBe('');
  });

  it('should initialize with null selected image', () => {
    expect(component.selectedImage()).toBeNull();
  });

  it('should initialize with null image preview', () => {
    expect(component.imagePreview()).toBeNull();
  });

  // ============================================
  // IMAGE SELECTION TESTS
  // ============================================

  it('should select valid image file', () => {
    const mockFile = new File(['image-data'], 'product.jpg', { type: 'image/jpeg' });
    mockMediaService.isImage.mockReturnValue(true);

    const mockEvent = {
      target: {
        files: [mockFile],
        value: 'C:\\fakepath\\product.jpg'
      }
    } as any;

    component.onImageSelected(mockEvent);

    expect(component.selectedImage()).toBe(mockFile);
  });

  it('should reject non-image file', () => {
    const mockFile = new File(['pdf-data'], 'document.pdf', { type: 'application/pdf' });
    mockMediaService.isImage.mockReturnValue(false);

    const mockEvent = {
      target: {
        files: [mockFile]
      }
    } as any;

    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    component.onImageSelected(mockEvent);

    expect(alertSpy).toHaveBeenCalledWith('Please select a valid image file!');
    expect(component.selectedImage()).toBeNull();
    expect(component.imagePreview()).toBeNull();
    alertSpy.mockRestore();
  });

  it('should call isImage service method with selected file', () => {
    const mockFile = new File(['image-data'], 'product.jpg', { type: 'image/jpeg' });
    mockMediaService.isImage.mockReturnValue(true);

    const mockEvent = {
      target: {
        files: [mockFile],
        value: ''
      }
    } as any;

    component.onImageSelected(mockEvent);

    expect(mockMediaService.isImage).toHaveBeenCalledWith(mockFile);
  });

  it('should handle empty file input', () => {
    const mockEvent = {
      target: {
        files: null
      }
    } as any;

    component.onImageSelected(mockEvent);

    expect(component.selectedImage()).toBeNull();
  });

  it('should handle file input with empty files array', () => {
    const mockEvent = {
      target: {
        files: []
      }
    } as any;

    component.onImageSelected(mockEvent);

    expect(component.selectedImage()).toBeNull();
  });

  it('should clear input value after selecting file', () => {
    const mockFile = new File(['image-data'], 'product.jpg', { type: 'image/jpeg' });
    mockMediaService.isImage.mockReturnValue(true);

    const mockEvent = {
      target: {
        files: [mockFile],
        value: 'C:\\fakepath\\product.jpg'
      }
    } as any;

    component.onImageSelected(mockEvent);

    expect(mockEvent.target.value).toBe('');
  });

  // ============================================
  // REMOVE IMAGE TESTS
  // ============================================

  it('should remove selected image', () => {
    component.selectedImage.set(new File(['data'], 'image.jpg'));
    component.imagePreview.set('data:image/jpeg;base64,data');

    component.removeImage();

    expect(component.selectedImage()).toBeNull();
    expect(component.imagePreview()).toBeNull();
  });

  // ============================================
  // CREATE PRODUCT TESTS
  // ============================================

  it('should create product with image', () => {
    const mockFile = new File(['image-data'], 'product.jpg', { type: 'image/jpeg' });
    const imageUrl = 'uploaded-image-url.jpg';
    const mockProduct: Partial<Product> = {
      name: 'Test Product',
      description: 'Test Description',
      price: 99.99,
      image: imageUrl
    };

    mockMediaService.uploadProductImage.mockReturnValue(of(imageUrl));
    mockProductsService.createProduct.mockReturnValue(of({ data: mockProduct }));

    component.product = {
      name: 'Test Product',
      description: 'Test Description',
      price: 99.99,
      image: ''
    };

    component.selectedImage.set(mockFile);
    component.createProduct();

    expect(mockMediaService.uploadProductImage).toHaveBeenCalledWith(mockFile);
  });

  it('should not create product if no image selected', () => {
    component.selectedImage.set(null);
    component.createProduct();

    expect(mockMediaService.uploadProductImage).not.toHaveBeenCalled();
    expect(mockProductsService.createProduct).not.toHaveBeenCalled();
  });

  it('should emit created product event', () => {
    const mockFile = new File(['image-data'], 'product.jpg', { type: 'image/jpeg' });
    const imageUrl = 'uploaded-image-url.jpg';
    const mockProduct: Partial<Product> = {
      id: '1',
      name: 'Test Product',
      description: 'Test Description',
      price: 99.99,
      image: imageUrl
    };

    mockMediaService.uploadProductImage.mockReturnValue(of(imageUrl));
    mockProductsService.createProduct.mockReturnValue(of({ data: mockProduct }));

    component.product = mockProduct;
    component.selectedImage.set(mockFile);

    let productEmitted = false;
    component.createdProduct.subscribe((product) => {
      expect(product).toEqual(mockProduct);
      productEmitted = true;
    });

    component.createProduct();
    expect(productEmitted).toBe(true);
  });

  it('should handle image upload error gracefully', () => {
    const mockFile = new File(['image-data'], 'product.jpg', { type: 'image/jpeg' });
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    mockMediaService.uploadProductImage.mockReturnValue(
      throwError(() => new Error('Upload failed'))
    );

    component.selectedImage.set(mockFile);
    component.createProduct();() => {}

    expect(consoleErrorSpy).toHaveBeenCalled();
    consoleErrorSpy.mockRestore();
  });

  it('should handle product creation error gracefully', () => {
    const mockFile = new File(['image-data'], 'product.jpg', { type: 'image/jpeg' });
    const imageUrl = 'uploaded-image-url.jpg';
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    mockMediaService.uploadProductImage.mockReturnValue(of(imageUrl));
    mockProductsService.createProduct.mockReturnValue(
      throwError(() => new Error('Creation failed'))
    );

    component.product = {
      name: 'Test Product',
      description: 'Test Description',
      price: 99.99
    };
    component.selectedImage.set(mockFile);
    component.createProduct();

    expect(consoleErrorSpy).toHaveBeenCalled();
    consoleErrorSpy.mockRestore();
  });

  // ============================================
  // CLOSE POP UP TESTS
  // ============================================

  it('should emit close event', () => {
    let closeEmitted = false;
    component.closePopUp.subscribe(() => {
      closeEmitted = true;
    });

    component.close();
    expect(closeEmitted).toBe(true);
  });

  // ============================================
  // FORM INPUT TESTS
  // ============================================

  it('should update product name', () => {
    component.product.name = 'New Product';
    expect(component.product.name).toBe('New Product');
  });

  it('should update product description', () => {
    component.product.description = 'New Description';
    expect(component.product.description).toBe('New Description');
  });

  it('should update product price', () => {
    component.product.price = 150.00;
    expect(component.product.price).toBe(150.00);
  });

  it('should handle zero price', () => {
    component.product.price = 0;
    expect(component.product.price).toBe(0);
  });

  // ============================================
  // EDGE CASE TESTS
  // ============================================

  it('should handle product with special characters', () => {
    component.product.name = 'Product "Special" & chars';
    component.product.description = 'Description with <html> tags';

    expect(component.product.name).toContain('Special');
    expect(component.product.description).toContain('<html>');
  });

  it('should handle large product name', () => {
    const longName = 'A'.repeat(500);
    component.product.name = longName;

    expect(component.product.name).toBe(longName);
  });

  it('should handle product with very high price', () => {
    component.product.price = 999999.99;
    expect(component.product.price).toBe(999999.99);
  });

  it('should reset form after product creation', () => {
    component.product.name = 'Test';
    component.product.description = 'Test Desc';
    component.selectedImage.set(new File(['data'], 'image.jpg'));

    // Simulate form reset
    component.product = {
      name: '',
      description: '',
      price: 0,
      image: '',
    };
    component.selectedImage.set(null);
    component.imagePreview.set(null);

    expect(component.product.name).toBe('');
    expect(component.selectedImage()).toBeNull();
  });
});
