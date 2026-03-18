package com.example.ragdemo.tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * LangChain4j Tool providing FDS (Fraud Detection System) statistical data.
 * FDS(이상거래탐지시스템) 통계 데이터를 제공하는 LangChain4j 도구 클래스.
 *
 * <p>각 메서드에 {@code @Tool} 어노테이션이 붙으면 AiServices가 자동으로
 * 해당 메서드를 LLM 호출 가능한 함수로 등록합니다.
 * Methods annotated with {@code @Tool} are automatically registered as
 * callable functions by AiServices when the agent decides to use them.
 */
@Component
public class FdsTool {

    /**
     * Retrieves FDS detection statistics for the given year-month.
     * 지정된 연월의 이상탐지 통계를 조회합니다.
     *
     * <p>현재는 Mock 데이터를 반환하며, 실제 운영 시에는 DB 조회로 교체합니다.
     * Currently returns mock data; replace with real DB queries in production.
     *
     * @param yearMonth target year-month in {@code YYYYMM} format (예: "202502")
     *                  / 조회 대상 연월 (형식: YYYYMM, 예: "202502")
     * @return formatted statistics string / 포맷된 통계 문자열
     */
    @Tool("Retrieve FDS anomaly detection statistics for a given year-month (YYYYMM). " +
          "지정한 연월(YYYYMM)의 이상탐지 통계를 조회합니다.")
    public String getFdsStats(String yearMonth) {
        // Mock 데이터 — 실제 환경에서는 Repository 또는 REST 호출로 대체
        // Mock data — replace with Repository or REST call in real environment
        return """
                === FDS 통계 보고서 | FDS Statistics Report ===
                기준 월 / Reference Month : %s
                ─────────────────────────────────────────
                총 거래 건수   / Total Transactions  : 1,240,500
                탐지 건수      / Detected Anomalies  : 3,821
                탐지율         / Detection Rate      : 0.308%%
                오탐율         / False Positive Rate : 0.041%%
                평균 탐지 점수 / Avg Detection Score : 87.4
                ─────────────────────────────────────────
                룰 기반 탐지   / Rule-based          : 2,105건
                ML 모델 탐지   / ML Model            : 1,716건
                """.formatted(yearMonth);
    }

    /**
     * Compares FDS statistics between a base month and a target month.
     * 기준 월과 비교 월의 이상탐지 통계를 비교합니다.
     *
     * <p>전월 대비 증감률을 포함한 비교 리포트를 반환합니다.
     * Returns a comparison report including month-over-month change rates.
     *
     * @param base   base year-month in {@code YYYYMM} format / 기준 연월 (형식: YYYYMM)
     * @param target target year-month to compare against / 비교 대상 연월 (형식: YYYYMM)
     * @return formatted comparison report / 포맷된 비교 리포트 문자열
     */
    @Tool("Compare FDS anomaly detection statistics between two months (YYYYMM). " +
          "두 연월(YYYYMM) 간 이상탐지 통계를 비교합니다.")
    public String compareFdsStats(String base, String target) {
        // Mock 비교 데이터 — 기준/비교 월 값을 그대로 출력에 활용
        // Mock comparison data — uses base/target values directly in output
        return """
                === FDS 전월 비교 보고서 | Month-over-Month Comparison ===
                기준 월 / Base Month   : %s
                비교 월 / Target Month : %s
                ──────────────────────────────────────────────────
                항목                      기준 월    비교 월    증감률
                Item                      Base       Target     Change
                ──────────────────────────────────────────────────
                총 거래 건수 / Transactions : 1,198,200  1,240,500  +3.53%%
                탐지 건수    / Detected     : 3,502      3,821      +9.11%%
                탐지율       / Det. Rate    : 0.292%%    0.308%%    +5.48%%
                오탐율       / FP Rate      : 0.045%%    0.041%%    -8.89%%
                평균 점수    / Avg Score    : 85.1       87.4       +2.70%%
                ──────────────────────────────────────────────────
                총평 / Summary :
                  비교 월에 탐지 건수가 9.1%% 증가했으나 오탐율은 감소하여
                  모델 정확도가 개선된 것으로 판단됩니다.
                  Detection count rose 9.1%% in the target month, but the
                  false-positive rate dropped, indicating improved model accuracy.
                """.formatted(base, target);
    }
}
