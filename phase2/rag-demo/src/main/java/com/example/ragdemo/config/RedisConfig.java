package com.example.ragdemo.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 캐시 설정 클래스.
 * Redis cache configuration class.
 *
 * <h3>설정 요약 / Configuration summary</h3>
 * <ul>
 *   <li>캐시 직렬화 방식: JSON (GenericJackson2JsonRedisSerializer) /
 *       Cache serialization: JSON (GenericJackson2JsonRedisSerializer)</li>
 *   <li>기본 TTL: 1시간 (3600초) / Default TTL: 1 hour (3600 seconds)</li>
 *   <li>키 직렬화: StringRedisSerializer (가독성 향상) /
 *       Key serialization: StringRedisSerializer (for readability)</li>
 *   <li>null 캐싱 비활성화 — DB에 없는 데이터는 캐시하지 않습니다 /
 *       Null caching disabled — values absent from DB are not cached</li>
 * </ul>
 *
 * <h3>Jackson 직렬화 주의사항 / Jackson serialization note</h3>
 * <p>{@code FdsStats}는 private 필드만 가지므로 ObjectMapper에
 * {@code FIELD} 가시성을 설정하여 리플렉션으로 직접 접근합니다.
 * {@code FdsStats} has only private fields, so the ObjectMapper is configured
 * with {@code FIELD} visibility to access them directly via reflection.
 */
@Configuration
@EnableCaching  // @Cacheable, @CacheEvict 등 Spring Cache 어노테이션 활성화 / Enable Spring Cache annotations
public class RedisConfig {

    /**
     * Redis 캐시에 사용할 Jackson ObjectMapper를 생성합니다.
     * Creates the Jackson ObjectMapper used for Redis cache serialization.
     *
     * <ul>
     *   <li>{@code FIELD} 가시성: private 필드 직접 접근 허용 /
     *       {@code FIELD} visibility: allows direct access to private fields</li>
     *   <li>{@code JavaTimeModule}: {@code LocalDateTime} 등 Java 8 날짜 타입 지원 /
     *       {@code JavaTimeModule}: supports Java 8 date types like {@code LocalDateTime}</li>
     *   <li>{@code DefaultTyping}: 역직렬화 시 타입 정보를 JSON에 포함하여 올바른 클래스로 복원 /
     *       {@code DefaultTyping}: embeds type info in JSON for correct class restoration on deserialization</li>
     * </ul>
     *
     * @return Redis 캐시 전용 ObjectMapper / ObjectMapper dedicated to Redis caching
     */
    @Bean(name = "redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // private 필드에 직접 접근 허용 (getter/setter 없이 직렬화·역직렬화 가능)
        // Allow direct access to private fields (serialize/deserialize without getters/setters)
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        // getter 기반 자동 감지 비활성화 — FIELD 설정과 충돌 방지
        // Disable getter-based auto-detection — prevents conflict with FIELD setting
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);

        // Java 8 날짜/시간 타입(LocalDateTime 등) 지원 모듈 등록
        // Register module for Java 8 date/time types (LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());
        // LocalDateTime을 타임스탬프 숫자 대신 ISO-8601 문자열로 직렬화
        // Serialize LocalDateTime as ISO-8601 string instead of numeric timestamp
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // JSON에 클래스 타입 정보 포함 — Redis에서 꺼낼 때 올바른 타입으로 역직렬화하기 위해 필요
        // Embed class type info in JSON — required to deserialize to the correct type when reading from Redis
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return mapper;
    }

    /**
     * Redis 캐시 관리자(CacheManager)를 생성합니다.
     * Creates the Redis CacheManager.
     *
     * <p>모든 캐시에 동일한 기본 설정(TTL 1시간, JSON 직렬화, null 캐싱 비활성화)을 적용합니다.
     * Applies the same default settings (1-hour TTL, JSON serialization, no null caching)
     * to all caches managed by this manager.
     *
     * @param connectionFactory Spring Boot가 자동 구성한 Redis 연결 팩토리 /
     *                          Redis connection factory auto-configured by Spring Boot
     * @return 설정된 RedisCacheManager / configured RedisCacheManager
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // JSON 직렬화기 생성 (위에서 만든 타입 정보 포함 ObjectMapper 사용)
        // Create JSON serializer (using the type-aware ObjectMapper configured above)
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 기본 TTL: 1시간 / Default TTL: 1 hour
                .entryTtl(Duration.ofHours(1))
                // 캐시 키: 문자열 직렬화 (Redis에서 사람이 읽기 쉬운 키)
                // Cache key: string serialization (human-readable keys in Redis)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()))
                // 캐시 값: JSON 직렬화 / Cache value: JSON serialization
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                // null 값은 캐시하지 않음 — DB에 없는 데이터도 매번 DB를 조회합니다
                // Do not cache null values — absent DB records are always re-queried
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
