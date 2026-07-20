# Payment and Order Flow

This document traces one purchase from the Buy button to a terminal order state. It identifies every code component involved, explains why it exists, and describes how the system prevents price tampering, overselling, duplicate processing, and lost stock.

## System boundary

The payment flow crosses five systems:

```text
Angular browser
    |
    | JWT + HTTPS
    v
Spring Cloud Gateway
    |
    | X-User-Id + service mTLS
    v
payments-service ---- HTTPS/mTLS ----> products-service
    |                                      |
    | MongoDB                              | MongoDB atomic updates
    |
    +--------------> Stripe Checkout
    |
    +--------------> Kafka item-sold-events
                                           |
                                           v
                                  products-service consumer
```

No database transaction can span all of these systems. The implementation uses a reservation identifier, atomic single-document MongoDB updates, conditional order state changes, Stripe webhook retries, synchronous Kafka acknowledgement, and compensating releases instead.

## The identifiers that connect the flow

Before contacting Stripe, payments-service generates a UUID. The same value is used as both the payment order ID and inventory reservation ID:

```text
PaymentOrder.id == StockReservation.reservationId
```

After Stripe creates a Checkout Session, its session ID is attached to the order and is also used as the sale event ID:

```text
PaymentOrder.stripeSessionId == ItemSoldEvent.stripeSessionId
ItemSoldEvent.eventId == Stripe Session ID
```

These stable identifiers make retries recognizable across MongoDB, Stripe, and Kafka.

## 1. Buyer starts checkout in Angular

### Product UI components

Files:

- `frontend/src/app/pages/product-detail/product-detail.ts`
- `frontend/src/app/sub-components/product/product.ts`

Both components perform two UX checks before checkout:

1. a buyer must be logged in;
2. the current user must not be the product owner.

They then call `PaymentsService.createCheckoutSession(product)`. On success, the browser navigates to Stripe's hosted URL. On failure, the API error message is shown through the toaster.

These checks improve UX, but client-side checks are not security boundaries. A modified client can bypass them. The backend remains responsible for price and stock integrity. At present, the seller-versus-buyer rule is enforced only in the frontend; the payment backend does not receive product owner data in `ProductSnapshot` and therefore does not independently reject self-purchases.

### Angular payments client

File: `frontend/src/app/core/services/payments-service.ts`

The request is deliberately minimal:

```http
POST /api/payments/checkout-sessions
Content-Type: application/json

{
  "productId": "product-uuid",
  "quantity": 1
}
```

The browser does not send an authoritative name, currency, or price. This solves a common payment vulnerability where a buyer edits the request and pays a lower amount.

`CheckoutSessionResponse` contains:

```json
{
  "sessionId": "cs_...",
  "checkoutUrl": "https://checkout.stripe.com/..."
}
```

The browser redirects with `window.location.href`. Card data is entered on Stripe's hosted page and never passes through this application.

## 2. Gateway authenticates and routes the request

### JWT filter

File: `gateway/gateway/src/main/java/com/example/gateway/filter/JwtAuthenticationFilter.java`

For a normal checkout request, the gateway verifies the JWT and derives trusted identity headers:

```text
X-User-Id   = JWT subject/user identifier
X-User-Role = JWT role claim
```

This prevents the browser from choosing the buyer ID stored on the order. The payments controller reads the gateway-generated `X-User-Id` rather than accepting `buyerId` in the JSON body.

The Stripe webhook path `/api/payments/webhooks/stripe` is public at the JWT layer because Stripe cannot possess a marketplace JWT. Its authenticity is checked later with Stripe's signature.

### Payments route

File: `gateway/gateway/src/main/java/com/example/gateway/config/GetwayConfig.java`

The `/api/payments/**` route resolves `lb://payments` through Eureka and applies the Redis request-rate limiter. The gateway communicates with the HTTPS payments endpoint using the platform's service TLS configuration.

Rate limiting protects the service and reduces abusive Checkout Session creation, but it does not replace authentication, Stripe signature verification, or inventory constraints.

