FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY payments-service/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B

COPY payments-service/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8010
ENTRYPOINT ["java", "-jar", "app.jar"]
