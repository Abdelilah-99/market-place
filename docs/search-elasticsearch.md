# Product Search with OpenSearch

This project uses OpenSearch, the Elasticsearch-compatible search engine, to make product discovery a real full-text search feature instead of a simple MongoDB list query.

## Why OpenSearch Was Added

MongoDB remains the source of truth for products, but it is not ideal for marketplace search features such as:

- Full-text matching across product names and descriptions
- Fuzzy matching for small typing mistakes
- Fast category and price filtering
- Sorting by newest or price
- Independent search indexing that can be rebuilt from the main database

OpenSearch is used as a read-optimized search index. Product writes still happen in `products-service` and MongoDB first.

## Architecture Flow

```text
Frontend
  |
  | GET /api/products/search?q=phone
  v
Spring Cloud Gateway
  |
  v
products-service
  |
  | HTTP search query
  v
OpenSearch
```

Product indexing is asynchronous:

```text
products-service writes product to MongoDB
  |
  | publishes product-search-sync-events
  v
Kafka
  |
  | consumed by products-service indexer
  v
OpenSearch products-search index
```

This keeps MongoDB as the source of truth while making search fast and interview-friendly.

## Docker Service

OpenSearch is defined in `opensearch/docker-compose.yaml`:

```yaml
services:
  opensearch:
    image: opensearchproject/opensearch:2.15.0
    container_name: opensearch
    environment:
      discovery.type: single-node
      plugins.security.disabled: "true"
      OPENSEARCH_JAVA_OPTS: "-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    networks:
      - shared-net
```

The service uses the existing external Docker network `shared-net`, so `products-service` can reach it with the Docker DNS name `opensearch`.

The products-service environment values live in `products-service/.env.product`:

```env
OPENSEARCH_HOST=opensearch
OPENSEARCH_PORT=9200
OPENSEARCH_SCHEME=http
PRODUCT_SEARCH_INDEX=products-search
```

## Product Search Index

The searchable product document is represented by:

`products-service/products/src/main/java/com/example/products/search/ProductSearchDocument.java`

Indexed fields:

- `id`
- `name`
- `description`
- `category`
- `price`
- `images`
- `userId`
- `createdAt`

The index is created automatically when `products-service` starts if it does not already exist. The mapping configures:

- `name` and `description` as full-text fields
- `category`, `images`, and `userId` as keyword fields
- `price` as a double
- `createdAt` as a date

## Kafka Synchronization

Product writes publish search sync events to Kafka topic:

```text
product-search-sync-events
```

The event is:

`products-service/products/src/main/java/com/example/products/search/events/ProductSearchSyncEvent.java`

Supported actions:

- `UPSERT` for product create/update
- `DELETE` for product delete

The consumer is:

`products-service/products/src/main/java/com/example/products/search/ProductSearchEventListener.java`

When it receives an event:

- `UPSERT` indexes or replaces the product document in OpenSearch
- `DELETE` removes the product document from OpenSearch

## Search API

Public endpoint:

```http
GET /api/products/search
```

Query parameters:

| Parameter | Example | Description |
| --- | --- | --- |
| `q` | `iphone` | Full-text search query |
| `category` | `phones` | Exact category filter |
| `minPrice` | `100` | Minimum price |
| `maxPrice` | `900` | Maximum price |
| `page` | `0` | Zero-based page number |
| `size` | `12` | Page size, capped at 50 |
| `sort` | `newest` | `newest`, `price_asc`, `price_desc`, or `relevance` |

Search uses OpenSearch `multi_match` over:

- `name` boosted higher
- `description`
- `category`

Fuzzy matching is enabled with `AUTO`, so small typos can still match relevant products.

## Reindexing

Admin-only endpoint:

```http
POST /api/admin/products/reindex-search
```

This reads all products from MongoDB and writes them into OpenSearch again. Use it after deploying search for the first time, after index deletion, or after mapping changes.

The endpoint is protected in `products-service` security config:

```java
.requestMatchers(HttpMethod.POST, "/api/admin/products/reindex-search")
.hasRole("ADMIN")
```

The gateway exposes `/api/admin/products/**` to `products-service`.

## Example Commands

Start OpenSearch:

```bash
docker compose -f opensearch/docker-compose.yaml up -d
```

Check OpenSearch health:

```bash
curl http://localhost:9200/_cluster/health
```

Search products:

```bash
curl "https://localhost:10000/api/products/search?q=phone&page=0&size=12&sort=relevance" -k
```

Search by category and price range:

```bash
curl "https://localhost:10000/api/products/search?category=electronics&minPrice=100&maxPrice=900&sort=price_asc" -k
```

Reindex all products, using an admin JWT:

```bash
curl -X POST "https://localhost:10000/api/admin/products/reindex-search" \
  -H "Authorization: Bearer <ADMIN_JWT>" \
  -k
```

Verify indexed products directly in OpenSearch:

```bash
curl "http://localhost:9200/products-search/_search?pretty"
```

Count indexed products:

```bash
curl "http://localhost:9200/products-search/_count?pretty"
```

Inspect the index mapping:

```bash
curl "http://localhost:9200/products-search/_mapping?pretty"
```

## Operational Notes

- MongoDB is still the source of truth.
- OpenSearch is a derived read model.
- Kafka keeps the search index synchronized after create/update/delete.
- The admin reindex endpoint repairs or rebuilds the index from MongoDB.
- OpenSearch security is disabled in Docker for local/internal deployment simplicity. In production, expose it only on a private network or enable security/authentication.
