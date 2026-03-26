package com.example.ragdemo.tool;

import com.example.ragdemo.entity.FdsStats;
import com.example.ragdemo.service.FdsStatsService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * LangChain4j Tool providing FDS (Fraud Detection System) statistical data.
 * FDS(이상거래탐지시스템) 통계 데이터를 제공하는 LangChain4j 도구 클래스.
 *
 * <p>각 메서드에 {@code @Tool} 어노테이션이 붙으면 AiServices가 자동으로
 * 해당 메서드를 LLM 호출 가능한 함수로 등록합니다.
 * Methods annotated with {@code @Tool} are automatically registered as
 * callable functions by AiServices when the agent decides to use them.
 *
 * <p>Redis 캐시를 우선 조회하고, 미스(miss) 시에만 PostgreSQL을 조회합니다.
 * Checks the Redis cache first; queries PostgreSQL only on a cache miss.
 * 캐싱 로직은 {@link FdsStatsService}에 위임합니다.
 * Caching logic is delegated to {@link FdsStatsService}.
 */
@Component
@RequiredArgsConstructor
public class FdsTool {

    /**
     * Redis 캐시를 통한 FDS 통계 조회 서비스 / FDS statistics service with Redis caching.
     */
    private final FdsStatsService fdsStatsService;

    /**
     * Retrieves FDS detection statistics for the given year-month from the database.
     * 지정된 연월의 이상탐지 통계를 데이터베이스에서 조회합니다.
     *
     * <p>Redis 캐시({@code fds-stats})를 먼저 확인하고, 미스 시 PostgreSQL을 조회합니다.
     * Checks the Redis cache ({@code fds-stats}) first; queries PostgreSQL only on a cache miss.
     * 해당 데이터가 없으면 "데이터 없음" 메시지를 반환합니다.
     * Returns a "no data" message if the record does not exist.
     *
     * @param yearMonth target year-month in {@code YYYYMM} format (예: "202502")
     *                  / 조회 대상 연월 (형식: YYYYMM, 예: "202502")
     * @return formatted statistics string / 포맷된 통계 문자열
     */
    @Tool("Retrieve FDS anomaly detection statistics for a given year-month (YYYYMM). " +
          "지정한 연월(YYYYMM)의 이상탐지 통계를 조회합니다.")
    public String getFdsStats(String yearMonth) {
        return fdsStatsService.getFdsStats(yearMonth)
                .map(this::formatStats)
                .orElse(noDataMessage(yearMonth));
    }

