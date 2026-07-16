# Payments, Orders, and Atomic Stock Reservations

The payments service creates Stripe Checkout sessions without trusting product names or prices supplied by the browser. It coordinates with the products service over HTTPS/mTLS, persists an order, reserves inventory before payment, and uses Stripe webhooks plus Kafka to complete or release the reservation.

## Why Stock Must Be Reserved

A simple checkout flow might read a product, verify `quantity > 0`, and reduce the quantity after payment. That allows overselling:

1. A product has one unit.
2. Buyer A reads `quantity = 1`.
3. Buyer B reads `quantity = 1` before Buyer A pays.
4. Both buyers create a checkout and pay for the same unit.

The implementation closes this race by reducing the available quantity when checkout starts. MongoDB performs the availability check, quantity reduction, and reservation insertion as one atomic update on the product document.

## Lifecycle

```text
POST /api/payments/checkout-sessions
                 |
                 v
Generate order/reservation ID
                 |
                 v
Atomically reserve stock in products-service
                 |
                 v
Persist PENDING payment order
                 |
                 v
Create 30-minute Stripe Checkout Session
        +--------+---------+
        |                  |
        v                  v
checkout.session.completed checkout.session.expired
        |                  |
        v                  v
Publish Kafka sale event   Release reservation
        |                  |
        v                  v
Confirm reservation        Order -> CANCELLED
        |
        v
Order -> PAID
```

If order persistence or Stripe session creation fails, the payment service immediately releases the reservation and marks an existing order `CANCELLED`.

## Checkout Contract

The frontend sends only the buyer's choices:

```http
POST /api/payments/checkout-sessions
Content-Type: application/json

{
  "productId": "9f834ed8-c8a8-4e68-8647-a3e12bcb61f2",
  "quantity": 2
}
```

The payment service does not accept an authoritative product name or price from the browser. The products service returns those fields after successfully reserving stock, and the returned price is used for both the order and Stripe Checkout.

## Reservation Identity

The payment service generates a UUID order ID before contacting the products service. The same value is used as the stock reservation ID:

```text
PaymentOrder.id == StockReservation.reservationId
```

This identifier connects the payment order, product reservation, Stripe metadata, and Kafka sale event.

## mTLS Inventory Request

The payment service sends an internal request directly to the products service:

```http
POST https://products-service:8000/api/internal/inventory/reservations
Content-Type: application/json

{
  "reservationId": "order-uuid",
  "productId": "product-uuid",
  "quantity": 2
}
```

`MutualTlsConfig` loads:

- `payments-service.p12` as the client identity;
- `truststore.p12` to trust the internal CA that issued the products-service certificate.

The connection therefore provides encryption, server verification, and client certificate authentication. The internal CA is already imported by `scripts/generate-certs.sh`, and `scripts/setup-cert-volumes.sh` copies the truststore into the service certificate volumes.

## Atomic MongoDB Reservation

The products service builds a conditional query:

```java
Query query = Query.query(Criteria.where("_id").is(productId)
        .and("quantity").gte(requestedQuantity)
        .and("stockReservations.reservationId").ne(reservationId));
```

The product matches only when:

1. the product exists;
2. its available quantity is sufficient;
3. the reservation ID is not already present.

It then applies one update:

```java
Update update = new Update()
        .inc("quantity", -requestedQuantity)
        .push("stockReservations",
                new StockReservation(reservationId, requestedQuantity));

Product reserved = mongoTemplate.findAndModify(
        query,
        update,
        FindAndModifyOptions.options().returnNew(true),
        Product.class);
```

MongoDB guarantees atomicity for this update because the conditions and changes affect one product document. There is no intermediate state in which the quantity is reduced without a reservation or a reservation is recorded without reducing quantity.

### Concurrent Example

Assume one unit is available:

```json
{
  "quantity": 1,
  "stockReservations": []
}
```

Buyer A and Buyer B concurrently request one unit. If Buyer A's operation wins, MongoDB changes the document to:

```json
{
  "quantity": 0,
  "stockReservations": [
    {
      "reservationId": "order-A",
      "quantity": 1
    }
  ]
}
```

Buyer B's query then fails `quantity >= 1`, so its update is not applied and checkout returns `409 Conflict`. The quantity can never become `-1`.

## Order Record

After reserving stock, payments-service stores a `PaymentOrder` in its own MongoDB database:

