#!/usr/bin/env bash
set -euo pipefail

mkdir -p certs gateway/certs products-service/certs media-service/certs users-service/certs

cp "$USR_ENV"     ./users-service/.env.users
cp "$GATEWAY_ENV" ./gateway/.env.gateway
cp "$PRDCT_ENV"   ./products-service/.env.product
cp "$MDA_ENV"     ./media-service/.env.media

chmod 600 \
  ./users-service/.env.users \
  ./gateway/.env.gateway \
  ./products-service/.env.product \
  ./media-service/.env.media

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
