# Build stage - Maven으로 JAR 빌드 / Maven build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Run stage - 실행 스테이지 (경량 이미지)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Expose Gateway port / Gateway 포트 노출
EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
