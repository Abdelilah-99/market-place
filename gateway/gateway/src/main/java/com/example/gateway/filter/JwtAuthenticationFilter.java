package com.example.gateway.filter;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter {

    private final RSAPublicKey rsaPublicKey;
    private final JwtParser jwtParser;

    public JwtAuthenticationFilter(RSAPublicKey rsaPublicKey) {
        this.rsaPublicKey = rsaPublicKey;
        this.jwtParser = Jwts.parser().verifyWith(rsaPublicKey).build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isPublicEndpoint(request.getPath().value())) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("logs ============================================ Guest");
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Role", "GUEST")
                    .build();
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } else {
            System.out.println("logs ============================================ jwt");

            String token = authHeader.substring(7);

            try {
                // System.out.println("subject: " + rsaPublicKey.getEncoded());
                var claims = jwtParser.parseSignedClaims(token).getPayload();
                String userId = claims.getSubject();
                String role = claims.get("role", String.class);

                // if (!hasAccess(request.getPath().value(), role)) {
                // return sendForbiddenError(exchange.getResponse(), "Insufficient
                // permissions");
                // }

                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Role", role)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (SignatureException e) {
                return sendUnauthorizedError(exchange.getResponse(), "Invalid token signature");
            } catch (Exception e) {
                return sendUnauthorizedError(exchange.getResponse(), "Invalid or expired token: " + e.getMessage());
            }
        }

    }

    private boolean isPublicEndpoint(String path) {
        System.out.println("public path ------------------------- " + path);
        List<String> publicPaths = List.of(
                "/api/users/login",
                // "/api/products",
                "/api/users/register");
        return publicPaths.stream().anyMatch(path::startsWith);
    }

    // private boolean hasAccess(String path, String role) {
    // if (path.startsWith("/api/products") && "SELLER".equals(role)) {
    // System.out.println("Seller here ======================================");
    // return true;
    // }
    // if (path.startsWith("/api/users/me") && "SELLER".equals(role)) {
    // return true;
    // }
    // if (path.startsWith("/api/seller") && "SELLER".equals(role)) {
    // return true;
    // }
    // return false;
    // }

    private Mono<Void> sendUnauthorizedError(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        byte[] body = String.format("{\"error\":\"%s\"}", message).getBytes();
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    private Mono<Void> sendForbiddenError(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.NOT_FOUND);
        response.getHeaders().add("Content-Type", "application/json");
        byte[] body = String.format("{\"error\":\"%s\"}", message).getBytes();
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }
}
