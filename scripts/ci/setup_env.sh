#!/usr/bin/env bash
set -euo pipefail

mkdir -p gateway/certs users-service/certs opensearch

cp "$USR_ENV"     ./users-service/.env.users
cp "$GATEWAY_ENV" ./gateway/.env.gateway
cp "$PRDCT_ENV"   ./products-service/.env.product
cp "$MDA_ENV"     ./media-service/.env.media
cp "$OPENSEARCH_ENV" ./opensearch/.env.opensearch
cp "$JWT_PRIVATE_KEY" ./users-service/certs/jwt-private.pem
cp "$JWT_PUBLIC_KEY" ./gateway/certs/jwt-public.pem

# ensure_env_value() {
#   local file="$1"
#   local key="$2"
#   local value="$3"

#   if ! grep -qE "^${key}=.+" "$file"; then
#     printf '%s=%s\n' "$key" "$value" >> "$file"
#   fi
# }

# ensure_env_value ./products-service/.env.product SPRING_KAFKA_BOOTSTRAP_SERVERS kafka:29092
# ensure_env_value ./products-service/.env.product OPENSEARCH_HOST opensearch
# ensure_env_value ./products-service/.env.product OPENSEARCH_PORT 9200
# ensure_env_value ./products-service/.env.product OPENSEARCH_SCHEME http
# ensure_env_value ./products-service/.env.product PRODUCT_SEARCH_INDEX products-search

chmod 600 \
  ./users-service/.env.users \
  ./gateway/.env.gateway \
  ./products-service/.env.product \
  ./media-service/.env.media \
  ./opensearch/.env.opensearch \
  ./users-service/certs/jwt-private.pem \
  ./gateway/certs/jwt-public.pem
