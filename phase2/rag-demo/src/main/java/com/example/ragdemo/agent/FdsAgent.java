package com.example.ragdemo.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * FDS (Fraud Detection System) Agent Interface for LangChain4j AiServices.
 * LangChain4j AiServices용 FDS(이상거래탐지시스템) 에이전트 인터페이스.
 *
 * <p>AiServices.builder() 를 통해 프록시 구현체가 자동 생성됩니다.
 * The proxy implementation is auto-generated via {@code AiServices.builder()}.
 */
public interface FdsAgent {

    /**
     * System prompt that defines the agent's role and behavior.
     * 에이전트의 역할과 행동 방식을 정의하는 시스템 프롬프트.
     */
    @SystemMessage("""
            You are an expert analyst for a Fraud Detection System (FDS).
            당신은 이상거래탐지시스템(FDS) 전문 분석가입니다.

            When asked about statistics or comparisons, use the available tools to fetch real data.
            통계나 비교 분석 요청 시, 제공된 도구를 사용하여 실제 데이터를 조회하세요.

            Always respond in Korean unless the user writes in English.
            사용자가 영어로 작성하지 않는 한 항상 한국어로 응답하세요.
            """)
    /**
     * Sends a user message to the agent and returns its response.
     * 사용자 메시지를 에이전트에 전달하고 응답을 반환합니다.
     *
     * @param userMessage the user's input message / 사용자 입력 메시지
     * @return the agent's response / 에이전트 응답 문자열
     */
    String chat(@UserMessage String userMessage);
}
