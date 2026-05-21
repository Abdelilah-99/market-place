# --- STAGE 1: Build ---
# Using Maven 3.9 image with Java 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# --- STAGE 2: Run ---
# Use the official Java 21 JRE for a slim final image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy only the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "app.jar"]