package com.example.ragdemo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FDS(이상거래탐지시스템) 월별 통계 엔티티.
 * FDS (Fraud Detection System) monthly statistics entity.
 *
 * <p>PostgreSQL {@code fds_stats} 테이블과 1:1로 매핑됩니다.
 * Maps 1:1 to the PostgreSQL {@code fds_stats} table.
 *
 * <h3>테이블 구조 / Table DDL</h3>
 * <pre>{@code
 * CREATE TABLE fds_stats (
 *     id           BIGSERIAL PRIMARY KEY,
 *     year_month   VARCHAR(6)  NOT NULL UNIQUE,  -- YYYYMM 형식
 *     total_count  BIGINT      NOT NULL,
 *     detected     BIGINT      NOT NULL,
 *     high_risk    BIGINT      NOT NULL,
 *     mid_risk     BIGINT      NOT NULL,
 *     low_risk     BIGINT      NOT NULL,
 *     rule_based   BIGINT      NOT NULL,
 *     ml_based     BIGINT      NOT NULL,
 *     created_at   TIMESTAMP   NOT NULL DEFAULT NOW()
 * );
 * }</pre>
 */
@Entity
@Table(name = "fds_stats")
@Getter
@NoArgsConstructor
public class FdsStats {

    /**
     * 기본키 (자동 증가) / Primary key (auto-increment).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 통계 기준 연월 (YYYYMM 형식, 유니크 제약) / Reference year-month (YYYYMM format, unique constraint).
     * 예: "202502"
     */
    @Column(name = "year_month", nullable = false, unique = true, length = 6)
    private String yearMonth;

    /**
     * 해당 월 총 거래 건수 / Total number of transactions in the month.
     */
    @Column(name = "total_count", nullable = false)
    private Long totalCount;

    /**
     * 이상 탐지된 총 건수 / Total number of detected anomalies.
     */
    @Column(name = "detected", nullable = false)
    private Long detected;

    /**
     * 고위험(High Risk)으로 분류된 탐지 건수 / Number of detections classified as high risk.
     */
    @Column(name = "high_risk", nullable = false)
    private Long highRisk;

    /**
     * 중위험(Mid Risk)으로 분류된 탐지 건수 / Number of detections classified as mid risk.
     */
    @Column(name = "mid_risk", nullable = false)
    private Long midRisk;

    /**
     * 저위험(Low Risk)으로 분류된 탐지 건수 / Number of detections classified as low risk.
     */
    @Column(name = "low_risk", nullable = false)
    private Long lowRisk;

    /**
     * 룰 기반(Rule-based) 엔진이 탐지한 건수 / Number of detections by the rule-based engine.
     */
    @Column(name = "rule_based", nullable = false)
    private Long ruleBased;

    /**
     * ML 모델이 탐지한 건수 / Number of detections by the ML model.
     */
    @Column(name = "ml_based", nullable = false)
    private Long mlBased;

    /**
     * 레코드 생성 시각 (자동 설정) / Record creation timestamp (set automatically).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 엔티티 저장 전 {@code createdAt}을 현재 시각으로 자동 설정합니다.
     * Automatically sets {@code createdAt} to the current time before persisting.
     */
    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
