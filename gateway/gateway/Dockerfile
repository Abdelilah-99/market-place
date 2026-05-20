FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
ARG GITHUB_TOKEN
RUN echo "<settings><servers><server><id>github</id><username>Abdelilah-99</username><password>${GITHUB_TOKEN}</password></server></servers></settings>" > settings.xml
COPY pom.xml .
RUN mvn dependency:go-offline -B -s settings.xml
COPY src ./src
RUN mvn clean package -DskipTests -s settings.xml

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "app.jar"]