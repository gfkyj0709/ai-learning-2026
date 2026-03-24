package com.example.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the API Gateway service.
 * API Gateway 서비스의 진입점 클래스입니다.
 *
 * <p>Spring Cloud Gateway는 Project Reactor 기반의 리액티브 게이트웨이입니다.
 * Spring Cloud Gateway is a reactive gateway built on Project Reactor / WebFlux.</p>
 *
 * <h3>Routing rules / 라우팅 규칙</h3>
 * <ul>
 *   <li>{@code /rag/**}   → {@code http://localhost:8085} (rag-demo — RAG 엔드포인트)</li>
 *   <li>{@code /agent/**} → {@code http://localhost:8085} (rag-demo — FDS Agent 엔드포인트)</li>
 * </ul>
 *
 * <h3>Actuator endpoints / 운영 엔드포인트</h3>
 * <ul>
 *   <li>{@code GET /actuator/health}          — 헬스 체크 / Health check</li>
 *   <li>{@code GET /actuator/gateway/routes}  — 현재 라우팅 목록 조회 / List active routes</li>
 * </ul>
 */
@SpringBootApplication
public class ApiGatewayApplication {

    /**
     * Starts the Spring Boot application context.
     * Spring Boot 애플리케이션 컨텍스트를 시작합니다.
     *
     * @param args command-line arguments / 커맨드라인 인수
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
