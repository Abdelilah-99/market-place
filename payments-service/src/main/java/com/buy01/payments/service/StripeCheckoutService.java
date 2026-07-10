package com.buy01.payments.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.buy01.payments.dto.CheckoutSessionResponse;
import com.buy01.payments.dto.CreateCheckoutSessionRequest;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class StripeCheckoutService {
    private final CheckoutSessionCreator sessionCreator;
    private final String appPublicUrl;

    public StripeCheckoutService(
            @Value("${stripe.secret-key:}") String stripeSecretKey,
            @Value("${app.public-url:http://localhost:4200}") String appPublicUrl) {
        String normalizedKey = stripeSecretKey == null ? "" : stripeSecretKey.trim();
        this.sessionCreator = normalizedKey.isBlank()
                ? null
                : new StripeClient(normalizedKey).checkout().sessions()::create;
        this.appPublicUrl = stripTrailingSlash(appPublicUrl);
    }

    StripeCheckoutService(CheckoutSessionCreator sessionCreator, String appPublicUrl) {
        this.sessionCreator = sessionCreator;
        this.appPublicUrl = stripTrailingSlash(appPublicUrl);
    }

    public CheckoutSessionResponse createCheckoutSession(
            CreateCheckoutSessionRequest request,
            String userId) throws StripeException {
        if (sessionCreator == null) {
            throw new IllegalStateException("Stripe secret key is not configured");
        }
        String currency = normalizeCurrency(request.currency());

        SessionCreateParams.LineItem.PriceData.ProductData.Builder productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(request.productName())
                        .putMetadata("product_id", request.productId());

        if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
            productData.addImage(request.imageUrl());
        }

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(appPublicUrl + "/products/" + request.productId() + "?payment=success")
                .setCancelUrl(appPublicUrl + "/products/" + request.productId() + "?payment=cancelled")
                .putMetadata("product_id", request.productId())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency)
                                .setUnitAmount(toMinorUnits(request.amount()))
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

    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "usd";
        }
        return currency.trim().toLowerCase(Locale.ROOT);
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:4200";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