## 3. Payments API validates the checkout contract

### Request and response DTOs

Files:

- `payments-service/src/main/java/com/buy01/payments/dto/CreateCheckoutSessionRequest.java`
- `payments-service/src/main/java/com/buy01/payments/dto/CheckoutSessionResponse.java`

`@NotBlank productId` rejects an absent product identifier and `@Positive quantity` rejects zero or negative quantities before orchestration begins.

### PaymentController

File: `payments-service/src/main/java/com/buy01/payments/controller/PaymentController.java`

`POST /api/payments/checkout-sessions` passes the validated request and `X-User-Id` to `StripeCheckoutService`.

It maps important infrastructure failures:

- missing Stripe configuration becomes `503 Service Unavailable`;
- a Stripe API failure becomes `502 Bad Gateway`;
- inventory and product errors propagated as `ResponseStatusException` retain their meaningful status.

`PaymentExceptionHandler` returns a consistent JSON error containing timestamp, HTTP status, and message so the frontend can show useful feedback.

## 4. Checkout orchestration creates a reservation first

### StripeCheckoutService

File: `payments-service/src/main/java/com/buy01/payments/service/StripeCheckoutService.java`

This is the main orchestrator. Its production path runs in this order:

```text
Generate order ID
  -> reserve stock
  -> persist PENDING order
  -> create Stripe Session
  -> attach Stripe Session ID
  -> return checkout URL
```

The order ID is generated before the inventory call so the stock reservation exists under the same stable identifier that will later appear in the order, Stripe metadata, and Kafka event.

Reserving before sending the buyer to Stripe solves overselling. If stock were reduced only after payment, two buyers could both pay for the last unit.

### ProductClient

File: `payments-service/src/main/java/com/buy01/payments/service/ProductClient.java`

The reservation call is internal service-to-service traffic:

```http
POST https://products-service:8000/api/internal/inventory/reservations

{
  "reservationId": "order-uuid",
  "productId": "product-uuid",
  "quantity": 1
}
```

The returned `ProductSnapshot` supplies the authoritative product ID, name, price, available quantity, and optional image URL. Payments-service uses that snapshot for both its order record and Stripe's line item.

The client maps a products-service `409 Conflict` to “Insufficient product quantity” and other connectivity problems to `502 Bad Gateway`.

### MutualTlsConfig

File: `payments-service/src/main/java/com/buy01/payments/config/MutualTlsConfig.java`

This configuration builds the `RestClient` with:

- the payments-service PKCS12 identity as its client certificate;
- the platform truststore for server verification;
- the application `ObservationRegistry` for HTTP client metrics.

It solves confidentiality and service identity for the direct payments-to-products call. The call does not traverse the public browser route.

## 5. Products-service reserves stock atomically

### InternalInventoryController

File: `products-service/products/src/main/java/com/example/products/controllers/InternalInventoryController.java`

The controller exposes three commands:

```text
POST /api/internal/inventory/reservations          reserve
POST /api/internal/inventory/reservations/confirm  confirm a paid sale
POST /api/internal/inventory/reservations/release  restore unpaid stock
```

`StockReservationRequest` validates the reservation ID, product ID, and positive quantity.

### Product and StockReservation

Files:

- `products-service/products/src/main/java/com/example/products/models/Product.java`
- `products-service/products/src/main/java/com/example/products/models/StockReservation.java`

Each product document stores:

```text
quantity                 units currently available to new buyers
stockReservations[]      units removed from availability but not terminal yet
processedSaleEventIds[]  legacy direct-decrement event idempotency records
```

Reservation internals are `@JsonIgnore`, so public product responses do not expose payment coordination data.

### ProductService.reserveStock

File: `products-service/products/src/main/java/com/example/products/services/ProductService.java`

MongoDB performs one conditional `findAndModify`:

```java
Criteria.where("_id").is(id)
    .and("quantity").gte(quantity)
    .and("stockReservations.reservationId").ne(reservationId)
```

with one update:

```java
new Update()
    .inc("quantity", -quantity)
    .push("stockReservations", new StockReservation(reservationId, quantity))
```

