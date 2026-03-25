package com.example.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Config Server Application — phase2 마이크로서비스들의 중앙 집중식 설정 서버.
 *
 * <p>Config Server Application — centralized configuration server for phase2 microservices.
 *
 * <h3>역할 / Role</h3>
 * <ul>
 *   <li>클라이언트 애플리케이션(rag-demo, api-gateway 등)이 시작될 때 이 서버에서 설정을 가져옵니다.</li>
 *   <li>Client applications (rag-demo, api-gateway, etc.) fetch their configuration from this server at startup.</li>
 *   <li>native 프로파일: classpath:/config 디렉토리의 YAML/properties 파일을 설정 소스로 사용합니다.</li>
 *   <li>native profile: uses YAML/properties files in classpath:/config directory as the config source.</li>
 * </ul>
 *
 * <h3>엔드포인트 예시 / Endpoint examples</h3>
 * <ul>
 *   <li>{@code GET /{application}/{profile}} — 특정 앱·프로파일의 설정 조회 / Fetch config for app+profile</li>
 *   <li>{@code GET /rag-demo/default} — rag-demo의 기본 설정 조회</li>
 *   <li>{@code GET /api-gateway/default} — api-gateway의 기본 설정 조회</li>
 * </ul>
 *
 * <h3>포트 / Port</h3>
 * <p>8888 (Spring Cloud Config Server 기본 포트 / default port for Spring Cloud Config Server)</p>
 *
 * @see org.springframework.cloud.config.server.EnableConfigServer
 */
@SpringBootApplication
@EnableConfigServer  // Spring Cloud Config Server 기능 활성화 / Enable Config Server functionality
public class ConfigServerApplication {

    /**
     * 애플리케이션 진입점 / Application entry point.
     *
     * @param args 커맨드라인 인수 / command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
