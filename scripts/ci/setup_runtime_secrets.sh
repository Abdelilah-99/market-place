#!/usr/bin/env bash
set -euo pipefail

mkdir -p gateway/certs products-service/certs media-service/certs users-service/certs

cp "$JWT_PRIVATE_KEY" users-service/certs/jwt-private.pem
cp "$JWT_PUBLIC_KEY" gateway/certs/jwt-public.pem

chmod 600 \
  users-service/certs/jwt-private.pem \
  gateway/certs/jwt-public.pem

for service_cert_dir in gateway/certs products-service/certs media-service/certs users-service/certs; do
  cp "$TRUSTSTORE" "${service_cert_dir}/truststore.p12"
  chmod 600 "${service_cert_dir}/truststore.p12"
done