This single-document operation guarantees that:

- quantity cannot fall below zero;
- availability cannot be reduced without recording the reservation;
- the same reservation ID cannot reserve twice;
- two buyers racing for the last unit cannot both succeed.

If the predicate does not match, checkout receives `409 Conflict`.

## 6. Payments-service persists a PENDING order

### PaymentOrder

File: `payments-service/src/main/java/com/buy01/payments/model/PaymentOrder.java`

The `payment_orders` MongoDB document records:

```text
id                 order ID and reservation ID
stripeSessionId    unique sparse index, attached after Stripe creation
productId/name     immutable product snapshot
quantity
unitPrice/currency authoritative charged values
buyerId            derived from the JWT by the gateway
status             PENDING, PAID, or CANCELLED
createdAt/updatedAt
```

Keeping the product name and price snapshot preserves what the buyer actually agreed to even if the seller edits the product later.

### OrderService and repository

Files:

- `payments-service/src/main/java/com/buy01/payments/service/OrderService.java`
- `payments-service/src/main/java/com/buy01/payments/repository/PaymentOrderRepository.java`

`OrderService.create` stores the order as `PENDING`. `attachStripeSession` then connects it to Stripe's session ID.

Order state changes use MongoDB `findAndModify` predicates that include the current state:

```text
PENDING -> PAID
PENDING -> CANCELLED
```

A duplicate webhook cannot move an already-terminal order again. The unique sparse Stripe session index also prevents two orders from being attached to the same session.

`GET /api/payments/orders/me` uses the gateway-derived `X-User-Id` and `findAllByBuyerIdOrderByCreatedAtDesc` so a buyer retrieves only their own orders, newest first.

## 7. Stripe Checkout Session is created

`StripeCheckoutService` constructs a one-time payment session with:

- currency `usd`;
- quantity from the validated request;
- name, image, and price from products-service;
- price converted to cents with `HALF_UP` rounding;
- a 30-minute expiration;
- product, quantity, buyer, and order metadata;
- success and cancel return URLs for the Angular product page.

Example metadata:

```text
product_id = product-uuid
quantity   = 1
buyer_id   = authenticated-user-id
order_id   = order/reservation-uuid
```

The secret Stripe API key is used only inside payments-service. The browser receives the hosted checkout URL, not the key.

The success URL `?payment=success` does not mark an order paid. A user can visit or forge a return URL. Only a verified webhook causes a state transition.

Likewise, returning through `?payment=cancelled` is a UI navigation event. It does not immediately release inventory. Stripe's authoritative expiration event releases it after the session expires.

## 8. Compensation if checkout creation fails

There are several failure points after inventory has been reserved. `StripeCheckoutService` compensates for them:

- If order persistence fails, it releases the reservation.
- If Stripe Session creation fails, it releases the reservation and conditionally cancels the order.
- If attaching the Stripe session fails, it performs the same release-and-cancel compensation.

Release uses the exact reservation ID, product ID, and quantity. This is essential: a broad `quantity += requested` retry could restore the same stock multiple times.

## 9. Stripe webhook is the payment authority

### StripeWebhookController

File: `payments-service/src/main/java/com/buy01/payments/controller/StripeWebhookController.java`

Endpoint:

```text
POST /api/payments/webhooks/stripe
```

The controller reads the raw request body and `Stripe-Signature`, then calls `Webhook.constructEvent(payload, signature, webhookSecret)`.

This cryptographically verifies that the event came from Stripe and that the body was not modified. Invalid signatures return `400`; an absent webhook secret returns `503`.

The controller handles:

- `checkout.session.completed` for successful payment;
- `checkout.session.expired` for an unpaid expired session.

Other Stripe event types are acknowledged without changing orders.

## 10A. Successful payment path

### Publish the sale event

`OrderService.completeStripeSession` loads the order by Stripe session ID and proceeds only while it is `PENDING`.

`SaleEventPublisher` publishes this record to `item-sold-events`, keyed by product ID:

