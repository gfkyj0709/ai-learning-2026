package com.example.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Config Server 컨텍스트 로딩 테스트 / Config Server context loading test.
 *
 * <p>Spring 애플리케이션 컨텍스트가 올바르게 시작되는지 검증합니다.
 * Validates that the Spring application context starts up correctly.
 */
@SpringBootTest
class ConfigServerApplicationTests {

    /**
     * 애플리케이션 컨텍스트가 예외 없이 로드되는지 확인합니다.
     * Verifies that the application context loads without exceptions.
     */
    @Test
    void contextLoads() {
        // 컨텍스트 로딩 성공 시 테스트 통과 / Test passes if context loads successfully
    }
}
