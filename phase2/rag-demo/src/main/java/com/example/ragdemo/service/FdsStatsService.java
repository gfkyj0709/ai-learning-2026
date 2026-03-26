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
 *     ├── getFdsStats(yearMonth) ──▶ FdsStatsService.getFdsStats(yearMonth)
 *     │                                 ├── [캐시 HIT]  → Redis 즉시 반환
 *     │                                 └── [캐시 MISS] → PostgreSQL 조회 → Redis 저장
 *     │
 *     └── compareFdsStats(base, target) ──▶ FdsStatsService.compareFdsStats(base, target)
 *                                               ├── [캐시 HIT]  → Redis 즉시 반환
 *                                               └── [캐시 MISS] → PostgreSQL 2회 조회
 *                                                               → 포맷 후 Redis 저장
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
    @Cacheable(value = "fds-stats", key = "#yearMonth")
    public Optional<FdsStats> getFdsStats(String yearMonth) {
        log.debug("[FdsStatsService] Cache MISS — querying DB for yearMonth={}", yearMonth);
        return fdsStatsRepository.findByYearMonth(yearMonth);
    }

    /**
     * 두 연월(YYYYMM)의 FDS 통계를 비교한 보고서 문자열을 반환합니다.
     * 결과 전체를 캐시하므로, 캐시 히트 시 DB를 전혀 조회하지 않습니다.
     *
     * Returns a formatted comparison report string for two year-months (YYYYMM).
     * The entire result is cached, so no DB query is made on a cache hit.
     *
     * <h4>캐시 동작 / Cache behavior</h4>
     * <ul>
     *   <li>캐시 이름: {@code fds-compare} / Cache name: {@code fds-compare}</li>
     *   <li>캐시 키: {@code "base-target"} (예: {@code "202501-202502"}) /
     *       Cache key: {@code "base-target"} (e.g. {@code "202501-202502"})</li>
     *   <li>TTL: 1시간 ({@link com.example.ragdemo.config.RedisConfig} 참조) /
     *       TTL: 1 hour (see {@link com.example.ragdemo.config.RedisConfig})</li>
     * </ul>
     *
     * <h4>Redis 키 예시 / Redis key example</h4>
     * <pre>{@code fds-compare::202501-202502}</pre>
     *
     * @param base   기준 연월 (YYYYMM 형식, 예: "202501") /
     *               base year-month (YYYYMM format, e.g. "202501")
     * @param target 비교 대상 연월 (YYYYMM 형식, 예: "202502") /
     *               target year-month to compare (YYYYMM format, e.g. "202502")
     * @return 포맷된 비교 보고서 문자열, 데이터 없을 경우 안내 메시지 /
     *         formatted comparison report string, or guidance message if data is missing
     */
    @Cacheable(value = "fds-compare", key = "#base + '-' + #target")
    public String compareFdsStats(String base, String target) {
        log.debug("[FdsStatsService] Cache MISS — comparing {} vs {}", base, target);

        // 두 연월 모두 DB에서 직접 조회 (self-invocation 캐시 미적용 방지)
        // Query DB directly for both months (avoids self-invocation cache bypass)
        FdsStats baseStats   = fdsStatsRepository.findByYearMonth(base).orElse(null);
        FdsStats targetStats = fdsStatsRepository.findByYearMonth(target).orElse(null);

        if (baseStats == null) {
            return noDataMessage(base);
        }
        if (targetStats == null) {
            return noDataMessage(target);
        }

        double baseDetRate   = pct(baseStats.getDetected(),   baseStats.getTotalCount());
        double targetDetRate = pct(targetStats.getDetected(), targetStats.getTotalCount());

        return """
                === FDS 전월 비교 보고서 | Month-over-Month Comparison ===
                기준 월 / Base Month   : %s
                비교 월 / Target Month : %s
                ──────────────────────────────────────────────────
                항목                          기준 월        비교 월        증감률
                Item                          Base           Target         Change
                ──────────────────────────────────────────────────
                총 거래 건수 / Transactions  : %,d         %,d         %+.2f%%
                탐지 건수    / Detected      : %,d         %,d         %+.2f%%
                탐지율       / Det. Rate     : %.3f%%       %.3f%%       %+.2f%%
                룰 기반      / Rule-based    : %,d         %,d         %+.2f%%
                ML 기반      / ML-based      : %,d         %,d         %+.2f%%
                ──────────────────────────────────────────────────
                위험도 분포 (비교 월 기준) / Risk Distribution (Target Month):
                  고위험 / High : %,d건
                  중위험 / Mid  : %,d건
                  저위험 / Low  : %,d건
                """.formatted(
                base, target,
                baseStats.getTotalCount(),  targetStats.getTotalCount(),
                        chg(baseStats.getTotalCount(), targetStats.getTotalCount()),
                baseStats.getDetected(),    targetStats.getDetected(),
                        chg(baseStats.getDetected(), targetStats.getDetected()),
                baseDetRate,                targetDetRate,
                        chg((long)(baseDetRate * 1_000_000), (long)(targetDetRate * 1_000_000)),
                baseStats.getRuleBased(),   targetStats.getRuleBased(),
                        chg(baseStats.getRuleBased(), targetStats.getRuleBased()),
                baseStats.getMlBased(),     targetStats.getMlBased(),
                        chg(baseStats.getMlBased(), targetStats.getMlBased()),
                targetStats.getHighRisk(),
                targetStats.getMidRisk(),
                targetStats.getLowRisk()
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / 내부 헬퍼 메서드
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 데이터 없음 안내 메시지 생성 / Generates a "no data" guidance message.
     *
     * @param yearMonth 데이터가 없는 연월 / year-month with no data
     * @return 안내 메시지 / guidance message
     */
    private String noDataMessage(String yearMonth) {
        return "[FDS Service] %s 월의 통계 데이터가 존재하지 않습니다. / No statistics data found for %s."
                .formatted(yearMonth, yearMonth);
    }

    /**
     * 분자/분모로 비율(%)을 계산합니다. 분모가 0이면 0 반환.
     * Calculates a percentage from numerator/denominator. Returns 0 if denominator is 0.
     */
    private double pct(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator * 100.0;
    }

    /**
     * 기준값 대비 비교값의 증감률(%)을 계산합니다. 기준값이 0이면 0 반환.
     * Calculates the change rate (%) of target vs base. Returns 0 if base is 0.
     */
    private double chg(long base, long target) {
        return base == 0 ? 0.0 : (double)(target - base) / base * 100.0;
    }
}
