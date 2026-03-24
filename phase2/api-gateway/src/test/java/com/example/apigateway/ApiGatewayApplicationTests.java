package com.example.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test — verifies the Spring application context loads without errors.
 * 스모크 테스트 — Spring 애플리케이션 컨텍스트가 오류 없이 로드되는지 확인합니다.
 */
@SpringBootTest
class ApiGatewayApplicationTests {

    /**
     * Asserts that the application context starts successfully.
     * 애플리케이션 컨텍스트가 정상적으로 시작되는지 검증합니다.
     */
    @Test
    void contextLoads() {
        // context load itself is the assertion / 컨텍스트 로드 자체가 검증입니다
    }
}
