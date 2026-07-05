import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ProductItem } from './product';
import { ProductsService } from '../../core/services/products-service';
import { MediaSevice } from '../../core/services/media-sevice';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Product } from '../../core/models/Product';
import { ToasterService } from '../../core/services/toaster-service';

describe('ProductItem Component', () => {
  let component: ProductItem;
  let mockProductsService: any;
  let mockMediaService: any;
  let mockRouter: any;
  let mockToaster: any;
  let mockPurchaseAnalyticsService: any;
  let mockStateService: any;

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

    mockToaster = {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn()
    };

    mockPurchaseAnalyticsService = {
      recordPurchase: vi.fn()
    };

    mockStateService = {
      currentUserSubject: {
        value: {
          id: 'buyer1',
          username: 'Buyer',
          email: 'buyer@example.com',
          role: 'BUYER',
          avatarUrl: null
        }
      }
    };

    component = new ProductItem(
      mockRouter,
      mockProductsService,
      mockMediaService,
      mockToaster as ToasterService,
      mockPurchaseAnalyticsService,
      mockStateService
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

  it('should record a purchase for a logged-in buyer', () => {
    component.buyProduct();

    expect(mockPurchaseAnalyticsService.recordPurchase).toHaveBeenCalledWith(mockProduct, mockStateService.currentUserSubject.value);
    expect(mockToaster.success).toHaveBeenCalledWith('Purchase saved to your profile.');
  });

  it('should send anonymous buyers to login', () => {
    mockStateService.currentUserSubject.value = null;

    component.buyProduct();

    expect(mockPurchaseAnalyticsService.recordPurchase).not.toHaveBeenCalled();
    expect(mockRouter.navigateByUrl).toHaveBeenCalledWith('/login');
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

    component.onImageSelected(mockEvent);

    expect(mockToaster.error).toHaveBeenCalledWith('Please select a valid image file.');
    expect(component.selectedImage()).toBeNull();
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
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    mockMediaService.uploadProductImage.mockReturnValue(
      throwError(() => new Error('Upload failed'))
    );

    component.selectedImage.set(mockFile);
    component.save();

    expect(consoleErrorSpy).toHaveBeenCalled();
    expect(mockToaster.error).toHaveBeenCalledWith('Image upload failed. Please try again.');
    consoleErrorSpy.mockRestore();
  });

  it('should handle update error', () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    mockProductsService.updateProduct.mockReturnValue(
      throwError(() => new Error('Update failed'))
    );

    component.selectedImage.set(null);
    component.save();

    expect(consoleErrorSpy).toHaveBeenCalled();
    expect(mockToaster.error).toHaveBeenCalledWith('Failed to update product. Please try again.');
    consoleErrorSpy.mockRestore();
  });

  // ============================================
  // DELETE TESTS
  // ============================================

  it('should emit deleted product id', () => {
    const emitSpy = vi.spyOn(component.deletedProductId, 'emit');
    mockProductsService.deleteProducts.mockReturnValue(of({}));

    component.delete();

    expect(mockToaster.success).toHaveBeenCalledWith('Product deleted successfully.');
    expect(emitSpy).toHaveBeenCalledWith(mockProduct.id);
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
