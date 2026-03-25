package com.example.ragdemo.repository;

import com.example.ragdemo.entity.FdsStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * {@link FdsStats} 엔티티에 대한 Spring Data JPA 리포지토리.
 * Spring Data JPA repository for the {@link FdsStats} entity.
 *
 * <p>기본 CRUD 메서드는 {@link JpaRepository}에서 자동 제공됩니다.
 * Basic CRUD methods are automatically provided by {@link JpaRepository}.
 *
 * <h3>사용 예시 / Usage example</h3>
 * <pre>{@code
 * // 202502 월 통계 조회
 * Optional<FdsStats> stats = repository.findByYearMonth("202502");
 * stats.ifPresent(s -> System.out.println(s.getDetected()));
 * }</pre>
 */
public interface FdsStatsRepository extends JpaRepository<FdsStats, Long> {

    /**
     * 연월(YYYYMM)로 FDS 통계 레코드를 조회합니다.
     * Retrieves an FDS statistics record by year-month (YYYYMM).
     *
     * <p>Spring Data JPA가 메서드 이름을 분석하여 쿼리를 자동 생성합니다.
     * Spring Data JPA automatically generates the query by parsing the method name.
     * ({@code SELECT * FROM fds_stats WHERE year_month = ?})
     *
     * @param yearMonth 조회 대상 연월 (YYYYMM 형식, 예: "202502") /
     *                  target year-month (YYYYMM format, e.g. "202502")
     * @return 해당 연월의 통계 데이터, 없으면 빈 Optional /
     *         statistics for the given year-month, or empty if not found
     */
    Optional<FdsStats> findByYearMonth(String yearMonth);
}
