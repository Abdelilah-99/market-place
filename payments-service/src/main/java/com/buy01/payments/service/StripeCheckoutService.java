package com.buy01.payments.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.buy01.payments.dto.CheckoutSessionResponse;
import com.buy01.payments.dto.CreateCheckoutSessionRequest;
import com.buy01.payments.dto.ProductSnapshot;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class StripeCheckoutService {
    private final CheckoutSessionCreator sessionCreator;
    private final ProductLookup productLookup;
    private final String appPublicUrl;

    @Autowired
    public StripeCheckoutService(
            @Value("${stripe.secret-key:}") String stripeSecretKey,
            @Value("${app.public-url:http://localhost:4200}") String appPublicUrl,
            ProductClient productClient) {
        String normalizedKey = stripeSecretKey == null ? "" : stripeSecretKey.trim();
        this.sessionCreator = normalizedKey.isBlank()
                ? null
                : new StripeClient(normalizedKey).checkout().sessions()::create;
        this.appPublicUrl = stripTrailingSlash(appPublicUrl);
        this.productLookup = productClient::getAvailableProduct;
    }

    StripeCheckoutService(CheckoutSessionCreator sessionCreator, String appPublicUrl) {
        this(sessionCreator, appPublicUrl,
                (productId, quantity) -> new ProductSnapshot(productId, "Test product",
                        new BigDecimal("10.505"), quantity, null));
    }

    public StripeCheckoutService(String ignoredStripeKey, String appPublicUrl) {
        this.sessionCreator = null;
        this.appPublicUrl = stripTrailingSlash(appPublicUrl);
        this.productLookup = (productId, quantity) -> new ProductSnapshot(productId, "Product",
                BigDecimal.ONE, quantity, null);
    }

    StripeCheckoutService(CheckoutSessionCreator sessionCreator, String appPublicUrl, ProductLookup productLookup) {
        this.sessionCreator = sessionCreator;
        this.appPublicUrl = stripTrailingSlash(appPublicUrl);
        this.productLookup = productLookup;
    }

    public CheckoutSessionResponse createCheckoutSession(
            CreateCheckoutSessionRequest request,
            String userId) throws StripeException {
        if (sessionCreator == null) {
            throw new IllegalStateException("Stripe secret key is not configured");
        }
        ProductSnapshot product = productLookup.get(request.productId(), request.quantity());
        String currency = "usd";

        SessionCreateParams.LineItem.PriceData.ProductData.Builder productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(product.name())
                        .putMetadata("product_id", request.productId());

        if (product.imageUrl() != null && !product.imageUrl().isBlank()) {
            productData.addImage(product.imageUrl());
        }

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(appPublicUrl + "/products/" + request.productId() + "?payment=success")
                .setCancelUrl(appPublicUrl + "/products/" + request.productId() + "?payment=cancelled")
                .putMetadata("product_id", request.productId())
                .putMetadata("quantity", Long.toString(request.quantity()))
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(request.quantity())
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency)
                                .setUnitAmount(toMinorUnits(product.price()))
                                .setProductData(productData.build())
                                .build())
                        .build());

        if (userId != null && !userId.isBlank()) {
            params.putMetadata("buyer_id", userId);
        }

        Session session = sessionCreator.create(params.build());
        return new CheckoutSessionResponse(session.getId(), session.getUrl());
    }

    @FunctionalInterface
    interface CheckoutSessionCreator {
        Session create(SessionCreateParams params) throws StripeException;
    }

    @FunctionalInterface
    interface ProductLookup {
        ProductSnapshot get(String productId, long quantity);
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:4200";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
