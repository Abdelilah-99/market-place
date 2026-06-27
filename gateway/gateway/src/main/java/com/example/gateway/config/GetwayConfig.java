package com.example.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GetwayConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        // Rate limit per client IP
        return exchange -> Mono.just(
                exchange.getRequest().getRemoteAddress()
                        .getAddress()
                        .getHostAddress()
        );
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // 5 requests/sec, burst capacity 10
        return new RedisRateLimiter(5, 10);
    }

    @Bean
    public RouteLocator routerBuilder(RouteLocatorBuilder builder, RedisRateLimiter redisRateLimiter, KeyResolver keyResolver) {
        return builder.routes()
                .route("products", r -> r
                        .path("/api/products/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(redisRateLimiter);
                                    c.setKeyResolver(keyResolver);
                                })
                        )
                        .uri("lb://products"))
                .route("products-admin", r -> r
                        .path("/api/admin/products/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(redisRateLimiter);
                                    c.setKeyResolver(keyResolver);
                                })
                        )
                        .uri("lb://products"))
                .route("users", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(redisRateLimiter);
                                    c.setKeyResolver(keyResolver);
                                })
                        )
                        .uri("lb://users"))
                .route("media", r -> r
                        .path("/api/media/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(redisRateLimiter);
                                    c.setKeyResolver(keyResolver);
                                })
                        )
                        .uri("lb://media"))
                .build();
    }
}
