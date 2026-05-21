#!/bin/bash

cd ../shared
mvn compile install 
cd ../script

docker network create shared-net

docker compose -f ../eureka-server/docker-compose.yaml up -d 

docker compose -f ../redis/docker-compose.yaml up -d 

docker compose -f ../kafka/docker-compose.yaml up -d 

# docker compose -f ../products-service/docker-compose.yaml up -d 

# docker compose -f ../media-service/docker-compose.yaml up -d 

# docker compose -f ../users-service/docker-compose.yaml up -d 

# docker compose -f ../gateway/docker-compose.yaml up   -d

# docker compose -f ../ci/jenkins/docker-compose.yaml up   -d

############################

# docker compose -f ../eureka-server/docker-compose.yaml up -d  --build
 
# docker compose -f ../redis/docker-compose.yaml up -d  --build

# docker compose -f ../kafka/docker-compose.yaml up -d  --build

docker compose -f ../products-service/docker-compose.yaml up -d  --build

docker compose -f ../media-service/docker-compose.yaml up -d  --build

docker compose -f ../users-service/docker-compose.yaml up -d  --build

docker compose -f ../gateway/docker-compose.yaml up   -d --build

# docker compose -f ../ci/jenkins/docker-compose.yaml up   -d --build