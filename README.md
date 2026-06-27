# E-Commerce Microservices Platform
A distributed e-commerce ecosystem built with Spring Boot, Angular, and a microservices architecture. This project leverages Eureka for service discovery, Spring Cloud Gateway for routing and rate limiting, Kafka for messaging, and Redis for gateway throttling.

# Architecture Overview
The system is composed of several specialized services:

- **frontend**: Angular-based web interface.

- **gateway**: Central entry point using Spring Cloud Gateway (includes rate limiting).

- **eureka-server**: Service registry and discovery.

- **users-service**: Manages user profiles, authentication, and authorization.

- **products-service**: Handles product catalog and inventory.

- **media-service**: Manages image uploads and static assets.

- **shared**: Common library containing shared DTOs and utilities.


# Technology Stack

| Component            | Technology                      |
| :------------------- | :------------------------------ |
| **Frontend**         | Angular 21.1.3                     |
| **API Gateway**      | Spring Cloud Gateway            |
| **Registry**         | Netflix Eureka Server           |
| **Databases**        | MongoDB (Per-service instances) |
| **File Storage**     | Spring Content FS               |
| **Rate Limiting**    | Redis                           |
| **Messaging**        | Apache Kafka                    |
| **Security**         | JWT, OpenSSL (MTLS/HTTPS)       |
| **Containerization** | Docker, Docker Compose          |

# Redis Integration

Redis is integrated as a supporting service for **Spring Cloud Gateway rate limiting**. It is not currently used for user sessions, product caching, media caching, queues, or pub/sub.

## Redis Container

Redis runs from `redis/docker-compose.yaml`:

```yaml
services:
  redis:
    image: redis:7
    container_name: redis
    ports:
      - "6379:6379"
    networks:
      - shared-net
```

The Redis container is attached to the external Docker network `shared-net`, which lets the gateway reach it by container name.

## Gateway Connection

The gateway reads the Redis connection values from `gateway/.env.gateway`:

```env
REDIS-HOST=redis
REDIS-PORT=6379
```

Those values are mapped into Spring Boot in `gateway/gateway/src/main/resources/application.properties`:

```properties
spring.redis.host=${REDIS-HOST}
spring.redis.port=${REDIS-PORT}
```

Inside Docker, `redis` resolves to the Redis container because both services are connected to `shared-net`.

## Java Redis Configuration

The gateway includes reactive Redis support through `spring-boot-starter-data-redis-reactive` in `gateway/gateway/pom.xml`.

`gateway/gateway/src/main/java/com/example/gateway/config/RedisConfig.java` creates the Redis connection:

```java
@Bean
public LettuceConnectionFactory redisConnectionFactory() {
    return new LettuceConnectionFactory(this.host, this.port);
}

@Bean
public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(LettuceConnectionFactory factory) {
    return new ReactiveRedisTemplate<>(factory, RedisSerializationContext.string());
}
```

Spring Cloud Gateway uses this Redis connection when applying the Redis-backed rate limiter.

## Rate Limiting Behavior

The rate limiter is configured in `gateway/gateway/src/main/java/com/example/gateway/config/GetwayConfig.java`:

```java
@Bean
public RedisRateLimiter redisRateLimiter() {
    return new RedisRateLimiter(5, 10);
}
```

This means:

- `5` requests per second are replenished.
- `10` requests are allowed as burst capacity.
- Requests beyond the limit receive `429 Too Many Requests`.

The gateway identifies each client by IP address:

```java
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> Mono.just(
        exchange.getRequest().getRemoteAddress()
            .getAddress()
            .getHostAddress()
    );
}
```

The limiter is applied to these gateway routes:

- `/api/products/**`
- `/api/users/**`
- `/api/media/**`

## Runtime Flow

1. A client sends a request to the gateway.
2. The gateway resolves the client's IP address.
3. Spring Cloud Gateway checks the Redis-backed token bucket for that IP.
4. If tokens are available, the request is forwarded to the target service.
5. If the limit is exceeded, the gateway returns `429 Too Many Requests`.

Because the Redis data is only used for rate-limit counters, the Redis container does not need persistent storage for the current project setup.

# Launching
The project includes orchestration scripts to manage the complex microservice lifecycle:

 - Start All Services: ./scripts/run-all.sh

 - Stop All Services: ./scripts/stop.sh

 - Full Cleanup: ./scripts/remove-all.sh