```json
{
  "eventId": "cs_stripe_session_id",
  "reservationId": "order-uuid",
  "stripeSessionId": "cs_stripe_session_id",
  "productId": "product-uuid",
  "quantity": 1,
  "buyerId": "user-id",
  "soldAt": "timestamp"
}
```

File: `payments-service/src/main/java/com/buy01/payments/service/SaleEventPublisher.java`

`KafkaTemplate.send(...).join()` waits for broker acknowledgement. The order is changed to `PAID` only after Kafka accepts the sale event. If publication fails, webhook processing fails before the state transition, leaving the order `PENDING` so Stripe can retry the webhook.

### Confirm the reservation

Files:

- `products-service/products/src/main/java/com/example/products/kafka/ItemSoldEventListener.java`
- `products-service/products/src/main/java/com/example/products/config/KafkaConfig.java`

The `products-inventory` Kafka consumer deserializes the event and calls `confirmReservation`.

Confirmation atomically removes the exact reservation entry. It does not decrement quantity again because the unit was already removed from available quantity at reservation time. Reprocessing the event finds no reservation and therefore cannot consume stock twice.

### Mark the order paid

After Kafka acknowledgement, payments-service conditionally changes the matching order from `PENDING` to `PAID`.

The effective sequence is therefore:

```text
Stripe signature verified
  -> Kafka sale event acknowledged
  -> PaymentOrder becomes PAID
  -> products consumer removes reservation marker
```

Kafka consumption is asynchronous. The order may become `PAID` shortly before products-service removes the reservation marker, but the product quantity is already correct.

## 10B. Expired or unpaid path

For `checkout.session.expired`, `OrderService.expireStripeSession`:

1. loads the order by Stripe session ID;
2. verifies it is still `PENDING`;
3. calls the products-service release endpoint;
4. conditionally changes the order to `CANCELLED`.

`ProductService.releaseReservation` atomically matches the exact reservation and quantity, increments available quantity, and removes the reservation in the same update.

Once removed, a repeated expiration webhook cannot match the reservation and cannot increment stock again. `ProductClient` treats a `404` release response as an already released or confirmed no-op.

## Failure and retry guarantees

### What is strongly atomic

The following operations are atomic because each modifies one MongoDB document:

- decrease quantity and add reservation;
- increase quantity and remove reservation;
- remove a confirmed reservation;
- transition an order only from its expected current state.

### What is eventually consistent

There is no transaction across products MongoDB, payments MongoDB, Stripe, and Kafka. Short-lived intermediate states are possible:

- stock reserved before the order is saved;
- a PENDING order before the Stripe session ID is attached;
- a PAID order before Kafka consumption confirms the reservation.

Compensation and retries handle expected failures, but this is not a full transactional outbox implementation. For example, Kafka can acknowledge an event and the later PAID database update can fail. A Stripe retry may publish the same session again; reservation confirmation remains safe because it only removes an existing matching reservation.

## Security responsibilities

| Boundary | Mechanism | Problem solved |
| --- | --- | --- |
| Browser to gateway | JWT | Authenticates the buyer. |
| Gateway identity propagation | `X-User-Id` from verified JWT | Stops the request body choosing another buyer ID. |
| Gateway rate limiter | Redis token bucket | Limits checkout/API abuse. |
| Gateway/service and service/service | TLS and mTLS | Encrypts traffic and authenticates service certificates. |
| Browser request contract | Product ID and quantity only | Prevents browser-supplied authoritative prices. |
| Stripe API | Server-side secret key | Keeps Stripe credentials out of the browser. |
| Stripe webhook | `Stripe-Signature` plus webhook secret | Makes Stripe, rather than a redirect URL, payment authority. |
| MongoDB reservation predicate | Quantity and unique reservation ID | Prevents overselling and duplicate reservation. |
| Conditional order transition | Current status must be PENDING | Makes webhook state changes idempotent. |

## Runtime configuration

Files:

- `payments-service/src/main/resources/application.properties`
- `payments-service/docker-compose.yaml`

Required production values include:

