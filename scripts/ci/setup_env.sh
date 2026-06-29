#!/usr/bin/env bash
set -euo pipefail

mkdir -p certs gateway/certs products-service/certs media-service/certs users-service/certs opensearch

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

declare -A certs=(
  ["$TRUSTSTORE"]="certs/truststore.p12"
  ["$GATE_CERT"]="gateway/certs/gateway.p12"
  ["$PROD_CERT"]="products-service/certs/products-service.p12"
  ["$MEDIA_CERT"]="media-service/certs/media-service.p12"
  ["$USR_CERT"]="users-service/certs/users-service.p12"
)

for src in "${!certs[@]}"; do
  dest="${certs[$src]}"
  cp "$src" "$dest"
  chmod 600 "$dest"
done

for service_cert_dir in gateway/certs products-service/certs media-service/certs users-service/certs; do
  cp "$TRUSTSTORE" "${service_cert_dir}/truststore.p12"
  chmod 600 "${service_cert_dir}/truststore.p12"
done
