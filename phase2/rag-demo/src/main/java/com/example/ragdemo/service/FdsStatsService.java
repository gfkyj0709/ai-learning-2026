package com.example.ragdemo.service;

import com.example.ragdemo.entity.FdsStats;
import com.example.ragdemo.repository.FdsStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * FDS(이상거래탐지시스템) 통계 서비스.
 * FDS (Fraud Detection System) statistics service.
 *
 * <h3>캐싱 전략 / Caching strategy</h3>
 * <p>{@code getFdsStats()} 는 Redis 캐시({@code fds-stats})를 먼저 확인합니다.
 * {@code getFdsStats()} checks the Redis cache ({@code fds-stats}) first.
 *
 * <pre>
 * 요청 흐름 / Request flow:
 *
 *   FdsTool
 *     │
 *     ▼
 *   FdsStatsService.getFdsStats(yearMonth)
 *     │
 *     ├── [캐시 HIT / Cache HIT]  → Redis에서 즉시 반환 (DB 조회 없음)
 *     │                              Returned from Redis immediately (no DB query)
 *     │
 *     └── [캐시 MISS / Cache MISS] → PostgreSQL fds_stats 조회
 *                                     → 결과를 Redis에 저장 (TTL: 1시간)
 *                                     Query PostgreSQL fds_stats
 *                                     → Store result in Redis (TTL: 1 hour)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FdsStatsService {

    /**
     * FDS 통계 데이터 접근을 위한 JPA 리포지토리 / JPA repository for FDS statistics data access.
     */
    private final FdsStatsRepository fdsStatsRepository;

    /**
     * 연월(YYYYMM)로 FDS 통계를 조회합니다. 캐시 히트 시 DB를 조회하지 않습니다.
     * Retrieves FDS statistics by year-month (YYYYMM). No DB query on cache hit.
     *
     * <h4>캐시 동작 / Cache behavior</h4>
     * <ul>
     *   <li>캐시 이름: {@code fds-stats} / Cache name: {@code fds-stats}</li>
     *   <li>캐시 키: yearMonth 파라미터 값 (예: {@code "202502"}) /
     *       Cache key: yearMonth parameter value (e.g. {@code "202502"})</li>
     *   <li>TTL: 1시간 ({@link com.example.ragdemo.config.RedisConfig} 참조) /
     *       TTL: 1 hour (see {@link com.example.ragdemo.config.RedisConfig})</li>
     *   <li>{@code unless = "#result.isEmpty()"}: DB에 데이터가 없는 경우 캐시하지 않음 /
     *       {@code unless = "#result.isEmpty()"}: does not cache when no DB record exists</li>
     * </ul>
     *
     * <h4>Redis 키 예시 / Redis key example</h4>
     * <pre>{@code fds-stats::202502}</pre>
     *
     * @param yearMonth 조회 대상 연월 (YYYYMM 형식, 예: "202502") /
     *                  target year-month (YYYYMM format, e.g. "202502")
     * @return DB에 데이터가 있으면 {@code Optional<FdsStats>}, 없으면 {@code Optional.empty()} /
     *         {@code Optional<FdsStats>} if found in DB, {@code Optional.empty()} otherwise
     */
    @Cacheable(value = "fds-stats", key = "#yearMonth", unless = "#result.isEmpty()")
    public Optional<FdsStats> getFdsStats(String yearMonth) {
        log.debug("[FdsStatsService] Cache MISS — querying DB for yearMonth={}", yearMonth);
        return fdsStatsRepository.findByYearMonth(yearMonth);
    }
}
