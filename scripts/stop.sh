#!/bin/bash

docker compose -f ../gateway/docker-compose.yaml down

docker compose -f ../eureka-server/docker-compose.yaml down

docker compose -f ../products-service/docker-compose.yaml down

docker compose -f ../users-service/docker-compose.yaml down

docker compose -f ../media-service/docker-compose.yaml down

docker compose -f ../kafka/docker-compose.yaml down

docker compose -f ../redis/docker-compose.yaml down

docker compose -f ../opensearch/docker-compose.yaml down
