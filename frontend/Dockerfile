# Multi-stage build for Angular frontend

# Stage 1: Build stage
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

RUN npm ci
# Copy source code
COPY . .

# Build the application for production using the workspace-installed dependencies
RUN npm run build

# Stage 2: Runtime stage with Caddy
FROM caddy:2-alpine

RUN apk add --no-cache wget

# Copy the built Angular browser bundle into Caddy.
# Angular 20 emits an SSR build with a browser directory under dist/.
COPY --from=build /app/dist/ /tmp/dist/
RUN set -eux; \
  browser_dir="$(find /tmp/dist -type d -name browser | head -n 1)"; \
  test -n "$browser_dir"; \
  rm -rf /usr/share/caddy/*; \
  cp -r "$browser_dir"/. /usr/share/caddy/; \
    if [ -f /usr/share/caddy/index.csr.html ] && [ ! -f /usr/share/caddy/index.html ]; then \
      cp /usr/share/caddy/index.csr.html /usr/share/caddy/index.html; \
    fi; \
  rm -rf /tmp/dist

# Copy custom Caddy configuration
COPY Caddyfile /etc/caddy/Caddyfile

# Expose port
EXPOSE 443

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --no-check-certificate --tries=1 --spider https://localhost/ || exit 1

# Start Caddy
CMD ["caddy", "run", "--config", "/etc/caddy/Caddyfile", "--adapter", "caddyfile"]
