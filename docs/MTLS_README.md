# mTLS (mutual TLS) — Implementation Guide

This document explains how mutual TLS (mTLS) is implemented in this project: certificate creation, application configuration, deployment considerations, verification commands, and troubleshooting tips.

## Purpose
- mTLS ensures both client and server authenticate each other using X.509 certificates. In this repo the gateway and backend services authenticate mutually so internal traffic is encrypted and trusted.

## Key files
- Cert generation: `scripts/generate-certs.sh`
- Example service Spring config: `users-service/service/src/main/resources/application.properties`
- Troubleshooting summary: `docs/MTLS_DEBUG_SUMMARY.md`

## Certificate creation and trust model
- The project uses an internal CA created by `scripts/generate-certs.sh`.
- Steps performed by the script:
  1. Create an internal CA key and certificate (`certs/ca.key`, `certs/ca.crt`).
  2. For each service (gateway, users-service, media-service, products-service, eureka-server):
     - Generate a private key and CSR.
     - Sign the CSR with the internal CA to create a service certificate (`$SERVICE.crt`).
     - Produce a PKCS#12 keystore (`$SERVICE.p12`) containing the private key + cert (password from `scripts/.env.scripts`).
  3. Create a shared `truststore.p12` (PKCS12) that contains the CA certificate and is used by JVM truststores.
- Single CA: all service certs are signed by the same internal CA. Each JVM must trust that CA to validate peer certificates.

## Spring Boot configuration (server and mTLS)
- Important properties (see `users-service/.../application.properties`):
  - `server.ssl.enabled=true` — enable HTTPS.
  - `server.ssl.key-store` / `server.ssl.key-store-password` — service keystore (.p12) used to present the server cert.
  - `server.ssl.trust-store` / `server.ssl.trust-store-password` — truststore containing the CA(s) used to validate client certs.
  - `server.ssl.client-auth=need` — require the client (gateway) to present a valid certificate; handshake fails if missing/invalid.
- Behavior: the service presents its cert and validates the gateway's client cert against its truststore; the gateway validates the service cert against its own truststore.

## Gateway JVM and outbound TLS
- The gateway is both an HTTPS server and a TLS client (when calling backend services). It must have:
  - A keystore (`gateway.p12`) so backends can verify the gateway's presented client certificate.
  - A truststore (`truststore.p12`) containing the internal CA so the gateway can verify the backend certificates.
- Typical JVM flags (set in container or startup script):
```
-Djavax.net.ssl.keyStore=/path/to/gateway.p12
-Djavax.net.ssl.keyStorePassword=<pwd>
-Djavax.net.ssl.trustStore=/path/to/truststore.p12
-Djavax.net.ssl.trustStorePassword=<pwd>
```
- If the gateway JVM truststore is missing the internal CA, you'll see errors like `unable to get local issuer certificate` or `unknown CA`.

## Why PKCS#12
- PKCS#12 (.p12) is a single-file keystore containing private key and certificate and is supported natively by the JVM and Spring Boot. The script creates `.p12` files to simplify container deployment.

## Common deployment pitfalls
- Gateway missing CA in its truststore → gateway cannot validate backend certs.
- Service missing CA in truststore → service rejects gateway client cert.
- Wrong keystore/truststore paths or passwords in Spring/JVM configuration.
- SAN/hostname mismatch: generated cert SANs must include the DNS names used by clients (the script sets `DNS.1=$SERVICE` and `DNS.2=localhost`).
- Not rebuilding containers after updating cert files — containers might still have old/stale certs.

## Verification commands
- Curl through gateway (example):
```bash
curl -vk --cert scripts/certs/gateway.p12:PSSWD --cacert scripts/certs/ca.crt \
  https://localhost:10000/api/users/register
```
- Inspect TLS handshake from gateway container to users-service:
```bash
openssl s_client -connect users-service:8080 -cert /app/resources/certs/gateway.p12 -CAfile /app/resources/certs/ca.crt
```
- Inspect PKCS#12 contents:
```bash
keytool -list -v -keystore scripts/certs/users-service.p12 -storetype PKCS12 -storepass <PSSWD>
```

## Quick troubleshooting checklist
1. Confirm the CA is present in `truststore.p12`:
```bash
keytool -list -keystore certs/truststore.p12 -storetype PKCS12 -storepass <PSSWD>
```
2. Confirm keystore and truststore paths and passwords are configured in Spring properties or JVM flags.
3. Check logs for SSL/TLS exceptions (`unknown CA`, `handshake_failure`, `peer not authenticated`).
4. Use `openssl s_client` to inspect the certs presented and the chain.
5. Ensure SANs include the hostnames clients use.
6. Rebuild/recreate containers after updating certs.

## Recommendations
- Keep a single canonical `truststore.p12` (the CA) and mount it into every container that initiates TLS connections.
- When regenerating certs, update truststores first and then rebuild containers to avoid temporary mismatches.
- Add automated checks that validate TLS endpoints during startup.

---
Generated from repository scan on May 23, 2026.
