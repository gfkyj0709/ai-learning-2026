package com.example.eurekaserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test — verifies the Eureka server application context loads without errors.
 * 스모크 테스트 — Eureka 서버 애플리케이션 컨텍스트가 오류 없이 로드되는지 확인합니다.
 */
@SpringBootTest
class EurekaServerApplicationTests {

    /**
     * Asserts that the application context starts successfully.
     * 애플리케이션 컨텍스트가 정상적으로 시작되는지 검증합니다.
     */
    @Test
    void contextLoads() {
        // context load itself is the assertion / 컨텍스트 로드 자체가 검증입니다
    }
}