```text
id
stripeSessionId
productId
productName
quantity
unitPrice
currency
buyerId
status
createdAt
updatedAt
```

Order states are:

```text
PENDING -> PAID
PENDING -> CANCELLED
```

State changes use conditional `findAndModify` queries. For example, payment completion only changes an order whose current state is `PENDING`. Repeated webhooks therefore cannot repeatedly complete the same order.

A buyer can retrieve their orders through:

```http
GET /api/payments/orders/me
X-User-Id: buyer-id
```

## Successful Payment

Stripe sends a signed `checkout.session.completed` webhook. The payment service:

1. verifies the `Stripe-Signature` using `STRIPE_WEBHOOK_SECRET`;
2. loads the `PENDING` order using the Stripe session ID;
3. publishes an `item-sold-events` Kafka event and waits for Kafka to acknowledge it;
4. conditionally changes the order to `PAID`.

The event contains the order/reservation ID:

```json
{
  "eventId": "cs_stripe_session_id",
  "reservationId": "order-uuid",
  "stripeSessionId": "cs_stripe_session_id",
  "productId": "product-uuid",
  "quantity": 2,
  "buyerId": "buyer-id",
  "soldAt": "2026-07-15T12:00:00Z"
}
```

The products service consumes this event and atomically removes the matching reservation. It does **not** decrement quantity again—the inventory was already removed from availability when checkout began.

Waiting for Kafka acknowledgement is important. If publication fails, the order remains `PENDING`, allowing Stripe's webhook retry to attempt publication again.

## Expiration and Failure Compensation

Stripe Checkout sessions expire after 30 minutes. On `checkout.session.expired`, payments-service releases the reservation and changes the order to `CANCELLED`.

Release requires the exact reservation ID and quantity:

```java
Query query = Query.query(Criteria.where("_id").is(productId)
        .and("stockReservations").elemMatch(
                Criteria.where("reservationId").is(reservationId)
                        .and("quantity").is(quantity)));

Update update = new Update()
        .inc("quantity", quantity)
        .pull("stockReservations", matchingReservation);
```

The increment and removal happen atomically. Once the reservation is removed, a repeated release request cannot match it and cannot increment quantity again.

The same compensation runs when:

- payment-order persistence fails after reservation;
- Stripe Checkout session creation fails;
- attaching the Stripe session to the order fails.

## Idempotency

Stripe webhooks and Kafka records can be delivered more than once. The flow remains safe because:

- only a `PENDING` order can transition to `PAID` or `CANCELLED`;
- reservation creation rejects an existing reservation ID;
- confirmation only removes a reservation that still exists;
- release only restores the quantity when the exact reservation still exists;
- confirming a sale never decrements quantity a second time.

## Atomicity Boundary

The strict atomic boundary is one MongoDB product document. MongoDB cannot create a distributed transaction spanning products-service MongoDB, payments-service MongoDB, Stripe, and Kafka.

Cross-system consistency is therefore implemented as a saga-like workflow:

- local atomic database operations;
- conditional state transitions;
- stable reservation identifiers;
- idempotent confirmation and release operations;
- compensating release actions when later steps fail;
- webhook retry when Kafka publication fails.

This prevents overselling while keeping each microservice responsible for its own database.

## Runtime Configuration

Relevant payment-service variables are:

```dotenv
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
SPRING_DATA_MONGODB_URI=mongodb://payments-service-mongodb:27017/payments
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
PRODUCTS_BASE_URL=https://products-service:8000
CERTIFICATE_PATH=file:/app/resources/certs/payments-service.p12
TRUSTSTORE_PATH=file:/app/resources/certs/truststore.p12
KEY_STORE_PWD=...
TRUST_STORE_PWD=...
```

The payments Compose file starts `payments-service-mongodb` and persists its data in the `payments_mongo_data` volume.

## Main Implementation Files

- `StripeCheckoutService`: orchestrates reservation, order creation, Stripe Checkout, and compensation.
- `ProductClient`: performs mTLS inventory reservation and release requests.
- `OrderService`: persists orders and handles conditional lifecycle transitions.
- `StripeWebhookController`: verifies Stripe events and dispatches completion/expiration.
- `SaleEventPublisher`: publishes reservation-confirmation events to Kafka.
- `InternalInventoryController`: exposes products-service reservation operations.
- `ProductService`: performs atomic MongoDB reserve, confirm, and release updates.
- `ItemSoldEventListener`: confirms reservations after successful payment.