```text
STRIPE_SECRET_KEY
STRIPE_WEBHOOK_SECRET
APP_PUBLIC_URL
SPRING_DATA_MONGODB_URI
SPRING_KAFKA_BOOTSTRAP_SERVERS
DISCOVERY
CERTIFICATE_PATH
TRUSTSTORE_PATH
KEY_STORE_PWD
TRUST_STORE_PWD
```

`payments-service-mongodb` owns the `payment_orders` collection in the persistent `payments_mongo_data` volume. The payments container retains the public Java CA bundle for outbound Stripe HTTPS while its application keystore and truststore handle inbound and internal mTLS.

## Observability and retention

Critical steps have low-cardinality Micrometer observations:

```text
marketplace.payment.checkout
marketplace.payment.stock.reserve
marketplace.payment.order.create
marketplace.payment.webhook.completed
marketplace.payment.sale.publish
marketplace.inventory.stock.confirm
marketplace.payment.webhook.expired
marketplace.payment.stock.release
marketplace.inventory.stock.release
```

Order, buyer, product, reservation, and Stripe session IDs are intentionally not metric labels because unbounded labels would overload Prometheus.

`PaymentOrderRetentionJob` keeps terminal history bounded:

- `CANCELLED` orders: 30 days by default;
- `PAID` orders: 365 days by default;
- `PENDING` orders: never deleted by this retention job.

## Complete outcome matrix

| Situation | Inventory result | Order result | Buyer result |
| --- | --- | --- | --- |
| Invalid input | Unchanged | Not created | 400 response |
| Product missing | Unchanged | Not created | 404 response |
| Insufficient stock | Unchanged | Not created | 409 response |
| Order save fails after reserve | Reservation released | Not created | Checkout error |
| Stripe creation/attachment fails | Reservation released | CANCELLED when present | Checkout error |
| Stripe payment succeeds | Reservation confirmed/removed | PAID | Redirected to success page |
| Session expires unpaid | Quantity restored | CANCELLED | Session no longer payable |
| Duplicate completion webhook | No second decrement | Remains PAID | No duplicate order transition |
| Duplicate expiration/release | No second increment | Remains CANCELLED | No stock inflation |

## Code ownership index

| Component | Responsibility | What it solves |
| --- | --- | --- |
| Angular product components | Login/owner UX checks and error display | Prevents obvious invalid UI actions. |
| `PaymentsService` | Minimal checkout HTTP contract and Stripe redirect | Keeps price/card handling out of the browser. |
| `JwtAuthenticationFilter` | JWT validation and trusted identity headers | Associates orders with the authenticated buyer. |
| `GetwayConfig` | Discovery routing and rate limiting | Provides one controlled edge route. |
| `PaymentController` | Validated checkout endpoint and error mapping | Separates HTTP concerns from orchestration. |
| `StripeCheckoutService` | Reservation/order/Stripe orchestration and compensation | Coordinates a distributed workflow safely. |
| `ProductClient` | mTLS inventory commands | Encapsulates internal products-service calls and errors. |
| `MutualTlsConfig` | Client identity, trust, and observations | Secures and measures internal HTTPS. |
| `InternalInventoryController` | Reserve/confirm/release API | Exposes explicit inventory state transitions. |
| `ProductService` | Atomic MongoDB inventory mutations | Prevents overselling and duplicate stock changes. |
| `StockReservation` | Pending inventory ownership marker | Connects unavailable stock to an order. |
| `PaymentOrder` | Durable payment/order snapshot | Records buyer, price, status, and Stripe correlation. |
| `OrderService` | Conditional order transitions | Makes repeated webhook processing safe. |
| `StripeWebhookController` | Signature verification and event dispatch | Makes Stripe the payment authority. |
| `SaleEventPublisher` | Synchronously acknowledged Kafka publication | Prevents PAID before the sale event reaches Kafka. |
| `ItemSoldEventListener` | Reservation confirmation | Finalizes sold inventory without decrementing twice. |
| `PaymentOrderRetentionJob` | Deletes old terminal orders | Bounds long-term database growth. |
