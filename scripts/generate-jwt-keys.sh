#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="${1:-/tmp/marketo-jwt-keys}"

mkdir -p "${OUT_DIR}"
chmod 700 "${OUT_DIR}"

openssl genpkey \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:2048 \
  -out "${OUT_DIR}/private.pem"

openssl rsa \
  -pubout \
  -in "${OUT_DIR}/private.pem" \
  -out "${OUT_DIR}/public.pem"

chmod 600 "${OUT_DIR}/private.pem" "${OUT_DIR}/public.pem"

echo "JWT RSA key pair generated:"
echo "  private: ${OUT_DIR}/private.pem"
echo "  public:  ${OUT_DIR}/public.pem"
echo
echo "Import these files into Jenkins as File credentials:"
echo "  jwt-private-key -> ${OUT_DIR}/private.pem"
echo "  jwt-public-key  -> ${OUT_DIR}/public.pem"
