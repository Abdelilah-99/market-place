import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ProductItem } from './product';
import { ProductsService } from '../../core/services/products-service';
import { MediaSevice } from '../../core/services/media-sevice';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Product } from '../../core/models/Product';

describe('ProductItem Component', () => {
  let component: ProductItem;
  let mockProductsService: any;
  let mockMediaService: any;
  let mockRouter: any;

  const mockProduct: Product = {
    id: '1',
    name: 'Test Product',
    description: 'Test Description',
    price: 99.99,
    image: 'image.jpg',
    userId: 'user1'
  };

  beforeEach(() => {
    mockProductsService = {
      updateProduct: vi.fn(),
      deleteProducts: vi.fn()
    };

    mockMediaService = {
      uploadProductImage: vi.fn()
    };

    mockRouter = {
      url: '/home',
      navigateByUrl: vi.fn()
    };

    component = new ProductItem(
      mockRouter,
      mockProductsService,
      mockMediaService
    );

    component.product = mockProduct;
  });

  // ============================================
  // SERVICE CREATION TESTS
  // ============================================

  it('should create the product item component', () => {
    expect(component).toBeDefined();
  });

  it('should initialize with product not being edited', () => {
    expect(component.isEditing).toBe(false);
  });

  it('should initialize with null image preview', () => {
    expect(component.imagePreview()).toBeNull();
  });

  it('should initialize with null selected image', () => {
    expect(component.selectedImage()).toBeNull();
  });

  // ============================================
  // INIT TESTS
  // ============================================

  it('should clone product on init', () => {
    component.ngOnInit();

    expect(component.updatedProduct).toEqual(mockProduct);
    expect(component.updatedProduct).not.toBe(mockProduct);
  });

  it('should determine if product belongs to current user', () => {
    mockRouter.url = '/dashboard';
    component.ngOnInit();

    expect(component.isMyProduct).toBe(true);
  });

  it('should determine if product does not belong to current user', () => {
    mockRouter.url = '/home';
    component.ngOnInit();

    expect(component.isMyProduct).toBe(false);
  });

  // ============================================
  // IMAGE SELECTION TESTS
  // ============================================

  it('should reject non-image file', () => {
    const mockFile = new File(['pdf-data'], 'document.pdf', { type: 'application/pdf' });
    const mockEvent = {
      target: {
        files: [mockFile]
      }
    } as any;

    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    component.onImageSelected(mockEvent);

    expect(alertSpy).toHaveBeenCalledWith('Please select a valid image file!');
    expect(component.selectedImage()).toBeNull();
    alertSpy.mockRestore();
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

  // ============================================
  // SAVE TESTS
  // ============================================

  it('should save product without new image', () => {
    component.ngOnInit();
    component.selectedImage.set(null);
    mockProductsService.updateProduct.mockReturnValue(of({ data: component.updatedProduct }));

    component.save();

    expect(mockProductsService.updateProduct).toHaveBeenCalledWith(
      component.product.id,
      component.updatedProduct
    );
  });

  it('should handle image upload error', () => {
    const mockFile = new File(['image-data'], 'product.jpg', { type: 'image/jpeg' });
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    mockMediaService.uploadProductImage.mockReturnValue(
      throwError(() => new Error('Upload failed'))
    );

    component.selectedImage.set(mockFile);
    component.save();

    expect(consoleErrorSpy).toHaveBeenCalled();
    alertSpy.mockRestore();
    consoleErrorSpy.mockRestore();
  });

  it('should handle update error', () => {
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    mockProductsService.updateProduct.mockReturnValue(
      throwError(() => new Error('Update failed'))
    );

    component.selectedImage.set(null);
    component.save();

    expect(consoleErrorSpy).toHaveBeenCalled();
    alertSpy.mockRestore();
    consoleErrorSpy.mockRestore();
  });

  // ============================================
  // DELETE TESTS
  // ============================================

  it('should emit deleted product id', () => {
    const emitSpy = vi.spyOn(component.deletedProductId, 'emit');
    mockProductsService.deleteProducts.mockReturnValue(of({}));

    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

    component.delete();

    expect(confirmSpy).toHaveBeenCalled();
    expect(emitSpy).toHaveBeenCalledWith(mockProduct.id);
    confirmSpy.mockRestore();
  });

  // ============================================
  // EDIT MODE TESTS
  // ============================================

  it('should toggle edit mode', () => {
    expect(component.isEditing).toBe(false);

    component.isEditing = true;
    expect(component.isEditing).toBe(true);

    component.isEditing = false;
    expect(component.isEditing).toBe(false);
  });

  // ============================================
  // CLOSE UPDATE TESTS
  // ============================================

  it('should close edit mode and reset image', () => {
    component.isEditing = true;
    component.selectedImage.set(new File(['data'], 'image.jpg'));

    component.closeUpdate();

    expect(component.isEditing).toBe(false);
    expect(component.selectedImage()).toBeNull();
    expect(component.imagePreview()).toBeNull();
  });

  // ============================================
  // EDGE CASE TESTS
  // ============================================

  it('should handle product with no image', () => {
    const productNoImage: Product = { ...mockProduct, image: '' };
    component.product = productNoImage;
    component.ngOnInit();

    expect(component.updatedProduct.image).toBe('');
  });

  it('should handle product price update', () => {
    component.ngOnInit();
    component.updatedProduct.price = 150.00;

    mockProductsService.updateProduct.mockReturnValue(of({ data: component.updatedProduct }));

    component.selectedImage.set(null);
    component.save();

    expect(mockProductsService.updateProduct).toHaveBeenCalledWith(
      component.product.id,
      expect.objectContaining({ price: 150.00 })
    );
  });
});
