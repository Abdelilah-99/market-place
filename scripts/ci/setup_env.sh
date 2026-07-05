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

copy_secret_file() {
  local src="$1"
  local dest="$2"

  mkdir -p "$(dirname "$dest")"
  cp "$src" "$dest"
  chmod 600 "$dest"
}

copy_secret_file "$TRUSTSTORE" "certs/truststore.p12"
copy_secret_file "$CA_CERT" "certs/ca.crt"
copy_secret_file "$GATE_CERT" "certs/gateway.p12"
copy_secret_file "$PROD_CERT" "certs/products-service.p12"
copy_secret_file "$MEDIA_CERT" "certs/media-service.p12"
copy_secret_file "$USR_CERT" "certs/users-service.p12"
copy_secret_file "$EUREKA_CERT" "certs/eureka-server.p12"
copy_secret_file "$PROMETHEUS_CERT" "certs/prometheus.crt"
copy_secret_file "$PROMETHEUS_KEY" "certs/prometheus.key"
copy_secret_file "$GATE_CERT" "gateway/certs/gateway.p12"
copy_secret_file "$PROD_CERT" "products-service/certs/products-service.p12"
copy_secret_file "$MEDIA_CERT" "media-service/certs/media-service.p12"
copy_secret_file "$USR_CERT" "users-service/certs/users-service.p12"

for service_cert_dir in gateway/certs products-service/certs media-service/certs users-service/certs; do
  cp "$TRUSTSTORE" "${service_cert_dir}/truststore.p12"
  chmod 600 "${service_cert_dir}/truststore.p12"
done
