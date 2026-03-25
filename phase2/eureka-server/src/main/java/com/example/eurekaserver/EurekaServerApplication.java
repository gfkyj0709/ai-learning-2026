package com.example.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Entry point for the Eureka Service Registry server.
 * Eureka 서비스 레지스트리 서버의 진입점 클래스입니다.
 *
 * <p>{@link EnableEurekaServer}를 선언하면 Spring Cloud가 이 애플리케이션을
 * Eureka 서버(서비스 레지스트리)로 동작하도록 자동 구성합니다.<br>
 * Declaring {@link EnableEurekaServer} instructs Spring Cloud to auto-configure
 * this application as a Eureka server (service registry).</p>
 *
 * <h3>접근 URL / Access URLs</h3>
 * <ul>
 *   <li>Eureka 대시보드: {@code http://localhost:8761}</li>
 *   <li>등록된 서비스 목록 (REST): {@code GET http://localhost:8761/eureka/apps}</li>
 *   <li>헬스 체크: {@code GET http://localhost:8761/actuator/health}</li>
 * </ul>
 *
 * <h3>동작 모드 / Operating mode</h3>
 * <ul>
 *   <li>{@code register-with-eureka: false} — 자기 자신을 레지스트리에 등록하지 않음 / Does not register itself</li>
 *   <li>{@code fetch-registry: false} — 피어 서버에서 레지스트리를 가져오지 않음 / Does not fetch from peers</li>
 *   <li>{@code enable-self-preservation: false} — 개발용 자기 보존 모드 비활성화 / Self-preservation disabled for dev</li>
 * </ul>
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    /**
     * Starts the Eureka server application context.
     * Eureka 서버 애플리케이션 컨텍스트를 시작합니다.
     *
     * @param args command-line arguments / 커맨드라인 인수
     */
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
