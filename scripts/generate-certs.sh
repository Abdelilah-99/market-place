#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

set -a
. ./.env.scripts
set +a

: "${PSSWD:?Missing PSSWD in scripts/.env.scripts}"

mkdir -p certs

# 1. Create a private CA
openssl genrsa -out certs/ca.key 4096
openssl req -new -x509 -days 3650 -key certs/ca.key \
  -out certs/ca.crt \
  -subj "/CN=internal-ca/O=MyApp"

# 2. Generate cert for each service
for SERVICE in gateway users-service media-service products-service eureka-server; do
  openssl genrsa -out certs/$SERVICE.key 2048
  cat > certs/$SERVICE.ext <<EOF
 [v3_req]
 subjectAltName=@alt_names
 extendedKeyUsage=serverAuth,clientAuth

 [alt_names]
 DNS.1=$SERVICE
 DNS.2=localhost
EOF
  openssl req -new -key certs/$SERVICE.key \
    -out certs/$SERVICE.csr \
    -subj "/CN=$SERVICE/O=MyApp"
  openssl x509 -req -days 365 \
    -in certs/$SERVICE.csr \
    -CA certs/ca.crt -CAkey certs/ca.key -CAcreateserial \
    -out certs/$SERVICE.crt \
    -extfile certs/$SERVICE.ext \
    -extensions v3_req
  
  # Package as PKCS12 for Spring Boot
  openssl pkcs12 -export \
    -in certs/$SERVICE.crt \
    -inkey certs/$SERVICE.key \
    -out certs/$SERVICE.p12 \
    -name $SERVICE \
    -passout pass:"$PSSWD"
done

# Create a truststore with the CA certificate for all services
keytool -import -alias ca-cert -file certs/ca.crt \
  -keystore certs/truststore.p12 -storetype PKCS12 \
  -storepass "$PSSWD" -noprompt

echo "Certificates generated successfully in certs/ directory"
