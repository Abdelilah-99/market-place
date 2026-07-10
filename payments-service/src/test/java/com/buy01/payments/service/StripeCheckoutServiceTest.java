package com.buy01.payments.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.buy01.payments.dto.CheckoutSessionResponse;
import com.buy01.payments.dto.CreateCheckoutSessionRequest;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

class StripeCheckoutServiceTest {

    @Test
    void rejectsCheckoutWhenStripeKeyIsMissing() {
        StripeCheckoutService service = new StripeCheckoutService("  ", null);
        CreateCheckoutSessionRequest request = request(null, null);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.createCheckoutSession(request, null));

        assertEquals("Stripe secret key is not configured", error.getMessage());
    }

    @Test
    void createsCheckoutWithNormalizedDefaults() throws Exception {
        Session session = new Session();
        session.setId("cs_test");
        session.setUrl("https://checkout.stripe.test/session");
        AtomicReference<SessionCreateParams> captured = new AtomicReference<>();
        StripeCheckoutService service = new StripeCheckoutService(params -> {
            captured.set(params);
            return session;
        }, null);

        CheckoutSessionResponse response = service.createCheckoutSession(request(null, null), null);

        assertEquals("cs_test", response.sessionId());
        assertEquals("https://checkout.stripe.test/session", response.checkoutUrl());
        SessionCreateParams params = captured.get();
        assertEquals("http://localhost:4200/products/product-1?payment=success", params.getSuccessUrl());
        assertEquals("usd", params.getLineItems().getFirst().getPriceData().getCurrency());
        assertEquals(1051L, params.getLineItems().getFirst().getPriceData().getUnitAmount());
    }

    @Test
    void includesBuyerImageAndNormalizedCurrency() throws Exception {
        Session session = new Session();
        AtomicReference<SessionCreateParams> captured = new AtomicReference<>();
        StripeCheckoutService service = new StripeCheckoutService(params -> {
            captured.set(params);
            return session;
        }, "https://buy01.test/");

        service.createCheckoutSession(request(" MAD ", "https://images.test/product.jpg"), " user-7 ");

        SessionCreateParams params = captured.get();
        assertEquals("mad", params.getLineItems().getFirst().getPriceData().getCurrency());
        assertEquals(" user-7 ", params.getMetadata().get("buyer_id"));
        assertEquals(
                "https://images.test/product.jpg",
                params.getLineItems().getFirst().getPriceData().getProductData().getImages().getFirst());
        assertEquals("https://buy01.test/products/product-1?payment=cancelled", params.getCancelUrl());
    }

    private CreateCheckoutSessionRequest request(String currency, String imageUrl) {
        return new CreateCheckoutSessionRequest(
                "product-1", "Test product", new BigDecimal("10.505"), currency, imageUrl);
    }
}