    /**
     * Compares FDS statistics between a base month and a target month from the database.
     * 기준 월과 비교 월의 이상탐지 통계를 데이터베이스에서 조회하여 비교합니다.
     *
     * <p>두 연월 모두 {@code fds_stats} 테이블에 존재해야 비교 리포트를 생성합니다.
     * Both year-months must exist in the {@code fds_stats} table to generate a comparison report.
     * 어느 한쪽이라도 없으면 해당 연월의 "데이터 없음" 메시지를 반환합니다.
     * If either is missing, returns a "no data" message for that year-month.
     *
     * @param base   base year-month in {@code YYYYMM} format / 기준 연월 (형식: YYYYMM)
     * @param target target year-month to compare against / 비교 대상 연월 (형식: YYYYMM)
     * @return formatted comparison report / 포맷된 비교 리포트 문자열
     */
    @Tool("Compare FDS anomaly detection statistics between two months (YYYYMM). " +
          "두 연월(YYYYMM) 간 이상탐지 통계를 비교합니다.")
    public String compareFdsStats(String base, String target) {
        // Redis 캐시 우선 조회, 미스 시 DB 조회 / Check Redis cache first, query DB on miss
        FdsStats baseStats   = fdsStatsService.getFdsStats(base).orElse(null);
        FdsStats targetStats = fdsStatsService.getFdsStats(target).orElse(null);

        // 어느 한쪽이라도 없으면 조기 반환 / Early return if either record is missing
        if (baseStats == null) {
            return noDataMessage(base);
        }
        if (targetStats == null) {
            return noDataMessage(target);
        }

        // 탐지율 계산 (소수점 3자리) / Calculate detection rate (3 decimal places)
        double baseDetRate   = rate(baseStats.getDetected(),   baseStats.getTotalCount());
        double targetDetRate = rate(targetStats.getDetected(), targetStats.getTotalCount());

        // 증감률 계산 (% 포인트) / Calculate change rates (% points)
        double detectedChange   = changeRate(baseStats.getDetected(),   targetStats.getDetected());
        double totalChange      = changeRate(baseStats.getTotalCount(), targetStats.getTotalCount());
        double detRateChange    = changeRate((long)(baseDetRate * 1_000_000), (long)(targetDetRate * 1_000_000));
        double ruleBasedChange  = changeRate(baseStats.getRuleBased(),  targetStats.getRuleBased());
        double mlBasedChange    = changeRate(baseStats.getMlBased(),    targetStats.getMlBased());

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
                baseStats.getTotalCount(),  targetStats.getTotalCount(),  totalChange,
                baseStats.getDetected(),    targetStats.getDetected(),    detectedChange,
                baseDetRate,                targetDetRate,                detRateChange,
                baseStats.getRuleBased(),   targetStats.getRuleBased(),   ruleBasedChange,
                baseStats.getMlBased(),     targetStats.getMlBased(),     mlBasedChange,
                targetStats.getHighRisk(),
                targetStats.getMidRisk(),
                targetStats.getLowRisk()
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / 내부 헬퍼 메서드
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * {@link FdsStats} 엔티티를 단일 통계 보고서 문자열로 포맷합니다.
     * Formats a {@link FdsStats} entity into a single statistics report string.
     *
     * @param s FDS 통계 엔티티 / FDS statistics entity
     * @return 포맷된 보고서 문자열 / formatted report string
     */
    private String formatStats(FdsStats s) {
        double detectionRate = rate(s.getDetected(), s.getTotalCount());
        return """
                === FDS 통계 보고서 | FDS Statistics Report ===
                기준 월 / Reference Month : %s
                ─────────────────────────────────────────
                총 거래 건수   / Total Transactions  : %,d
                탐지 건수      / Detected Anomalies  : %,d
                탐지율         / Detection Rate      : %.3f%%
                ─────────────────────────────────────────
                위험도 분포 / Risk Distribution :
                  고위험 / High : %,d건
                  중위험 / Mid  : %,d건
                  저위험 / Low  : %,d건
                ─────────────────────────────────────────
                룰 기반 탐지   / Rule-based          : %,d건
                ML 모델 탐지   / ML Model            : %,d건
                """.formatted(
                s.getYearMonth(),
                s.getTotalCount(),
                s.getDetected(),
                detectionRate,
                s.getHighRisk(),
                s.getMidRisk(),
                s.getLowRisk(),
                s.getRuleBased(),
                s.getMlBased()
        );
    }

    /**
     * 해당 연월 데이터가 없을 때 반환하는 안내 메시지를 생성합니다.
     * Generates a guidance message returned when no data exists for a given year-month.
     *
     * @param yearMonth 데이터가 없는 연월 / year-month with no data
     * @return 안내 메시지 문자열 / guidance message string
     */
    private String noDataMessage(String yearMonth) {
        return "[FDS Tool] %s 월의 통계 데이터가 존재하지 않습니다. / No statistics data found for %s."
                .formatted(yearMonth, yearMonth);
    }

    /**
     * 분자/분모로 비율(퍼센트)을 계산합니다. 분모가 0이면 0을 반환합니다.
     * Calculates a rate (percentage) from numerator/denominator. Returns 0 if denominator is 0.
     *
     * @param numerator   분자 / numerator
     * @param denominator 분모 / denominator
     * @return 퍼센트 값 / percentage value
     */
    private double rate(long numerator, long denominator) {
        if (denominator == 0) return 0.0;
        return (double) numerator / denominator * 100.0;
    }

    /**
     * 기준값 대비 비교값의 증감률(%)을 계산합니다. 기준값이 0이면 0을 반환합니다.
     * Calculates the change rate (%) of target compared to base. Returns 0 if base is 0.
     *
     * @param base   기준값 / base value
     * @param target 비교값 / target value
     * @return 증감률 (%) / change rate (%)
     */
    private double changeRate(long base, long target) {
        if (base == 0) return 0.0;
        return (double)(target - base) / base * 100.0;
    }
}
