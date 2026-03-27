# Build stage - Maven으로 JAR 빌드 / Maven build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set working directory / 작업 디렉토리 설정
WORKDIR /app

# Copy pom.xml and download dependencies first (layer cache optimization)
# pom.xml 먼저 복사하여 의존성 캐시 최적화
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build / 소스 복사 후 빌드
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage - 실행 스테이지 (경량 이미지)
FROM eclipse-temurin:21-jre-alpine

# Set working directory / 작업 디렉토리
WORKDIR /app

# Copy JAR from build stage / 빌드 스테이지에서 JAR 복사
COPY --from=build /app/target/*.jar app.jar

# Expose Eureka port / Eureka 포트 노출
EXPOSE 8761

# Run the application / 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
