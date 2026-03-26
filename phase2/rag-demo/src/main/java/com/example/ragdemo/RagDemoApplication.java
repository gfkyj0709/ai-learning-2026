package com.example.ragdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * RAG Demo 애플리케이션 진입점.
 * Entry point for the RAG Demo application.
 *
 * <p>{@code @EnableCaching}을 선언하여 {@code @Cacheable} 등 Spring Cache 어노테이션을 활성화합니다.
 * Declares {@code @EnableCaching} to activate Spring Cache annotations such as {@code @Cacheable}.
 *
 * <p>실제 캐시 구현체(RedisCacheManager) 설정은 {@link com.example.ragdemo.config.RedisConfig}를 참조하세요.
 * For the actual cache implementation (RedisCacheManager) configuration, see
 * {@link com.example.ragdemo.config.RedisConfig}.
 */
@EnableCaching
@SpringBootApplication
public class RagDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagDemoApplication.class, args);
    }
}
